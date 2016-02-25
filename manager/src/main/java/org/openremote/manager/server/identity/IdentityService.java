package org.openremote.manager.server.identity;


import com.hubrick.vertx.rest.RestClientOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.logging.Logger;

import static org.openremote.manager.server.Constants.*;

public class IdentityService {

    private static final Logger LOG = Logger.getLogger(IdentityService.class.getName());

    public static final String IDENTITY_PROVIDER_HOST = "IDENTITY_PROVIDER_HOST";
    public static final String IDENTITY_PROVIDER_HOST_DEFAULT = "192.168.99.100";
    public static final String IDENTITY_PROVIDER_PORT = "IDENTITY_PROVIDER_PORT";
    public static final int IDENTITY_PROVIDER_PORT_DEFAULT = 8081;
    public static final String IDENTITY_PROVIDER_CONNECT_TIMEOUT_MS = "IDENTITY_PROVIDER_CONNECT_TIMEOUT_MS";
    public static final int IDENTITY_PROVIDER_CONNECT_TIMEOUT_MS_DEFAULT = 2000;
    public static final String IDENTITY_PROVIDER_REQUEST_TIMEOUT_MS = "IDENTITY_PROVIDER_REQUEST_TIMEOUT_MS";
    public static final int IDENTITY_PROVIDER_REQUEST_TIMEOUT_MS_DEFAULT = 10000;
    public static final String IDENTITY_PROVIDER_MAX_POOL_SIZE = "IDENTITY_PROVIDER_MAX_POOL_SIZE";
    public static final int IDENTITY_PROVIDER_MAX_POOL_SIZE_DEFAULT = 20;

    protected KeycloakClient keycloakClient;
    protected boolean devMode;
    protected boolean configNetworkSecure;
    protected String configNetworkHost;
    protected int configNetworkWebserverPort;

    public void start(Vertx vertx, JsonObject config) {
        this.devMode = config.getBoolean(DEV_MODE, DEV_MODE_DEFAULT);
        this.configNetworkSecure = config.getBoolean(NETWORK_SECURE, NETWORK_SECURE_DEFAULT);
        this.configNetworkHost = config.getString(NETWORK_HOST, NETWORK_HOST_DEFAULT);
        this.configNetworkWebserverPort = config.getInteger(NETWORK_WEBSERVER_PORT, NETWORK_WEBSERVER_PORT_DEFAULT);

        String host = config.getString(IDENTITY_PROVIDER_HOST, IDENTITY_PROVIDER_HOST_DEFAULT);
        int port = config.getInteger(IDENTITY_PROVIDER_PORT, IDENTITY_PROVIDER_PORT_DEFAULT);
        LOG.info("Starting identity service for Keycloak server: " + host + ":" + port);

        RestClientOptions clientOptions = new RestClientOptions()
            .setConnectTimeout(config.getInteger(IDENTITY_PROVIDER_CONNECT_TIMEOUT_MS, IDENTITY_PROVIDER_CONNECT_TIMEOUT_MS_DEFAULT))
            .setGlobalRequestTimeout(config.getInteger(IDENTITY_PROVIDER_REQUEST_TIMEOUT_MS, IDENTITY_PROVIDER_REQUEST_TIMEOUT_MS_DEFAULT))
            .setDefaultHost(host)
            .setDefaultPort(port)
            .setKeepAlive(true)
            .setMaxPoolSize(config.getInteger(IDENTITY_PROVIDER_MAX_POOL_SIZE, IDENTITY_PROVIDER_MAX_POOL_SIZE_DEFAULT));

        keycloakClient = new KeycloakClient(vertx, clientOptions);
    }

    public void stop() {
        LOG.info("Stopping identity service...");
        if (keycloakClient != null) {
            keycloakClient.close();
        }
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

    public KeycloakClient getKeycloakClient() {
        return keycloakClient;
    }
}