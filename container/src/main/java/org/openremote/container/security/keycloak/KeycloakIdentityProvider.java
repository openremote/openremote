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
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.util.HttpString;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.PublishedRealmRepresentation;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityProvider;
import org.openremote.container.web.*;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebClient.getTarget;
import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;
import static org.openremote.model.Constants.REQUEST_HEADER_REALM;

public abstract class KeycloakIdentityProvider implements IdentityProvider {

    // We use this client ID to access Keycloak because by default it allows obtaining
    // an access token from authentication directly, which gives us full access to import/delete
    // demo data as needed.
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final List<String> DEFAULT_CLIENTS = Arrays.asList(
        "account",
        ADMIN_CLI_CLIENT_ID,
        "broker",
        "master-realm",
        "security-admin-console");

// TODO: Below is here ready for Resteasy 4.x but their maven packages are a mess at the moment
//    protected static class EngineBuilder extends ClientHttpEngineBuilder43 {
//
//        protected final UnaryOperator<HttpClientBuilder> httpClientBuilderConfigurator;
//
//        public EngineBuilder(UnaryOperator<HttpClientBuilder> httpClientBuilderConfigurator) {
//            this.httpClientBuilderConfigurator = httpClientBuilderConfigurator;
//        }
//
//        @Override
//        protected ClientHttpEngine createEngine(HttpClientConnectionManager cm, RequestConfig.Builder rcBuilder, HttpHost defaultProxy, int responseBufferSize, HostnameVerifier verifier, SSLContext theContext) {
//            final HttpClient httpClient;
//            rcBuilder.setProxy(defaultProxy);
//            if (System.getSecurityManager() == null)
//            {
//                HttpClientBuilder clientBuilder = HttpClientBuilder.create()
//                .setConnectionManager(cm)
//                .setDefaultRequestConfig(rcBuilder.build())
//                .disableContentCompression();
//
//                if (httpClientBuilderConfigurator != null) {
//                    clientBuilder = httpClientBuilderConfigurator.apply(clientBuilder);
//                }
//
//                httpClient = clientBuilder.build();
//            }
//            else {
//                httpClient = AccessController.doPrivileged(new PrivilegedAction<HttpClient>()
//                {
//                    @Override
//                    public HttpClient run()
//                    {
//                        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
//                            .setConnectionManager(cm)
//                            .setDefaultRequestConfig(rcBuilder.build())
//                            .disableContentCompression();
//
//                        if (httpClientBuilderConfigurator != null) {
//                            clientBuilder = httpClientBuilderConfigurator.apply(clientBuilder);
//                        }
//
//                        return clientBuilder.build();
//                    }
//                });
//            }
//
//            ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient, true);
//            engine.setResponseBufferSize(responseBufferSize);
//            engine.setHostnameVerifier(verifier);
//            // this may be null.  We can't really support this with Apache Client.
//            engine.setSslContext(theContext);
//            return engine;
//        }
//    }
    public static final String KEYCLOAK_HOST = "KEYCLOAK_HOST";
    public static final String KEYCLOAK_HOST_DEFAULT = "localhost";
    public static final String KEYCLOAK_PORT = "KEYCLOAK_PORT";
    public static final int KEYCLOAK_PORT_DEFAULT = 8081;
    public static final String KEYCLOAK_CONNECT_TIMEOUT = "KEYCLOAK_CONNECT_TIMEOUT";
    public static final int KEYCLOAK_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String KEYCLOAK_REQUEST_TIMEOUT = "KEYCLOAK_REQUEST_TIMEOUT";
    public static final int KEYCLOAK_REQUEST_TIMEOUT_DEFAULT = 10000;
    public static final String KEYCLOAK_CLIENT_POOL_SIZE = "KEYCLOAK_CLIENT_POOL_SIZE";
    public static final int KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT = 20;
    public static final String IDENTITY_SESSION_MAX_MINUTES = "IDENTITY_SESSION_MAX_MINUTES";
    public static final int IDENTITY_SESSION_MAX_MINUTES_DEFAULT = 60 * 24; // 1 day
    public static final String IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES = "IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES";
    public static final int IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES_DEFAULT = 60 * 24 * 14; // 14 days
    private static final Logger LOG = Logger.getLogger(KeycloakIdentityProvider.class.getName());
    // Each realm in Keycloak has a client application with this identifier
    final protected String clientId;

    // The externally visible address of this installation
    final protected UriBuilder externalServerUri;

    // The (internal) URI where Keycloak can be found
    final protected UriBuilder keycloakServiceUri;
    // Configuration options for new realms
    final protected int sessionTimeoutSeconds;
    final protected int sessionMaxSeconds;
    final protected int sessionOfflineTimeoutSeconds;
    // This will pass authentication ("NOT ATTEMPTED" state), but later fail any role authorization
    final protected KeycloakDeployment notAuthenticatedKeycloakDeployment = new KeycloakDeployment();
    // The client we use to access Keycloak
    protected ResteasyClient httpClient;
    protected ResteasyWebTarget keycloakTarget;
    // Cache Keycloak deployment per realm/client so we don't have to access Keycloak for every token validation
    protected LoadingCache<KeycloakRealmClient, KeycloakDeployment> keycloakDeploymentCache;
    // The configuration for the Keycloak servlet extension, looks up the client application per realm
    protected KeycloakConfigResolver keycloakConfigResolver;
    // Optional reverse proxy that listens to AUTH_PATH and forwards requests to Keycloak
    protected HttpHandler authProxyHandler;

    @SuppressWarnings("deprecation")
    protected KeycloakIdentityProvider(String clientId, UriBuilder externalServerUri, ExecutorService executorService, Container container) {
        this.clientId = clientId;
        this.externalServerUri = externalServerUri;

        sessionMaxSeconds = getInteger(container.getConfig(), IDENTITY_SESSION_MAX_MINUTES, IDENTITY_SESSION_MAX_MINUTES_DEFAULT) * 60;
        if (sessionMaxSeconds < 60) {
            throw new IllegalArgumentException(IDENTITY_SESSION_MAX_MINUTES + " must be more than 1 minute");
        }
        // Use the same, as a session is never idle because we periodically check if the refresh token is
        // still good in frontend code, this check will reset the idle timeout anyway
        sessionTimeoutSeconds = sessionMaxSeconds;

        sessionOfflineTimeoutSeconds = getInteger(container.getConfig(), IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES, IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES_DEFAULT) * 60;
        if (sessionOfflineTimeoutSeconds < 60) {
            throw new IllegalArgumentException(IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES + " must be more than 1 minute");
        }

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

        WebTargetBuilder targetBuilder = new WebTargetBuilder(httpClient, keycloakServiceUri.build());
        targetBuilder.setOAuthAuthentication(new OAuthPasswordGrant(
            keycloakServiceUri.clone().path("/realms/master/protocol/openid-connect/token").build().toString(),
            ADMIN_CLI_CLIENT_ID,
            null,
            "openid",
            MASTER_REALM_ADMIN_USER,
            container.getConfig().getOrDefault(SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ));
        keycloakTarget = targetBuilder.build();

        keycloakDeploymentCache = createKeycloakDeploymentCache();

        keycloakConfigResolver = request -> {
            // The realm we authenticate against must be available as a request header
            String realm = request.getHeader(REQUEST_HEADER_REALM);
            if (realm == null || realm.length() == 0) {
                LOG.fine("No realm in request, no authentication will be attempted: " + request.getURI());
                return notAuthenticatedKeycloakDeployment;
            }
            KeycloakDeployment keycloakDeployment = getKeycloakDeployment(realm, clientId);
            if (keycloakDeployment == null) {
                LOG.fine("No Keycloak deployment available for realm, no authentication will be attempted: " + request.getURI());
                return notAuthenticatedKeycloakDeployment;
            }
            return keycloakDeployment;
        };

        ProxyHandler proxyHandler = new ProxyHandler(
            new io.undertow.server.handlers.proxy.SimpleProxyClientProvider(keycloakServiceUri.clone().replacePath("").build()),
            getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
            ResponseCodeHandler.HANDLE_404
        ).setReuseXForwarded(true);
        authProxyHandler = exchange -> {
            proxyHandler.handleRequest(exchange);
            // Let the client cache the keycloak.js file for 12 hours
            if (exchange.getRequestPath().endsWith("keycloak.js") && !exchange.isResponseComplete()) {
                exchange.getResponseHeaders().put(HttpString.tryFromString("Cache-Control"), "public,max-age=" + (12 * 60 * 60) + ",must-revalidate");
            }
        };

    }

    @Override
    public void init() {
        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        waitForKeycloak();
        LOG.info("Keycloak identity provider available: " + keycloakServiceUri.build());
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
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

    public KeycloakResource getExternalKeycloak() {
        return getTarget(httpClient, keycloakServiceUri.build(), null, null, externalServerUri.build())
            .proxy(KeycloakResource.class);
    }

    //There is a bug in {@link org.keycloak.admin.client.resource.UserStorageProviderResource#syncUsers} which misses the componentId as parameter
    protected Response syncUsers(ClientRequestInfo clientRequestInfo, String componentId, String realm, String action) {
        return getTarget(httpClient,
            keycloakServiceUri.build(),
            clientRequestInfo.getAccessToken(),
            clientRequestInfo.getRemoteAddress(),
            clientRequestInfo.getRemoteAddress() != null ? externalServerUri.build() : null)
            .path("admin")
            .path("realms")
            .path(realm)
            .path("user-storage")
            .path(componentId)
            .path("sync")
            .queryParam("action", action)
            .request()
            .build(HttpMethod.POST)
            .invoke();
    }

    /**
     * Must be the client source address if this is effectively a forwarded request and the access token was obtained by
     * the client (can be null e.g. for Admin CLI calls during setup or tests where the access token was obtained
     * directly). This should not be overloaded because we want to know who is calling this method with "null", as this
     * can lead to subtle runtime problems.
     */
    final protected RealmsResource getRealms() {
        return keycloakTarget.proxy(RealmsResource.class);
    }

    protected void waitForKeycloak() {
        boolean keycloakAvailable = false;
        while (!keycloakAvailable) {
            LOG.info("Connecting to Keycloak server: " + keycloakServiceUri.build());
            try {
                pingKeycloak();
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

                    KeycloakResource keycloak = getTarget(httpClient, keycloakServiceUri.build(), null, null, null).proxy(KeycloakResource.class);

                    AdapterConfig adapterConfig = keycloak.getAdapterConfig(
                        keycloakRealmClient.realm, keycloakRealmClient.clientId
                    );

                    // Get the public key for token verification
                    PublishedRealmRepresentation realmRepresentation = keycloak.getPublishedRealm(keycloakRealmClient.realm);
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

    protected void enableAuthProxy(WebService webService) {
        if (authProxyHandler == null)
            throw new IllegalStateException("Initialize this service first");

        LOG.info("Enabling auth reverse proxy (passing requests through to Keycloak) on web context: /" + KeycloakResource.KEYCLOAK_CONTEXT_PATH);
        webService.getRequestHandlers().add(0, pathStartsWithHandler(
            "Keycloak auth proxy",
            "/" + KeycloakResource.KEYCLOAK_CONTEXT_PATH,
            authProxyHandler));
    }

    /**
     * There must be _some_ valid redirect URIs for the application or authentication will not be possible.
     */
    abstract protected void addClientRedirectUris(String client, List<String> redirectUrls);

}
