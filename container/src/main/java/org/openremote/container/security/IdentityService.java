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
import static org.openremote.container.Constants.*;
import static org.openremote.container.web.WebClient.getTarget;

public abstract class IdentityService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(IdentityService.class.getName());

    public static final String DISABLE_API_SECURITY = "DISABLE_API_SECURITY";
    public static final boolean DISABLE_API_SECURITY_DEFAULT = false;
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

    protected boolean disableAPISecurity;
    protected boolean configNetworkSecure;
    protected String configNetworkHost;
    protected int configNetworkWebserverPort;
    protected UriBuilder externalAuthServerUrl;
    protected UriBuilder keycloakHostUri;
    protected UriBuilder keycloakServiceUri;
    protected Client client;
    protected LoadingCache<ClientRealm.Key, ClientRealm> clientApplicationCache;

    @Override
    public void prepare(Container container) {

        this.disableAPISecurity = container.getConfigBoolean(DISABLE_API_SECURITY, DISABLE_API_SECURITY_DEFAULT);
        this.configNetworkSecure = container.getConfigBoolean(NETWORK_SECURE, NETWORK_SECURE_DEFAULT);
        this.configNetworkHost = container.getConfig(NETWORK_HOST, NETWORK_HOST_DEFAULT);
        this.configNetworkWebserverPort = container.getConfigInteger(NETWORK_WEBSERVER_PORT, NETWORK_WEBSERVER_PORT_DEFAULT);

        externalAuthServerUrl = UriBuilder.fromUri("")
            .scheme(this.configNetworkSecure ? "https" : "http")
            .host(this.configNetworkHost)
            .port(this.configNetworkWebserverPort)
            .path(AUTH_PATH);

        LOG.info("External auth server URL (reverse proxy for Keycloak) is: " + externalAuthServerUrl.build());

        keycloakHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(container.getConfig(KEYCLOAK_HOST, KEYCLOAK_HOST_DEFAULT))
                .port(container.getConfigInteger(KEYCLOAK_PORT, KEYCLOAK_PORT_DEFAULT));

        keycloakServiceUri = keycloakHostUri.clone().replacePath(Keycloak.KEYCLOAK_CONTEXT_PATH);

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

        this.client = WebClient.registerDefaults(clientBuilder).build();

        clientApplicationCache = createClientApplicationCache();

        container.getService(getWebServiceClass()).getApiSingletons().add(
            new IdentityResource(this)
        );

        if (enableKeycloakReverseProxy()) {
            SimpleProxyClientProvider proxyClient = new SimpleProxyClientProvider(keycloakHostUri.build());
            ProxyHandler proxyHandler = new ProxyHandler(
                proxyClient,
                container.getConfigInteger(KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
                ResponseCodeHandler.HANDLE_404
            );
            container.getService(getWebServiceClass()).getPrefixRoutes().put(AUTH_PATH, proxyHandler);
        }

        if (isDisableAPISecurity()) {
            LOG.warning("###################### API SECURITY DISABLED! ######################");
        } else {
            container.getService(getWebServiceClass()).setKeycloakConfigResolver(request -> {
                KeycloakDeployment passthroughKeycloakDeployment = new KeycloakDeployment();

                String realm = request.getQueryParamValue("realm");
                if (realm == null || realm.length() == 0)
                    return passthroughKeycloakDeployment;
                ClientRealm clientApplication = getClientRealm(realm, getClientId());
                if (clientApplication == null)
                    return passthroughKeycloakDeployment;
                return clientApplication.keycloakDeployment;
            });
        }
    }

    @Override
    public void start(Container container) {
        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        pingKeycloak();
        LOG.info("Keycloak identity provider available: " + keycloakServiceUri.build());
    }

    @Override
    public void stop(Container container) {
        if (client != null)
            client.close();
    }

    public boolean isDisableAPISecurity() {
        return disableAPISecurity;
    }

    public boolean isConfigNetworkSecure() {
        return configNetworkSecure;
    }

    public String getConfigNetworkHost() {
        return configNetworkHost;
    }

    public int getConfigNetworkWebserverPort() {
        return configNetworkWebserverPort;
    }

    public Client getClient() {
        return client;
    }

    public Keycloak getKeycloak() {
        return getKeycloak(getTarget(client, keycloakServiceUri.build(), null));
    }

    public Keycloak getKeycloak(String accessToken) {
        return getKeycloak(getTarget(client, keycloakServiceUri.build(), accessToken));
    }

    public Keycloak getKeycloak(ResteasyWebTarget target) {
        return target.proxy(Keycloak.class);
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
            new RetryWithDelay("Connecting to Keycloak server " + keycloakServiceUri.build(), 10, 3000)
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
                    // we'll later reverse-proxy back to Keycloak
                    clientInstall.setAuthServerUrl(externalAuthServerUrl.build().toString());

                    // Also correct the backend URL at this time, this URL will be written
                    // by Keycloak as the issuer into each token automatically
                    clientInstall.setAuthServerUrlForBackendRequests(
                        UriBuilder.fromUri(clientInstall.getAuthServerUrl()).path("realms").path(clientInstall.getRealm())
                            .build().toString()
                    );

                    // Some more bloated infrastructure needed, this is for the Keycloak container adapter which
                    // does the automatic token verification. As you can see, it's duplicating ClientInstall.
                    // TODO: Some of the options should be configurable, e.g. CORS and NotBefore
                    KeycloakDeployment keycloakDeployment = new KeycloakDeployment();

                    keycloakDeployment.setRealm(clientInstall.getRealm());
                    keycloakDeployment.setRealmKey(clientInstall.getPublicKey());
                    keycloakDeployment.setResourceName(clientInstall.getClientId());
                    keycloakDeployment.setSslRequired(SslRequired.valueOf(clientInstall.getSslRequired().toUpperCase(Locale.ROOT)));
                    keycloakDeployment.setBearerOnly(true);

                    AdapterConfig adapterConfig = new AdapterConfig();
                    adapterConfig.setResource(keycloakDeployment.getResourceName());
                    adapterConfig.setAuthServerUrl(clientInstall.getAuthServerUrl());
                    adapterConfig.setAuthServerUrlForBackendRequests(clientInstall.getAuthServerUrlForBackendRequests());
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

    protected abstract boolean enableKeycloakReverseProxy();

    protected abstract Class<? extends WebService> getWebServiceClass();

    protected abstract String getClientId();
}