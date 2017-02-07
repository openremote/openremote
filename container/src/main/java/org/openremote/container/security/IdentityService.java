/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.proxy.SimpleProxyClientProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.observable.RetryWithDelay;
import org.openremote.container.web.ProxyWebClientBuilder;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;
import rx.Observable;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.openremote.container.util.MapAccess.*;
import static org.openremote.container.web.WebClient.getTarget;

public class IdentityService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(IdentityService.class.getName());

    public static final String DISABLE_API_SECURITY = "DISABLE_API_SECURITY";
    public static final boolean DISABLE_API_SECURITY_DEFAULT = false;

    public static final String IDENTITY_NETWORK_SECURE = "IDENTITY_NETWORK_SECURE";
    public static final boolean IDENTITY_NETWORK_SECURE_DEFAULT = false;
    public static final String IDENTITY_NETWORK_HOST = "IDENTITY_NETWORK_HOST";
    public static final String IDENTITY_NETWORK_HOST_DEFAULT = "localhost";
    public static final String IDENTITY_NETWORK_WEBSERVER_PORT = "IDENTITY_NETWORK_WEBSERVER_PORT";
    public static final int IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT = 8080;
    public static final String IDENTITY_SESSION_TIMEOUT_SECONDS = "IDENTITY_SESSION_TIMEOUT_SECONDS";
    public static final int IDENTITY_SESSION_TIMEOUT_SECONDS_DEFAULT = 10800; // 3 hours

    public static final String KEYCLOAK_HOST = "KEYCLOAK_HOST";
    public static final String KEYCLOAK_HOST_DEFAULT = "192.168.99.100";
    public static final String KEYCLOAK_PORT = "KEYCLOAK_PORT";
    public static final int KEYCLOAK_PORT_DEFAULT = 8081;
    public static final String KEYCLOAK_CONNECT_TIMEOUT = "KEYCLOAK_CONNECT_TIMEOUT";
    public static final int KEYCLOAK_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String KEYCLOAK_REQUEST_TIMEOUT = "KEYCLOAK_REQUEST_TIMEOUT";
    public static final int KEYCLOAK_REQUEST_TIMEOUT_DEFAULT = 10000;
    public static final String KEYCLOAK_CLIENT_POOL_SIZE = "KEYCLOAK_CLIENT_POOL_SIZE";
    public static final int KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT = 20;

    public static final String AUTH_PATH = "/auth";

    protected URI externalAuthServerUri;
    protected UriBuilder keycloakHostUri;
    protected UriBuilder keycloakServiceUri;
    protected Client httpClient;
    protected LoadingCache<ClientRealm.Key, ClientRealm> clientApplicationCache;
    protected KeycloakConfigResolver keycloakConfigResolver;
    protected ProxyHandler authProxyHandler;
    protected String clientId;
    protected int sessionTimeoutSeconds;

    @Override
    public void init(Container container) throws Exception {
        boolean identityNetworkSecure = getBoolean(container.getConfig(), IDENTITY_NETWORK_SECURE, IDENTITY_NETWORK_SECURE_DEFAULT);
        String identityNetworkHost = getString(container.getConfig(), IDENTITY_NETWORK_HOST, IDENTITY_NETWORK_HOST_DEFAULT);
        int identityNetworkPort = getInteger(container.getConfig(), IDENTITY_NETWORK_WEBSERVER_PORT, IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT);
        sessionTimeoutSeconds = getInteger(container.getConfig(), IDENTITY_SESSION_TIMEOUT_SECONDS, IDENTITY_SESSION_TIMEOUT_SECONDS_DEFAULT);

        UriBuilder externalAuthServerUrl = UriBuilder.fromUri("")
            .scheme(identityNetworkSecure ? "https" : "http")
            .host(identityNetworkHost)
            .path(AUTH_PATH);

        // Only set the port if it's not the default protocol port. Browsers do this and Keycloak will
        // bake the browsers' redirect URL into the token, so we need a matching config when verifying tokens.
        if (identityNetworkPort != 80 && identityNetworkPort != 443) {
            externalAuthServerUrl = externalAuthServerUrl.port(identityNetworkPort);
        }

        externalAuthServerUri = externalAuthServerUrl.build();

        LOG.info("Token issuer URL: " + externalAuthServerUri);

        keycloakHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(getString(container.getConfig(), KEYCLOAK_HOST, KEYCLOAK_HOST_DEFAULT))
                .port(getInteger(container.getConfig(), KEYCLOAK_PORT, KEYCLOAK_PORT_DEFAULT));

        keycloakServiceUri = keycloakHostUri.clone().replacePath(KeycloakResource.KEYCLOAK_CONTEXT_PATH);

        LOG.info("Preparing identity service for Keycloak host: " + keycloakServiceUri.build());

        // This client sets a custom Host header on outgoing requests, acting like a reverse proxy that "preserves"
        // the Host header. Keycloak will verify token issuer name based on this, so it must match the external host
        // and port that was used to obtain the token.
        ResteasyClientBuilder clientBuilder =
            new ProxyWebClientBuilder(externalAuthServerUri.getHost(), externalAuthServerUri.getPort())
                .establishConnectionTimeout(
                    getInteger(container.getConfig(), KEYCLOAK_CONNECT_TIMEOUT, KEYCLOAK_CONNECT_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .socketTimeout(
                    getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .connectionPoolSize(
                    getInteger(container.getConfig(), KEYCLOAK_CLIENT_POOL_SIZE, KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT)
                );

        httpClient = WebClient.registerDefaults(container, clientBuilder).build();

        clientApplicationCache = createClientApplicationCache();

        if (getBoolean(container.getConfig(), DISABLE_API_SECURITY, DISABLE_API_SECURITY_DEFAULT)) {
            LOG.warning("###################### API SECURITY DISABLED! ######################");
        } else {
            if (getClientId() == null)
                throw new IllegalStateException("Client ID must be set to enable API security");
            keycloakConfigResolver = request -> {
                // This will pass authentication ("NOT ATTEMPTED" state), but later fail any role authorization
                KeycloakDeployment notAuthenticatedKeycloakDeployment = new KeycloakDeployment();

                String realm = request.getQueryParamValue(WebService.REQUEST_REALM_PARAM);
                if (realm == null || realm.length() == 0) {
                    LOG.fine("No realm in request, no authentication will be attempted: " + request.getURI());
                    return notAuthenticatedKeycloakDeployment;
                }
                ClientRealm clientRealm = getClientRealm(realm, getClientId());
                if (clientRealm == null) {
                    LOG.fine("No client application configured for realm, no authentication will be attempted: " + request.getURI());
                    return notAuthenticatedKeycloakDeployment;
                }
                return clientRealm.keycloakDeployment;
            };
        }

        authProxyHandler = new ProxyHandler(
            new SimpleProxyClientProvider(keycloakHostUri.build()),
            getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
            ResponseCodeHandler.HANDLE_404
        );

        container.getService(WebService.class).getApiSingletons().add(
            new IdentityResource(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        pingKeycloak();
        LOG.info("Keycloak identity provider available: " + keycloakServiceUri.build());
    }

    @Override
    public void stop(Container container) throws Exception {
        if (getHttpClient() != null)
            getHttpClient().close();
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public KeycloakConfigResolver getKeycloakConfigResolver() {
        return keycloakConfigResolver;
    }

    public void enableAuthProxy(WebService webService) {
        if (authProxyHandler == null)
            throw new IllegalStateException("Initialize this service first");
        LOG.info("Enabling auth reverse proxy (passing requests through to Keycloak) on web context: " + AUTH_PATH);
        webService.getPrefixRoutes().put(AUTH_PATH, authProxyHandler);
    }

    public KeycloakResource getKeycloak() {
        return getKeycloak(null, false);
    }

    public KeycloakResource getKeycloak(String accessToken) {
        return getKeycloak(accessToken, false);
    }

    public KeycloakResource getKeycloak(String accessToken, boolean enableProxyForward) {
        return getKeycloak(
            getTarget(getHttpClient(), keycloakServiceUri.build(), accessToken, externalAuthServerUri, enableProxyForward)
        );
    }

    public KeycloakResource getKeycloak(ResteasyWebTarget target) {
        return target.proxy(KeycloakResource.class);
    }

    public RealmsResource getRealms(String accessToken, boolean enableProxyForward) {
        return getRealms(
            getTarget(getHttpClient(), keycloakServiceUri.build(), accessToken, externalAuthServerUri, enableProxyForward)
        );
    }

    public RealmsResource getRealms(ResteasyWebTarget target) {
        return target.proxy(RealmsResource.class);
    }

    public ServerInfoResource getServerInfo(String accessToken, boolean enableProxyForward) {
        return getServerInfo(
            getTarget(getHttpClient(), keycloakServiceUri.build(), accessToken, externalAuthServerUri, enableProxyForward)
        );
    }

    public ServerInfoResource getServerInfo(ResteasyWebTarget target) {
        return target.proxy(ServerInfoResource.class);
    }

    public void pingKeycloak() {
        Observable.fromCallable(() -> {
            Response response = getKeycloak().getWelcomePage();
            try {
                if (response != null &&
                    (response.getStatusInfo().getFamily() == SUCCESSFUL
                        || response.getStatusInfo().getFamily() == REDIRECTION)) {
                    return true;
                }
                throw new WebApplicationException("Keycloak not available");
            } finally {
                if (response != null)
                    response.close();
            }
        }).retryWhen(
            new RetryWithDelay("Connecting to Keycloak server " + keycloakServiceUri.build(), 100, 3000)
        ).toBlocking().singleOrDefault(false);
    }

    public ClientRealm getClientRealm(String realm, String clientId) {
        try {
            return clientApplicationCache.get(new ClientRealm.Key(realm, clientId));
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

    protected LoadingCache<ClientRealm.Key, ClientRealm> createClientApplicationCache() {
        CacheLoader<ClientRealm.Key, ClientRealm> loader =
            new CacheLoader<ClientRealm.Key, ClientRealm>() {
                public ClientRealm load(ClientRealm.Key key) {
                    LOG.fine("Loading client '" + key.clientId + "' for realm '" + key.realm + "'");

                    // The client install contains the details we need to verify access tokens
                    ClientInstall clientInstall = getKeycloak().getClientInstall(key.realm, key.clientId);

                    // Make the public key usable for token verification
                    try {
                        clientInstall.setPublicKey(PemUtils.decodePublicKey(clientInstall.getPublicKeyPEM()));
                    } catch (Exception ex) {
                        throw new RuntimeException("Error decoding public key PEM for realm: " + clientInstall.getRealm(), ex);
                    }

                    // Must rewrite the auth-server URL to our external host and port, which
                    // we reverse-proxy back to Keycloak. This is the issuer baked into a token.
                    clientInstall.setAuthServerUrl(externalAuthServerUri.toString());

                    // Some more bloated infrastructure needed, this is for the Keycloak container adapter which
                    // does the automatic token verification. As you can see, it's duplicating ClientInstall.
                    // TODO: Some of the options should be configurable, e.g. CORS and NotBefore
                    KeycloakDeployment keycloakDeployment = new KeycloakDeployment();

                    keycloakDeployment.setRealm(clientInstall.getRealm());
                    keycloakDeployment.setRealmKey(clientInstall.getPublicKey());
                    keycloakDeployment.setResourceName(clientInstall.getClientId());
                    keycloakDeployment.setUseResourceRoleMappings(true);
                    keycloakDeployment.setSslRequired(SslRequired.valueOf(clientInstall.getSslRequired().toUpperCase(Locale.ROOT)));
                    keycloakDeployment.setBearerOnly(true);

                    AdapterConfig adapterConfig = new AdapterConfig();
                    adapterConfig.setResource(keycloakDeployment.getResourceName());
                    adapterConfig.setAuthServerUrl(clientInstall.getAuthServerUrl());
                    keycloakDeployment.setAuthServerBaseUrl(adapterConfig);

                    return new ClientRealm(
                        clientInstall, keycloakDeployment
                    );
                }
            };

        // TODO configurable? Or replace all of this with Observable.cache()?
        return CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, MINUTES)
            .build(loader);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void configureRealm(RealmRepresentation realmRepresentation, int accessTokenLifespanSeconds) {
        realmRepresentation.setDisplayNameHtml(
            "<div class=\"kc-logo-text\"><span>OpenRemote: "
                + (realmRepresentation.getDisplayName().replaceAll("[^A-Za-z0-9]", ""))
                + " </span></div>"
        );
        realmRepresentation.setAccessTokenLifespan(accessTokenLifespanSeconds);
        realmRepresentation.setLoginTheme("openremote");
        realmRepresentation.setAccountTheme("openremote");
        realmRepresentation.setSsoSessionIdleTimeout(sessionTimeoutSeconds);

        // TODO: Make SSL setup configurable
        realmRepresentation.setSslRequired(SslRequired.NONE.toString());
    }

    public ClientRepresentation createClientApplication(String realm, String clientId, String appName, boolean isDevMode) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setName(appName);
        client.setPublicClient(true);

        if (isDevMode) {
            // We need direct access for integration tests
            LOG.warning("### Allowing direct access grants for client app '" + appName + "', this must NOT be used in production! ###");
            client.setDirectAccessGrantsEnabled(true);
        }

        String callbackUrl = UriBuilder.fromUri("/").path(realm).path("*").build().toString();
        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add(callbackUrl);
        client.setRedirectUris(redirectUrls);

        String baseUrl = UriBuilder.fromUri("/").path(realm).build().toString();
        client.setBaseUrl(baseUrl);

        return client;
    }
}