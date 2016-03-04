package org.openremote.manager.server2.identity;


import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.server2.ManagerWebService;

import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

    protected KeycloakClient keycloakClient;


    @Override
    public void prepare(Container container) {

        UriBuilder serverUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(container.getConfig(KEYCLOAK_HOST, KEYCLOAK_HOST_DEFAULT))
                .port(container.getConfigInteger(KEYCLOAK_PORT, KEYCLOAK_PORT_DEFAULT));

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

        keycloakClient = new KeycloakClient(serverUri, clientBuilder);

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new IdentityResource(keycloakClient)
        );
    }

    @Override
    public void start(Container container) {

    }

    @Override
    public void stop(Container container) {
        if (keycloakClient != null)
            keycloakClient.close();
    }
}