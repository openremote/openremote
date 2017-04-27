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
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.PublishedRealmRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.web.ProxyWebClientBuilder;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.openremote.container.util.MapAccess.*;
import static org.openremote.container.web.WebClient.getTarget;

public abstract class IdentityService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(IdentityService.class.getName());

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

    // The externally visible address of this installation
    protected UriBuilder externalServerUri;

    // Each realm in Keycloak has a client application with this identifier
    final protected String clientId;

    // The (internal) URI where Keycloak can be found
    protected UriBuilder keycloakServiceUri;

    // The client we use to access Keycloak
    protected Client httpClient;

    // Cache Keycloak deployment per realm/client so we don't have to access Keycloak for every token validation
    protected LoadingCache<KeycloakRealmClient, KeycloakDeployment> keycloakDeploymentCache;

    // The configuration for the Keycloak servlet extension, looks up the client application per realm
    protected KeycloakConfigResolver keycloakConfigResolver;

    // Optional reverse proxy that listens to AUTH_PATH and forwards requests to Keycloak
    protected ProxyHandler authProxyHandler;

    // Configuration options for new realms
    protected int sessionTimeoutSeconds;

    // This will pass authentication ("NOT ATTEMPTED" state), but later fail any role authorization
    final protected KeycloakDeployment notAuthenticatedKeycloakDeployment = new KeycloakDeployment();

    public IdentityService(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void init(Container container) throws Exception {
        boolean identityNetworkSecure = getBoolean(container.getConfig(), IDENTITY_NETWORK_SECURE, IDENTITY_NETWORK_SECURE_DEFAULT);
        String identityNetworkHost = getString(container.getConfig(), IDENTITY_NETWORK_HOST, IDENTITY_NETWORK_HOST_DEFAULT);
        int identityNetworkPort = getInteger(container.getConfig(), IDENTITY_NETWORK_WEBSERVER_PORT, IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT);
        sessionTimeoutSeconds = getInteger(container.getConfig(), IDENTITY_SESSION_TIMEOUT_SECONDS, IDENTITY_SESSION_TIMEOUT_SECONDS_DEFAULT);

        externalServerUri = UriBuilder.fromUri("")
            .scheme(identityNetworkSecure ? "https" : "http")
            .host(identityNetworkHost);

        // Only set the port if it's not the default protocol port. Browsers do this and Keycloak will
        // bake the browsers' redirect URL into the token, so we need a matching config when verifying tokens.
        if (identityNetworkPort != 80 && identityNetworkPort != 443) {
            externalServerUri = externalServerUri.port(identityNetworkPort);
        }

        LOG.info("External system base URL: " + externalServerUri.build());

        keycloakServiceUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(getString(container.getConfig(), KEYCLOAK_HOST, KEYCLOAK_HOST_DEFAULT))
                .port(getInteger(container.getConfig(), KEYCLOAK_PORT, KEYCLOAK_PORT_DEFAULT))
                .path(KeycloakResource.KEYCLOAK_CONTEXT_PATH);

        LOG.info("Keycloak service URL: " + keycloakServiceUri.build());

        // This client sets a custom Host header on outgoing requests, acting like a reverse proxy that "preserves"
        // the Host header. Keycloak will verify token issuer name based on this, so it must match the external host
        // and port that was used to obtain the token.
        ResteasyClientBuilder clientBuilder =
            new ProxyWebClientBuilder(externalServerUri.build().getHost(), externalServerUri.build().getPort())
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

        httpClient = WebClient.registerDefaults(clientBuilder).build();

        keycloakDeploymentCache = createKeycloakDeploymentCache();

        keycloakConfigResolver = request -> {
            // The realm we authenticate against must be available as a request header
            String realm = request.getHeader(WebService.REQUEST_HEADER_REALM);
            if (realm == null || realm.length() == 0) {
                LOG.fine("No realm in request, no authentication will be attempted: " + request.getURI());
                return notAuthenticatedKeycloakDeployment;
            }
            KeycloakDeployment keycloakDeployment = getKeycloakDeployment(realm, getClientId());
            if (keycloakDeployment == null) {
                LOG.fine("No Keycloak deployment available for realm, no authentication will be attempted: " + request.getURI());
                return notAuthenticatedKeycloakDeployment;
            }
            return keycloakDeployment;
        };

        authProxyHandler = new ProxyHandler(
            new SimpleProxyClientProvider(keycloakServiceUri.clone().replacePath("").build()),
            getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
            ResponseCodeHandler.HANDLE_404
        ).setReuseXForwarded(true);
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        waitForKeycloak();
        LOG.info("Keycloak identity provider available: " + keycloakServiceUri.build());
    }

    @Override
    public void stop(Container container) throws Exception {
        if (getHttpClient() != null)
            getHttpClient().close();
    }

    public UriBuilder getExternalServerUri() {
        return externalServerUri.clone();
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
        LOG.info("Enabling auth reverse proxy (passing requests through to Keycloak) on web context: /" + KeycloakResource.KEYCLOAK_CONTEXT_PATH);
        webService.getPrefixRoutes().put("/" + KeycloakResource.KEYCLOAK_CONTEXT_PATH, authProxyHandler);
    }

    public KeycloakResource getKeycloak() {
        return getTarget(getHttpClient(), keycloakServiceUri.build(), null, null, null)
            .proxy(KeycloakResource.class);
    }

    /**
     * @param forwardFor Must be the client source address if this is effectively a forwarded request and the
     *                   access token was obtained by the client (can be null e.g. for Admin CLI calls during setup
     *                   or tests where the access token was obtained directly). This should not be overloaded because
     *                   we want to know who is calling this method with "null", as this can lead to subtle runtime
     *                   problems.
     */
    final public RealmsResource getRealms(String forwardFor, String accessToken) {
        return getTarget(getHttpClient(), keycloakServiceUri.build(), accessToken, forwardFor, forwardFor != null ? externalServerUri.build(): null)
            .proxy(RealmsResource.class);
    }

    protected void waitForKeycloak() {
        boolean keycloakAvailable = false;
        while (!keycloakAvailable) {
            LOG.info("Connecting to Keycloak server: " + keycloakServiceUri.build());
            try {
                pingKeycloak();
                keycloakAvailable = true;
            } catch(Exception ex) {
                LOG.info("Keycloak server not available, waiting...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected void pingKeycloak() throws Exception {
        Response response = null;
        try {
            response = getKeycloak().getWelcomePage();
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

    public KeycloakDeployment getKeycloakDeployment(String realm, String clientId) {
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

    protected LoadingCache<KeycloakRealmClient, KeycloakDeployment> createKeycloakDeploymentCache() {
        CacheLoader<KeycloakRealmClient, KeycloakDeployment> loader =
            new CacheLoader<KeycloakRealmClient, KeycloakDeployment>() {
                public KeycloakDeployment load(KeycloakRealmClient keycloakRealmClient) {
                    LOG.fine("Loading adapter config for client '" + keycloakRealmClient.clientId + "' in realm '" + keycloakRealmClient.realm + "'");

                    AdapterConfig adapterConfig = getKeycloak().getAdapterConfig(
                        keycloakRealmClient.realm, keycloakRealmClient.clientId
                    );

                    // Get the public key for token verification
                    PublishedRealmRepresentation realmRepresentation = getKeycloak().getPublishedRealm(keycloakRealmClient.realm);
                    adapterConfig.setRealmKey(realmRepresentation.getPublicKeyPem());

                    // Must rewrite the auth-server URL to our external host and port, which
                    // we reverse-proxy back to Keycloak. This is the issuer baked into a token.
                    adapterConfig.setAuthServerUrl(
                        externalServerUri.clone().replacePath(KeycloakResource.KEYCLOAK_CONTEXT_PATH).build().toString()
                    );

                    // TODO: Some other options should be configurable, e.g. CORS and NotBefore

                    return KeycloakDeploymentBuilder.build(adapterConfig);
                }
            };

        // TODO configurable? Or replace all of this with Observable.cache()?
        return CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, MINUTES)
            .build(loader);
    }

    public void configureRealm(RealmRepresentation realmRepresentation, int accessTokenLifespanSeconds) {
        realmRepresentation.setDisplayNameHtml(
            "<span>" + (realmRepresentation.getDisplayName().replaceAll("[^A-Za-z0-9]", "")) + " </span>"
        );
        realmRepresentation.setAccessTokenLifespan(accessTokenLifespanSeconds);
        realmRepresentation.setLoginTheme("openremote");
        realmRepresentation.setAccountTheme("openremote");
        realmRepresentation.setEmailTheme("openremote");
        realmRepresentation.setSsoSessionIdleTimeout(sessionTimeoutSeconds);

        realmRepresentation.setSslRequired(SslRequired.NONE.toString());
    }

    public ClientRepresentation createClientApplication(String realm, String clientId, String appName, boolean enableDirectAccess) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setName(appName);
        client.setPublicClient(true);

        if (enableDirectAccess) {
            // We need direct access for integration tests
            LOG.warning("### Allowing direct access grants for client app '" + appName + "', this must NOT be used in production! ###");
            client.setDirectAccessGrantsEnabled(true);
        }

        List<String> redirectUris = new ArrayList<>();
        addClientRedirectUris(realm, redirectUris);
        client.setRedirectUris(redirectUris);

        // Redirect URL for logout etc, go to /<realm>/
        String baseUrl = UriBuilder.fromUri("/").path(realm).build().toString();
        client.setBaseUrl(baseUrl);

        return client;
    }

    /**
     * There must be _some_ valid redirect URIs for the application or authentication will not be possible.
     */
    abstract protected void addClientRedirectUris(String realm, List<String> redirectUrls);
}