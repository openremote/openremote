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
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.observable.RetryWithDelay;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;
import rx.Observable;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
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

    protected UriBuilder externalAuthServerUrl;
    protected UriBuilder keycloakHostUri;
    protected UriBuilder keycloakServiceUri;
    protected Client httpClient;
    protected LoadingCache<ClientRealm.Key, ClientRealm> clientApplicationCache;
    protected boolean keycloakReverseProxy;
    protected String clientId;

    @Override
    public void init(Container container) throws Exception {
        boolean identityNetworkSecure = container.getConfigBoolean(IDENTITY_NETWORK_SECURE, IDENTITY_NETWORK_SECURE_DEFAULT);
        String identityNetworkHost = container.getConfig(IDENTITY_NETWORK_HOST, IDENTITY_NETWORK_HOST_DEFAULT);
        int identityNetworkPort = container.getConfigInteger(IDENTITY_NETWORK_WEBSERVER_PORT, IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT);

        externalAuthServerUrl = UriBuilder.fromUri("")
            .scheme(identityNetworkSecure ? "https" : "http")
            .host(identityNetworkHost)
            .path(AUTH_PATH);

        // Only set the port if it's not the default protocol port. Browsers do this and Keycloak will
        // bake the browsers' redirect URL into the token, so we need a matching config when verifying tokens.
        if (identityNetworkPort != 80 && identityNetworkPort != 443) {
            externalAuthServerUrl.port(identityNetworkPort);
        }

        LOG.info("Token issuer URL: " + externalAuthServerUrl.build());

        keycloakHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(container.getConfig(KEYCLOAK_HOST, KEYCLOAK_HOST_DEFAULT))
                .port(container.getConfigInteger(KEYCLOAK_PORT, KEYCLOAK_PORT_DEFAULT));

        keycloakServiceUri = keycloakHostUri.clone().replacePath(KeycloakResource.KEYCLOAK_CONTEXT_PATH);

        LOG.info("Preparing identity service for Keycloak host: " + keycloakServiceUri.build());

        ResteasyClientBuilder clientBuilder =
            new ResteasyClientBuilder()
                .establishConnectionTimeout(
                    container.getConfigInteger(KEYCLOAK_CONNECT_TIMEOUT, KEYCLOAK_CONNECT_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .socketTimeout(
                    container.getConfigInteger(KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .connectionPoolSize(
                    container.getConfigInteger(KEYCLOAK_CLIENT_POOL_SIZE, KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT)
                );

        this.httpClient = WebClient.registerDefaults(container, clientBuilder).build();

        clientApplicationCache = createClientApplicationCache();

        if (container.getConfigBoolean(DISABLE_API_SECURITY, DISABLE_API_SECURITY_DEFAULT)) {
            LOG.warning("###################### API SECURITY DISABLED! ######################");
        } else {
            if (getClientId() == null)
                throw new IllegalStateException("Client ID must be set to enable API security");
            container.getService(WebService.class).setKeycloakConfigResolver(request -> {
                // This will pass authentication ("NOT ATTEMPTED" state), but later fail any role authorization
                KeycloakDeployment notAuthenticatedKeycloakDeployment = new KeycloakDeployment();

                String realm = request.getQueryParamValue("realm");
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
            });
        }
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new IdentityResource(this)
        );

        if (isKeycloakReverseProxy()) {
            SimpleProxyClientProvider proxyClient = new SimpleProxyClientProvider(keycloakHostUri.build());
            ProxyHandler proxyHandler = new ProxyHandler(
                proxyClient,
                container.getConfigInteger(KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
                ResponseCodeHandler.HANDLE_404
            );
            container.getService(WebService.class).getPrefixRoutes().put(AUTH_PATH, proxyHandler);
        }
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        pingKeycloak();
        LOG.info("Keycloak identity provider available: " + keycloakServiceUri.build());
    }

    @Override
    public void stop(Container container) throws Exception {
        if (httpClient != null)
            httpClient.close();
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public KeycloakResource getKeycloak() {
        return getKeycloak(getTarget(httpClient, keycloakServiceUri.build(), null));
    }

    public KeycloakResource getKeycloak(String accessToken) {
        return getKeycloak(getTarget(httpClient, keycloakServiceUri.build(), accessToken));
    }

    public KeycloakResource getKeycloak(ResteasyWebTarget target) {
        return target.proxy(KeycloakResource.class);
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
                    Level.INFO,
                    "Error loading client '" + clientId + "' for realm '" + realm + "' from identity provider",
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
                    // we reverse-proxy back to Keycloak
                    clientInstall.setAuthServerUrl(externalAuthServerUrl.build().toString());

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

    public boolean isKeycloakReverseProxy() {
        return keycloakReverseProxy;
    }

    public void setKeycloakReverseProxy(boolean keycloakReverseProxy) {
        this.keycloakReverseProxy = keycloakReverseProxy;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}