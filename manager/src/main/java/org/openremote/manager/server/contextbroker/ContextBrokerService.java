package org.openremote.manager.server.contextbroker;


public class ContextBrokerService {
    /*

    private static final Logger LOG = Logger.getLogger(ContextBrokerService.class.getName());

    public static final String CONTEXT_BROKER_HOST = "CONTEXT_BROKER_HOST";
    public static final String CONTEXT_BROKER_HOST_DEFAULT = "192.168.99.100";
    public static final String CONTEXT_BROKER_PORT = "CONTEXT_BROKER_PORT";
    public static final int CONTEXT_BROKER_PORT_DEFAULT = 8082;
    public static final String CONTEXT_BROKER_CONNECT_TIMEOUT_MS = "CONTEXT_BROKER_CONNECT_TIMEOUT_MS";
    public static final int CONTEXT_BROKER_CONNECT_TIMEOUT_MS_DEFAULT = 2000;
    public static final String CONTEXT_BROKER_REQUEST_TIMEOUT_MS = "CONTEXT_BROKER_REQUEST_TIMEOUT_MS";
    public static final int CONTEXT_BROKER_REQUEST_TIMEOUT_MS_DEFAULT = 10000;
    public static final String CONTEXT_BROKER_MAX_POOL_SIZE = "CONTEXT_BROKER_MAX_POOL_SIZE";
    public static final int CONTEXT_BROKER_MAX_POOL_SIZE_DEFAULT = 20;

    protected NgsiClient client;

    public void start(Vertx vertx, JsonObject config) {
        String host = config.getString(CONTEXT_BROKER_HOST, CONTEXT_BROKER_HOST_DEFAULT);
        int port = config.getInteger(CONTEXT_BROKER_PORT, CONTEXT_BROKER_PORT_DEFAULT);
        LOG.info("Starting context broker service for NGSI server: " + host + ":" + port);

        RestClientOptions clientOptions = new RestClientOptions()
            .setConnectTimeout(config.getInteger(CONTEXT_BROKER_CONNECT_TIMEOUT_MS, CONTEXT_BROKER_CONNECT_TIMEOUT_MS_DEFAULT))
            .setGlobalRequestTimeout(config.getInteger(CONTEXT_BROKER_REQUEST_TIMEOUT_MS, CONTEXT_BROKER_REQUEST_TIMEOUT_MS_DEFAULT))
            .setDefaultHost(host)
            .setDefaultPort(port)
            .setKeepAlive(true)
            .setMaxPoolSize(config.getInteger(CONTEXT_BROKER_MAX_POOL_SIZE, CONTEXT_BROKER_MAX_POOL_SIZE_DEFAULT));

        client = new NgsiClient(vertx, clientOptions);
    }

    public void stop() {
        LOG.info("Stopping context broker service...");
        if (client != null) {
            client.close();
        }
    }

    public NgsiClient getClient() {
        return client;
    }
*/
}
