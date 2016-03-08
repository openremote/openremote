package org.openremote.test;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.openremote.container.Constants;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class ServerTest extends IntegrationTest {

    private static final Logger LOG = Logger.getLogger(ServerTest.class.getName());

    protected int serverPort;
    protected Container container;
    protected Client client;
    protected UriBuilder serverApiUri;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        this.serverPort = findEphemeralPort();

        Map<String, String> config = new HashMap<>();

        config.put(WebService.WEB_SERVER_LISTEN_PORT, Integer.toString(this.serverPort));
        config.put(Constants.NETWORK_WEBSERVER_PORT, Integer.toString(this.serverPort));

        container = new Container(
            config,
            Stream.concat(
                Stream.of(getContainerServices()),
                Stream.of(new org.openremote.manager.server.web.ManagerWebService())
            )
        );

        ResteasyClientBuilder clientBuilder =
            new ResteasyClientBuilder()
                .establishConnectionTimeout(2, TimeUnit.SECONDS)
                .socketTimeout(10, TimeUnit.SECONDS)
                .connectionPoolSize(10);

        this.client = WebClient.registerDefaults(clientBuilder).build();

        serverApiUri = UriBuilder.fromUri("")
            .scheme("http").host("localhost").port(serverPort)
            .path(WebService.API_PATH);

        container.startBackground();
    }

    @Override
    public void tearDown() throws Exception {
        if (container != null)
            container.stop();
        super.tearDown();
    }

    protected <T> T getTargetResource(Class<T> resourceType) {
        return WebClient.getTarget(client, serverApiUri.build()).proxy(resourceType);
    }

    protected Integer findEphemeralPort() {
        try {
            ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLocalHost());
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    abstract protected ContainerService[] getContainerServices();
}
