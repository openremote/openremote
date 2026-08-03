/*
 * Copyright 2017, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.security.keycloak;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;
import static org.openremote.model.util.MapAccess.*;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.admin.client.resource.RealmsResource;
import org.openremote.container.security.*;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.auth.OAuthClientCredentialsGrant;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthPasswordGrant;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.VersionUtil;

public abstract class KeycloakIdentityProvider implements IdentityProvider {

  public record DiscoveryResult(String issuer) {}

  // We use this client ID to access Keycloak because by default it allows obtaining
  // an access token from authentication directly, which gives us full access to import/delete
  // demo data as needed.
  public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
  public static final List<String> DEFAULT_CLIENTS =
      Arrays.asList(
          "account",
          "account-console",
          ADMIN_CLI_CLIENT_ID,
          "broker",
          "master-realm",
          "security-admin-console");

  public static final String OR_KEYCLOAK_HOST = "OR_KEYCLOAK_HOST";
  public static final String OR_KEYCLOAK_HOST_DEFAULT = "127.0.0.1";
  public static final String OR_KEYCLOAK_PORT = "OR_KEYCLOAK_PORT";
  public static final int OR_KEYCLOAK_PORT_DEFAULT = 8081;
  public static final String OR_KEYCLOAK_DISABLE_ISSUER_VALIDATION =
      "OR_KEYCLOAK_DISABLE_ISSUER_VALIDATION";
  public static final String OR_KEYCLOAK_PATH = "OR_KEYCLOAK_PATH";
  public static final String OR_KEYCLOAK_PATH_DEFAULT = "/auth";
  public static final String OIDC_CONFIG_PATH = "/realms/master/.well-known/openid-configuration";
  public static final String OR_IDENTITY_SESSION_MAX_MINUTES = "OR_IDENTITY_SESSION_MAX_MINUTES";
  public static final int OR_IDENTITY_SESSION_MAX_MINUTES_DEFAULT = 60 * 24; // 1 day
  public static final String OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES =
      "OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES";
  public static final int OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES_DEFAULT = 2628000; // 5 years
  private static final Logger LOG = Logger.getLogger(KeycloakIdentityProvider.class.getName());
  public static final String KEYCLOAK_MIN_VERSION = "26.7.0";
  // The URI where Keycloak can be found
  protected UriBuilder keycloakServiceUri;
  // Configuration options for new realms
  protected int sessionTimeoutSeconds;
  protected int sessionMaxSeconds;
  protected int sessionOfflineTimeoutSeconds;
  protected ResteasyWebTarget keycloakTarget;
  protected String keycloakPublicUri;
  protected OAuthGrant oAuthGrant;
  protected ConcurrentLinkedQueue<RealmsResource> realmsResourcePool =
      new ConcurrentLinkedQueue<>();
  protected TokenVerifier tokenVerifier;

  /**
   * The supplied {@link OAuthGrant} will be used to authenticate with keycloak so we can
   * programmatically make changes. It must be credentials for the master realm for a user with
   * `admin` role so that they can perform CRUD on realms, clients and users.
   */
  protected KeycloakIdentityProvider() {}

  public OAuthPasswordGrant getDefaultKeycloakGrant(Container container) {
    return new OAuthPasswordGrant(
        getTokenUri(MASTER_REALM).toString(),
        ADMIN_CLI_CLIENT_ID,
        null,
        "openid",
        MASTER_REALM_ADMIN_USER,
        container.getConfig().getOrDefault(OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT));
  }

  @Override
  public void init(Container container) {
    sessionMaxSeconds =
        getInteger(
                container.getConfig(),
                OR_IDENTITY_SESSION_MAX_MINUTES,
                OR_IDENTITY_SESSION_MAX_MINUTES_DEFAULT)
            * 60;
    if (sessionMaxSeconds < 60) {
      throw new IllegalArgumentException(
          OR_IDENTITY_SESSION_MAX_MINUTES + " must be more than 1 minute");
    }
    // Use the same, as a session is never idle because we periodically check if the refresh token
    // is
    // still good in frontend code, this check will reset the idle timeout anyway
    sessionTimeoutSeconds = sessionMaxSeconds;

    sessionOfflineTimeoutSeconds =
        getInteger(
                container.getConfig(),
                OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES,
                OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES_DEFAULT)
            * 60;
    if (sessionOfflineTimeoutSeconds < 60) {
      throw new IllegalArgumentException(
          OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES + " must be more than 1 minute");
    }

    keycloakServiceUri =
        UriBuilder.fromPath("/")
            .scheme("http")
            .host(getString(container.getConfig(), OR_KEYCLOAK_HOST, OR_KEYCLOAK_HOST_DEFAULT))
            .port(getInteger(container.getConfig(), OR_KEYCLOAK_PORT, OR_KEYCLOAK_PORT_DEFAULT));

    String keycloakPath =
        getString(container.getConfig(), OR_KEYCLOAK_PATH, OR_KEYCLOAK_PATH_DEFAULT);

    if (!TextUtil.isNullOrEmpty(keycloakPath)) {
      keycloakServiceUri.path(keycloakPath);
    }

    LOG.info("Keycloak service URL: " + keycloakServiceUri.build());

    String keycloakPublicUrl = getKeycloakPublicUrl();

    boolean disableIssuerValidation =
        getBoolean(container.getConfig(), OR_KEYCLOAK_DISABLE_ISSUER_VALIDATION, false);

    if (disableIssuerValidation) {
      LOG.warning(
          "Token issuer validation disabled this should not be used for publicly exposed instances");
    }

    tokenVerifier =
        new TokenVerifierImpl(
            keycloakServiceUri.build().toString(),
            keycloakPublicUrl != null ? keycloakPublicUrl : keycloakServiceUri.build().toString(),
            disableIssuerValidation);
  }

  @Override
  public void start(Container container) {

    OAuthGrant credentials = getStoredCredentials(container);

    if (credentials != null) {
      LOG.info("Found stored credentials so attempting to use them");
      if (!setActiveCredentials(credentials)) {
        LOG.warning(
            "Stored keycloak credentials are not valid, falling back to admin user using "
                + OR_ADMIN_PASSWORD);
        credentials = getDefaultKeycloakGrant(container);
      } else {
        credentials = null;
      }
    } else {
      LOG.info("No stored credentials so using " + OR_ADMIN_PASSWORD);
      credentials = getDefaultKeycloakGrant(container);
    }

    if (credentials != null) {
      if (!setActiveCredentials(credentials)) {
        String msg = "Credentials don't work so cannot continue";
        LOG.warning(msg);
        throw new RuntimeException(msg);
      } else {
        LOG.info(
            OR_ADMIN_PASSWORD + " credentials are valid so creating/recreating stored credentials");
        credentials = generateStoredCredentials(container);
        if (credentials != null) {
          LOG.info("Stored credentials successfully generated so using them");
          if (!setActiveCredentials(credentials)) {
            String msg =
                "Something went wrong trying to use the new stored credentials, cannot proceed";
            LOG.warning(msg);
            throw new RuntimeException(msg);
          }
        } else {
          LOG.info(
              "Failed to generate stored credentials will continue using " + OR_ADMIN_PASSWORD);
        }
      }
    }

    // Verify the version of keycloak matches our requirements
    JsonNode serverInfo =
        keycloakTarget
            .path("admin/serverinfo")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    String keycloakVersion = serverInfo.get("systemInfo").get("version").asText("0.0.0");
    if (!VersionUtil.isVersionGreaterOrEqual(keycloakVersion, KEYCLOAK_MIN_VERSION)) {
      String msg =
          "Keycloak version "
              + keycloakVersion
              + " is not supported; version "
              + KEYCLOAK_MIN_VERSION
              + " or newer is required.";
      LOG.severe(msg);
      throw new IllegalStateException(msg);
    }

    LOG.info("Keycloak version detected: " + keycloakVersion);
  }

  @Override
  public void stop(Container container) {}

  @Override
  public FilterRegistration.Dynamic secureDeployment(ServletContext servletContext) {
    JWTAuthenticationFilter jwtFilter = new JWTAuthenticationFilter(tokenVerifier);
    FilterRegistration.Dynamic registration =
        servletContext.addFilter(JWTAuthenticationFilter.NAME, jwtFilter);
    return registration;
  }

  protected ReactiveTokenService getReactiveTokenService() {
    return keycloakTarget.proxy(ReactiveTokenService.class);
  }

  // There is a bug in {@link
  // org.keycloak.admin.client.resource.UserStorageProviderResource#syncUsers} which misses the
  // componentId as parameter
  protected void syncUsers(String componentId, String realm, String action) {
    getRealms(
        realmsResource -> {
          realmsResource.realm(realm).userStorage().syncUsers(componentId, action);
          return null;
        });
  }

  public final synchronized <T> T getRealms(Function<RealmsResource, T> consumer) {
    RealmsResource realmsResource;
    ResteasyWebTarget target = keycloakTarget; // Track this in case active credentials updated
    if ((realmsResource = realmsResourcePool.poll()) == null) {
      realmsResource = keycloakTarget.proxy(RealmsResource.class);
    }
    try {
      return consumer.apply(realmsResource);
    } finally {
      if (target == this.keycloakTarget) {
        realmsResourcePool.offer(realmsResource);
      }
    }
  }

  protected URI getTokenUri(String realm) {
    return keycloakServiceUri
        .clone()
        .path("realms")
        .path(realm)
        .path("protocol/openid-connect/token")
        .build();
  }

  /**
   * Update the active credentials used to perform keycloak API actions; the token endpoint will be
   * overwritten with this instances keycloak server URI and for the master realm.
   */
  public synchronized boolean setActiveCredentials(OAuthGrant grant) {

    if (Objects.equals(this.oAuthGrant, grant)) {
      return true;
    }

    this.oAuthGrant = grant;

    // Force token endpoint to master realm as this is the realm we need to be in to do full
    // keycloak CRUD
    if (grant != null) {
      grant.setTokenEndpointUri(getTokenUri("master").toString());
    }

    URI proxyURI = keycloakServiceUri.build();
    WebTargetBuilder targetBuilder =
        new WebTargetBuilder(WebTargetBuilder.getClient(), proxyURI).setOAuthAuthentication(grant);
    keycloakTarget = targetBuilder.build();
    realmsResourcePool.clear();
    LOG.info("Keycloak proxy URI set to: " + proxyURI);
    LOG.info("Validating keycloak credentials");
    try {
      getRealms(
          realmsResource -> {
            realmsResource.realm(MASTER_REALM).toRepresentation();
            LOG.info("Credentials are valid");
            return null;
          });
    } catch (Exception e) {
      LOG.info("Credentials are invalid");
      return false;
    }

    return true;
  }

  /**
   * Get any stored credentials for connecting to the Keycloak admin API
   *
   * @return credentials or null if none defined.
   */
  protected abstract OAuthGrant getStoredCredentials(Container container);

  /**
   * Generate and store credentials for connecting to the Keycloak admin API
   *
   * @return credentials or null if generation/storage failed.
   */
  protected abstract OAuthGrant generateStoredCredentials(Container container);

  /**
   * There must be _some_ valid redirect URIs for the application or authentication will not be
   * possible.
   */
  protected abstract void addClientRedirectUris(
      String client, List<String> redirectUrls, boolean devMode);

  public static String getSubjectName(Subject subject) {
    if (subject == null || subject.getPrincipals() == null) {
      return null;
    }

    return subject.getPrincipals().stream()
        .filter(p -> p instanceof KeycloakPrincipal<?>)
        .findFirst()
        .map(Principal::getName)
        .orElse(null);
  }

  public static String getSubjectNameAndRealm(Principal principal) {
    return Optional.ofNullable(principal)
        .map(
            p -> {
              if (p instanceof KeycloakPrincipal<?>) {
                String realm = ((KeycloakPrincipal<?>) p).getKeycloakSecurityContext().getRealm();
                return realm + ":" + p.getName();
              } else {
                return p.getName();
              }
            })
        .orElse(null);
  }

  public static String getSubjectNameAndRealm(Subject subject) {
    if (subject == null || subject.getPrincipals() == null) {
      return null;
    }

    return subject.getPrincipals().stream()
        .filter(p -> p instanceof KeycloakPrincipal<?>)
        .findFirst()
        .map(
            keycloakPrincipal -> {
              String realm =
                  ((KeycloakPrincipal<?>) keycloakPrincipal)
                      .getKeycloakSecurityContext()
                      .getRealm();
              return realm + ":" + keycloakPrincipal.getName();
            })
        .orElse(null);
  }

  public static boolean isSuperUser(KeycloakSecurityContext securityContext) {
    return securityContext != null
        && Constants.MASTER_REALM.equals(securityContext.getRealm())
        && securityContext
            .getToken()
            .getRealmAccess()
            .isUserInRole(Constants.SUPER_USER_REALM_ROLE);
  }

  @Override
  public CompletableFuture<OIDCTokenResponse> authenticate(
      String realm, String clientId, String clientSecret) {
    return getReactiveTokenService()
        .grantToken(
            realm,
            new OAuthClientCredentialsGrant(null, clientId, clientSecret, null).asMultivaluedMap())
        .toCompletableFuture()
        .exceptionallyCompose(
            ex -> {
              Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;

              if (cause instanceof WebApplicationException webEx) {
                if (webEx.getResponse() != null) {
                  // Ensure the response is closed to free the connection
                  webEx.getResponse().close();
                }
              }

              if (!(cause instanceof AuthenticationException)) {
                return CompletableFuture.failedFuture(new AuthenticationException(cause));
              }

              return CompletableFuture.failedFuture(cause);
            })
        .thenApply(OIDCTokenResponse::create)
        .toCompletableFuture();
  }

  @Override
  public TokenPrincipal verify(String realm, String accessToken) throws AuthenticationException {
    return tokenVerifier.verify(realm, accessToken);
  }

  /** Get public URL of keycloak from the keycloak server token endpoint issuer */
  protected String getKeycloakPublicUrl() {
    if (keycloakPublicUri != null) {
      return keycloakPublicUri;
    }

    WebTarget webTarget =
        new WebTargetBuilder(WebTargetBuilder.getClient(), keycloakServiceUri.build())
            .build()
            .path(OIDC_CONFIG_PATH);

    LOG.info(
        "Getting public URL of keycloak from the keycloak OIDC discovery endpoint: "
            + webTarget.getUri());
    DiscoveryResult result = null;
    try (Response response =
        webTarget
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get()) {

      if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
        String body = "<unreadable>";
        try {
          body = response.readEntity(String.class);
        } catch (Exception ignore) {
        }

        LOG.severe("OIDC discovery failed: HTTP " + response.getStatus() + " body=" + body);
      } else {
        result = response.readEntity(DiscoveryResult.class);
      }
    }

    // We want the base public URI not the master specific one
    keycloakPublicUri =
        result != null
            ? result.issuer().replace("/realms/master", "")
            : keycloakServiceUri.build().toString();
    return keycloakPublicUri;
  }
}
