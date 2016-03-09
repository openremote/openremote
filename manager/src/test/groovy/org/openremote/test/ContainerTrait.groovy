package org.openremote.test

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.junit.After
import org.junit.Before
import org.openremote.container.Constants
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.web.WebClient
import org.openremote.container.web.WebService
import org.openremote.manager.server.web.ManagerWebService

import javax.ws.rs.client.Client
import javax.ws.rs.core.UriBuilder
import java.util.stream.Stream

import static java.util.concurrent.TimeUnit.SECONDS

trait ContainerTrait {

    Container container;
    int serverPort;
    Client client;
    UriBuilder serverApiUri;

    @Before
    setupContainer() {
        serverPort = findEphemeralPort();

        def config = [
                (WebService.WEB_SERVER_LISTEN_PORT): Integer.toString(serverPort),
                (Constants.NETWORK_WEBSERVER_PORT) : Integer.toString(serverPort)
        ];

        Stream<ContainerService> services = Stream.concat(
                Arrays.stream(getContainerServices()),
                Stream.of(
                        new ManagerWebService()
                )
        );

        container = new Container(config, services);

        ResteasyClientBuilder clientBuilder =
                new ResteasyClientBuilder()
                        .establishConnectionTimeout(2, SECONDS)
                        .socketTimeout(10, SECONDS)
                        .connectionPoolSize(10);

        client = WebClient.registerDefaults(clientBuilder).build();

        serverApiUri = UriBuilder.fromUri("")
                .scheme("http").host("localhost").port(serverPort)
                .path(WebService.API_PATH);

        container.startBackground();
    }

    @After
    cleanupContainer() {
        if (container)
            container.stop();
    }

    def ResteasyWebTarget getTarget() {
        WebClient.getTarget(client, serverApiUri.build());
    }

    def ResteasyWebTarget getTarget(String path) {
        WebClient.getTarget(client, serverApiUri.path(path).build());
    }

    def findEphemeralPort() {
        ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLocalHost());
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    abstract ContainerService[] getContainerServices();
}
