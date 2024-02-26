/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.security.keycloak;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.openremote.container.security.IdentityProvider;
import org.openremote.container.web.OAuthFilter;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthPasswordGrant;

import javax.security.auth.Subject;
import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.Family.REDIRECTION;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebClient.getTarget;
import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.model.Constants.*;

public abstract class KeycloakIdentityProvider implements IdentityProvider {

    // We use this client ID to access Keycloak because by default it allows obtaining
    // an access token from authentication directly, which gives us full access to import/delete
    // demo data as needed.
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final List<String> DEFAULT_CLIENTS = Arrays.asList(
        "account",
        "account-console",
        ADMIN_CLI_CLIENT_ID,
        "broker",
        "master-realm",
        "security-admin-console");

    public static final String OR_KEYCLOAK_HOST = "OR_KEYCLOAK_HOST";
    public static final String OR_KEYCLOAK_HOST_DEFAULT = "127.0.0.1"; // Bug in keycloak default hostname provider means localhost causes problems with dev-proxy profile
    public static final String OR_KEYCLOAK_PORT = "OR_KEYCLOAK_PORT";
    public static final int OR_KEYCLOAK_PORT_DEFAULT = 8081;
    public static final String KEYCLOAK_CONNECT_TIMEOUT = "KEYCLOAK_CONNECT_TIMEOUT";
    public static final int KEYCLOAK_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String KEYCLOAK_REQUEST_TIMEOUT = "KEYCLOAK_REQUEST_TIMEOUT";
    public static final int KEYCLOAK_REQUEST_TIMEOUT_DEFAULT = 10000;
    public static final String KEYCLOAK_CLIENT_POOL_SIZE = "KEYCLOAK_CLIENT_POOL_SIZE";
    public static final int KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT = 20;
    public static final String OR_IDENTITY_SESSION_MAX_MINUTES = "OR_IDENTITY_SESSION_MAX_MINUTES";
    public static final int OR_IDENTITY_SESSION_MAX_MINUTES_DEFAULT = 60 * 24; // 1 day
    public static final String OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES = "OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES";
    public static final int OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES_DEFAULT = 2628000; // 5 years

    public static final String KEYCLOAK_AUTH_PATH = "auth";
    private static final Logger LOG = Logger.getLogger(KeycloakIdentityProvider.class.getName());
    // The URI where Keycloak can be found
    protected UriBuilder keycloakServiceUri;
    // Configuration options for new realms
    protected int sessionTimeoutSeconds;
    protected int sessionMaxSeconds;
    protected int sessionOfflineTimeoutSeconds;
    // This will pass authentication ("NOT ATTEMPTED" state), but later fail any role authorization
    final protected KeycloakDeployment notAuthenticatedKeycloakDeployment = new KeycloakDeployment();
    // The client we use to access Keycloak
    protected ResteasyClient httpClient;
    protected ResteasyWebTarget keycloakTarget;
    protected OAuthGrant oAuthGrant;
    protected ConcurrentLinkedQueue<RealmsResource> realmsResourcePool = new ConcurrentLinkedQueue<>();
    // Cache Keycloak deployment per realm/client so we don't have to access Keycloak for every token validation
    protected LoadingCache<KeycloakRealmClient, KeycloakDeployment> keycloakDeploymentCache;
    // The configuration for the Keycloak servlet extension, looks up the openremote client application per realm
    protected KeycloakConfigResolver keycloakConfigResolver;
    // Optional reverse proxy that listens to KEYCLOAK_AUTH_PATH and forwards requests to Keycloak (used in dev mode to allow same url to be used for manager and keycloak) - handled by proxy in production
    protected HttpHandler authProxyHandler;

    /**
     * The supplied {@link OAuthGrant} will be used to authenticate with keycloak so we can programmatically make changes.
     * It must be credentials for the master realm for a user with `admin` role so that they can perform CRUD on realms,
     * clients and users.
     */
    protected KeycloakIdentityProvider() {
    }

    public OAuthPasswordGrant getDefaultKeycloakGrant(Container container) {
        return new OAuthPasswordGrant(
            getTokenUri(MASTER_REALM).toString(),
            ADMIN_CLI_CLIENT_ID,
            null,
            "openid",
            MASTER_REALM_ADMIN_USER,
            container.getConfig().getOrDefault(OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        );
    }

    @Override
    public void init(Container container) {
        if (httpClient != null) {
            return;
        }

        sessionMaxSeconds = getInteger(container.getConfig(), OR_IDENTITY_SESSION_MAX_MINUTES, OR_IDENTITY_SESSION_MAX_MINUTES_DEFAULT) * 60;
        if (sessionMaxSeconds < 60) {
            throw new IllegalArgumentException(OR_IDENTITY_SESSION_MAX_MINUTES + " must be more than 1 minute");
        }
        // Use the same, as a session is never idle because we periodically check if the refresh token is
        // still good in frontend code, this check will reset the idle timeout anyway
        sessionTimeoutSeconds = sessionMaxSeconds;

        sessionOfflineTimeoutSeconds = getInteger(container.getConfig(), OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES, OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES_DEFAULT) * 60;
        if (sessionOfflineTimeoutSeconds < 60) {
            throw new IllegalArgumentException(OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES + " must be more than 1 minute");
        }

        keycloakServiceUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(getString(container.getConfig(), OR_KEYCLOAK_HOST, OR_KEYCLOAK_HOST_DEFAULT))
                .port(getInteger(container.getConfig(), OR_KEYCLOAK_PORT, OR_KEYCLOAK_PORT_DEFAULT))
                .path(KEYCLOAK_AUTH_PATH);

        LOG.info("Keycloak service URL: " + keycloakServiceUri.build());

        ResteasyClientBuilderImpl clientBuilder = new ResteasyClientBuilderImpl()
                .connectTimeout(
                    getInteger(container.getConfig(), KEYCLOAK_CONNECT_TIMEOUT, KEYCLOAK_CONNECT_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .readTimeout(
                    getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .connectionPoolSize(
                    getInteger(container.getConfig(), KEYCLOAK_CLIENT_POOL_SIZE, KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT)
                );
        httpClient = WebClient.registerDefaults(clientBuilder).build();

        keycloakDeploymentCache = createKeycloakDeploymentCache();

        keycloakConfigResolver = request -> {
            // The realm we authenticate against must be available as a request header
            String realm = request.getHeader(REALM_PARAM_NAME);
            if (realm == null || realm.length() == 0) {
                LOG.finest("No realm in request, no authentication will be attempted: " + request.getURI());
                return notAuthenticatedKeycloakDeployment;
            }
            KeycloakDeployment keycloakDeployment = getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID);
            if (keycloakDeployment == null) {
                LOG.fine("No Keycloak deployment available for realm, no authentication will be attempted: " + request.getURI());
                return notAuthenticatedKeycloakDeployment;
            }
            return keycloakDeployment;
        };

        if (container.isDevMode()) {
            authProxyHandler = ProxyHandler.builder()
                .setProxyClient(new LoadBalancingProxyClient().addHost(keycloakServiceUri.clone().replacePath("").build()))
                .setMaxRequestTime(getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT))
                .setNext(ResponseCodeHandler.HANDLE_404)
                .setReuseXForwarded(true)
                .build();
        }

        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        waitForKeycloak();
        LOG.info("Keycloak identity provider available: " + keycloakServiceUri.build());
    }

    @Override
    public void start(Container container) {

        OAuthGrant credentials = getStoredCredentials(container);

        if (credentials != null) {
            LOG.info("Found stored credentials so attempting to use them");
            if (!setActiveCredentials(credentials)) {
                LOG.warning("Stored keycloak credentials are not valid, falling back to admin user using " + OR_ADMIN_PASSWORD);
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
                LOG.info(OR_ADMIN_PASSWORD + " credentials are valid so creating/recreating stored credentials");
                credentials = generateStoredCredentials(container);
                if (credentials != null) {
                    LOG.info("Stored credentials successfully generated so using them");
                    if (!setActiveCredentials(credentials)) {
                        String msg = "Something went wrong trying to use the new stored credentials, cannot proceed";
                        LOG.warning(msg);
                        throw new RuntimeException(msg);
                    }
                } else {
                    LOG.info("Failed to generate stored credentials will continue using " + OR_ADMIN_PASSWORD);
                }
            }
        }
    }

    @Override
    public void stop(Container container) {
        if (httpClient != null)
            httpClient.close();
    }

    @Override
    public void secureDeployment(DeploymentInfo deploymentInfo) {
        LoginConfig loginConfig = new LoginConfig(SimpleKeycloakServletExtension.AUTH_MECHANISM, "OpenRemote");
        deploymentInfo.setLoginConfig(loginConfig);
        deploymentInfo.addServletExtension(new SimpleKeycloakServletExtension(keycloakConfigResolver));
    }

    public KeycloakResource getKeycloak() {
        return keycloakTarget.proxy(KeycloakResource.class);
    }

    //There is a bug in {@link org.keycloak.admin.client.resource.UserStorageProviderResource#syncUsers} which misses the componentId as parameter
    protected void syncUsers(String componentId, String realm, String action) {
        getRealms(realmsResource -> {
            realmsResource.realm(realm).userStorage().syncUsers(componentId, action);
            return null;
        });
    }

    final synchronized public <T> T getRealms(Function<RealmsResource, T> consumer) {
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

    protected void waitForKeycloak() {
        boolean keycloakAvailable = false;
        WebTargetBuilder targetBuilder = new WebTargetBuilder(httpClient, keycloakServiceUri.build());
        ResteasyWebTarget target = targetBuilder.build();
        KeycloakResource keycloakResource = target.proxy(KeycloakResource.class);

        while (!keycloakAvailable) {
            LOG.info("Connecting to Keycloak server: " + keycloakServiceUri.build());
            try {
                pingKeycloak(keycloakResource);
                keycloakAvailable = true;
            } catch (Exception ex) {
                LOG.info("Keycloak server not available, waiting...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected void pingKeycloak(KeycloakResource resource) throws Exception {
        Response response = null;

        try {
            response = resource.getWelcomePage();
            if (response != null &&
                (response.getStatusInfo().getFamily() == SUCCESSFUL
                    || response.getStatusInfo().getFamily() == REDIRECTION)) {
                return;
            }
            throw new Exception();
        } finally {
            if (response != null)
                response.close();
        }
    }

    public synchronized KeycloakDeployment getKeycloakDeployment(String realm, String clientId) {
        try {
            return keycloakDeploymentCache.get(new KeycloakRealmClient(realm, clientId));
        } catch (Exception ex) {
            if (ex.getCause() != null && ex.getCause() instanceof NotFoundException) {
                LOG.fine("Client '" + clientId + "' for realm '" + realm + "' not found on identity provider");
            } else {
                LOG.log(
                    Level.WARNING,
                    "Error loading client '" + clientId + "' for realm '" + realm + "' from identity provider, " +
                        "exception from call to identity provider follows",
                    ex
                );
            }
            return null;
        }
    }

    public URI getTokenUri(String realm) {
        return keycloakServiceUri.clone().path("realms").path(realm).path("protocol/openid-connect/token").build();
    }

    /**
     * Convenience method for generating access tokens from a given OAuth compliant server
     */
    public Supplier<String> getAccessTokenSupplier(OAuthGrant grant) {
        OAuthFilter oAuthFilter = new OAuthFilter(httpClient, grant);
        return () -> {
            try {
                return oAuthFilter.getAccessToken();
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to get OAuth access token using grant: " + grant, e);
            }
            return null;
        };
    }

    /**
     * Update the active credentials used to perform keycloak API actions; the token endpoint will be overwritten with this
     * instances keycloak server URI and for the master realm.
     */
    public synchronized boolean setActiveCredentials(OAuthGrant grant) {

        if (Objects.equals(this.oAuthGrant, grant)) {
            return true;
        }

        this.oAuthGrant = grant;

        // Force token endpoint to master realm as this is the realm we need to be in to do full keycloak CRUD
        if (grant != null) {
            grant.setTokenEndpointUri(getTokenUri("master").toString());
        }

        URI proxyURI = keycloakServiceUri.build();
        WebTargetBuilder targetBuilder = new WebTargetBuilder(httpClient, proxyURI)
            .setOAuthAuthentication(grant);
        keycloakTarget = targetBuilder.build();
        realmsResourcePool.clear();
        LOG.info("Keycloak proxy URI set to: " + proxyURI);
        LOG.info("Validating keycloak credentials");
        try {
            getRealms(realmsResource -> {
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
     * @return credentials or null if none defined.
     */
    protected abstract OAuthGrant getStoredCredentials(Container container);

    /**
     * Generate and store credentials for connecting to the Keycloak admin API
     * @return credentials or null if generation/storage failed.
     */
    protected abstract OAuthGrant generateStoredCredentials(Container container);

    protected LoadingCache<KeycloakRealmClient, KeycloakDeployment> createKeycloakDeploymentCache() {
        CacheLoader<KeycloakRealmClient, KeycloakDeployment> loader =
            new CacheLoader<KeycloakRealmClient, KeycloakDeployment>() {
                public KeycloakDeployment load(KeycloakRealmClient keycloakRealmClient) {
                    LOG.finest("Loading adapter config for client '" + keycloakRealmClient.clientId + "' in realm '" + keycloakRealmClient.realm + "'");

                    //KeycloakResource keycloak = getKeycloak();
                    KeycloakResource keycloak = getTarget(httpClient, keycloakServiceUri.build(), null, null, null).proxy(KeycloakResource.class);

                    // Can't get adapter for client in another realm
                    AdapterConfig adapterConfig = keycloak.getAdapterConfig(
                        keycloakRealmClient.realm, KEYCLOAK_CLIENT_ID//keycloakRealmClient.clientId
                    );

                    // The auth-server-url in the adapter config must be reachable by this manager it will be the frontend URL by default
                    adapterConfig.setAuthServerUrl(
                        keycloakServiceUri.clone().build().toString()
                    );

                    // Set preferred username as principal attribute
                    adapterConfig.setPrincipalAttribute("preferred_username");

                    return KeycloakDeploymentBuilder.build(adapterConfig);
                }
            };

        // TODO configurable? Or replace all of this with Observable.cache()?
        return CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, MINUTES)
            .build(loader);
    }

    protected void enableAuthProxy(WebService webService) {
        if (authProxyHandler == null)
            throw new IllegalStateException("Initialize this service first");

        LOG.info("Enabling auth reverse proxy (passing requests through to Keycloak) on web context: /" + KEYCLOAK_AUTH_PATH);
        webService.getRequestHandlers().add(0, pathStartsWithHandler(
            "Keycloak auth proxy",
            "/" + KEYCLOAK_AUTH_PATH,
            authProxyHandler));
    }

    /**
     * There must be _some_ valid redirect URIs for the application or authentication will not be possible.
     */
    abstract protected void addClientRedirectUris(String client, List<String> redirectUrls, boolean devMode);

    public static KeycloakSecurityContext getSecurityContext(Subject subject) {
        if (subject == null || subject.getPrincipals() == null) {
            return null;
        }

        return subject.getPrincipals().stream().filter(p -> p instanceof KeycloakPrincipal<?>).findFirst()
            .map(keycloakPrincipal ->
                ((KeycloakPrincipal<?>)keycloakPrincipal).getKeycloakSecurityContext()).orElse(null);
    }

    public static String getSubjectName(Subject subject) {
        if (subject == null || subject.getPrincipals() == null) {
            return null;
        }

        return subject.getPrincipals().stream().filter(p -> p instanceof KeycloakPrincipal<?>).findFirst()
            .map(Principal::getName).orElse(null);
    }

    public static String getSubjectName(Principal principal) {
        return Optional.ofNullable(principal).map(Principal::getName).orElse(null);
    }

    public static String getSubjectNameAndRealm(Principal principal) {
        return Optional.ofNullable(principal).map(p -> {
            if (p instanceof KeycloakPrincipal<?>) {
                String realm = ((KeycloakPrincipal<?>)p).getKeycloakSecurityContext().getRealm();
                return realm + ":" + p.getName();
            } else {
                return p.getName();
            }
        }).orElse(null);
    }

    public static String getSubjectNameAndRealm(Subject subject) {
        if (subject == null || subject.getPrincipals() == null) {
            return null;
        }

        return subject.getPrincipals().stream().filter(p -> p instanceof KeycloakPrincipal<?>).findFirst()
            .map(keycloakPrincipal -> {
                String realm = ((KeycloakPrincipal<?>)keycloakPrincipal).getKeycloakSecurityContext().getRealm();
                return realm + ":" + keycloakPrincipal.getName();
            }).orElse(null);
    }

    public static String getSubjectId(Subject subject) {
        if (subject == null || subject.getPrincipals() == null) {
            return null;
        }

        return Optional.ofNullable(getSecurityContext(subject))
            .map(KeycloakSecurityContext::getToken)
            .map(AccessToken::getSubject)
            .orElse(null);
    }

    public static boolean isSuperUser(KeycloakSecurityContext securityContext) {
        return securityContext != null && Constants.MASTER_REALM.equals(securityContext.getRealm()) && securityContext.getToken().getRealmAccess().isUserInRole(Constants.REALM_ADMIN_ROLE);
    }
}
