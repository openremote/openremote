package org.openremote.manager.server2.identity;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.proxy.SimpleProxyClientProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.common.util.PemUtils;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.web.JacksonConfig;
import org.openremote.manager.server.Constants;
import org.openremote.manager.server.identity.ClientInstall;
import org.openremote.manager.server.observable.RetryWithDelay;
import org.openremote.manager.server.util.UrlUtil;
import org.openremote.manager.server2.ManagerWebService;
import rx.Observable;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.openremote.manager.server.Constants.*;
import static org.openremote.manager.server.util.UrlUtil.url;

public class IdentityService implements ContainerService {

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

    public static final String ADMIN_CLI_CLIENT = "admin-cli";
    protected static final String KEYCLOAK_CONTEXT_PATH = "auth";

    public class BearerAuthClientRequestFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            String accessToken = (String) requestContext.getConfiguration().getProperty("accessToken");
            if (accessToken != null) {
                String authorization = "Bearer " + accessToken;
                requestContext.getHeaders().add(AUTHORIZATION, authorization);
            }
        }
    }

    public class ClientSecretRequestFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            String clientId = (String) requestContext.getConfiguration().getProperty("clientId");
            String clientSecret = (String) requestContext.getConfiguration().getProperty("clientSecret");
            if (clientSecret != null) {
                try {
                    String authorization = "Basic " + Base64.getEncoder().encodeToString(
                        (clientId + ":" + clientSecret).getBytes("utf-8")
                    );
                    requestContext.getHeaders().add(AUTHORIZATION, authorization);
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    protected class ClientInstallKey {
        public final String realm;
        public final String clientId;

        public ClientInstallKey(String realm, String clientId) {
            this.realm = realm;
            this.clientId = clientId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClientInstallKey that = (ClientInstallKey) o;

            if (!realm.equals(that.realm)) return false;
            return clientId.equals(that.clientId);

        }

        @Override
        public int hashCode() {
            int result = realm.hashCode();
            result = 31 * result + clientId.hashCode();
            return result;
        }
    }

    protected boolean disableAPISecurity;
    protected boolean configNetworkSecure;
    protected String configNetworkHost;
    protected int configNetworkWebserverPort;
    protected String externalAuthServerUrl;
    protected LoadingCache<ClientInstallKey, ClientInstall> clientInstallCache;
    protected UriBuilder keycloakHostUri;
    protected UriBuilder keycloakServiceUri;
    protected Client client;

    @Override
    public void prepare(Container container) {

        this.disableAPISecurity = container.getConfigBoolean(DISABLE_API_SECURITY, DISABLE_API_SECURITY_DEFAULT);
        this.configNetworkSecure = container.getConfigBoolean(NETWORK_SECURE, NETWORK_SECURE_DEFAULT);
        this.configNetworkHost = container.getConfig(NETWORK_HOST, NETWORK_HOST_DEFAULT);
        this.configNetworkWebserverPort = container.getConfigInteger(NETWORK_WEBSERVER_PORT, NETWORK_WEBSERVER_PORT_DEFAULT);

        externalAuthServerUrl = UrlUtil.url(
            this.configNetworkSecure ? "https" : "http",
            this.configNetworkHost,
            this.configNetworkWebserverPort,
            Constants.AUTH_PATH
        ).toString();

        LOG.info("External auth server URL is: " + externalAuthServerUrl);

        keycloakHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(container.getConfig(KEYCLOAK_HOST, KEYCLOAK_HOST_DEFAULT))
                .port(container.getConfigInteger(KEYCLOAK_PORT, KEYCLOAK_PORT_DEFAULT));

        keycloakServiceUri = keycloakHostUri.clone().replacePath(KEYCLOAK_CONTEXT_PATH);

        LOG.info("Preparing identity service for Keycloak host: " + keycloakServiceUri);

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

        this.client = clientBuilder
            .register(JacksonConfig.class)
            .register(new BearerAuthClientRequestFilter())
            .register(new ClientSecretRequestFilter())
            .build();

        clientInstallCache = createClientInstallCache();

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new IdentityResource(this)
        );

        SimpleProxyClientProvider proxyClient = new SimpleProxyClientProvider(keycloakHostUri.build());
        ProxyHandler proxyHandler = new ProxyHandler(
            proxyClient,
            container.getConfigInteger(KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
            ResponseCodeHandler.HANDLE_404
        );
        container.getService(ManagerWebService.class).getPrefixRoutes().put(AUTH_PATH, proxyHandler);
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

    protected ResteasyWebTarget getTarget() {
        return getTarget(keycloakServiceUri, null);
    }

    protected ResteasyWebTarget getTarget(UriBuilder uri, String accessToken) {
        ResteasyWebTarget target = ((ResteasyWebTarget) client.target(uri));
        if (accessToken != null) {
            target.property("accessToken", accessToken);
        }
        return target;
    }

    protected ResteasyWebTarget getTarget(UriBuilder uri, String clientId, String clientSecret) {
        ResteasyWebTarget target = getTarget(uri, null);
        if (clientId != null) {
            target.property("clientId", clientId);
        }
        if (clientSecret != null) {
            target.property("clientSecret", clientSecret);
        }
        return target;
    }

    protected Keycloak getKeycloak() {
        return getKeycloak(getTarget(keycloakServiceUri, null));
    }

    protected Keycloak getKeycloak(String accessToken) {
        return getKeycloak(getTarget(keycloakServiceUri, accessToken));
    }

    protected Keycloak getKeycloak(ResteasyWebTarget target) {
        return target.proxy(Keycloak.class);
    }

    protected void pingKeycloak() {
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

    public ClientInstall getClientInstall(String realm, String clientId) {
        try {
            return clientInstallCache.get(new ClientInstallKey(realm, clientId));
        } catch (Exception ex) {
            LOG.log(
                Level.INFO,
                "Error loading client '" + clientId + "' install for realm '" + realm + "' from identity provider",
                ex
            );
            return null;
        }
    }

    protected LoadingCache<ClientInstallKey, ClientInstall> createClientInstallCache() {
        CacheLoader<ClientInstallKey, ClientInstall> loader =
            new CacheLoader<ClientInstallKey, ClientInstall>() {
                public ClientInstall load(ClientInstallKey key) {
                    LOG.fine("Loading client '" + key.clientId + "' install details for realm '" + key.realm + "'");

                    ClientInstall clientInstall = getKeycloak().getClientInstall(key.realm, key.clientId);

                    // Make the public key usable
                    try {
                        clientInstall.setPublicKey(PemUtils.decodePublicKey(clientInstall.getPublicKeyPEM()));
                    } catch (Exception ex) {
                        throw new RuntimeException("Error decoding public key PEM for realm: " + clientInstall.getRealm(), ex);
                    }

                    // Must rewrite the auth-server URL to our external host and port, which
                    // we'll later reverse-proxy back to Keycloak
                    clientInstall.setAuthServerUrl(externalAuthServerUrl);

                    // Also correct the realm info URL at this time, this URL will be written by Keycloak
                    // as the issue into each token and we need to verify it
                    clientInstall.setRealmInfoUrl(
                        url(clientInstall.getAuthServerUrl(), "realms", clientInstall.getRealm()).toString()
                    );

                    return clientInstall;
                }
            };

        // TODO configurable? Or replace all of this with Observable.cache()?
        return CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, MINUTES)
            .build(loader);
    }
}