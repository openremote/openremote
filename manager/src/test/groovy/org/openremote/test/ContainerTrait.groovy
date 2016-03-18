package org.openremote.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.junit.After
import org.junit.Before
import org.keycloak.representations.AccessTokenResponse
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerContext
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.security.AuthForm
import org.openremote.container.security.IdentityService
import org.openremote.container.web.WebClient
import org.openremote.container.web.WebService
import org.openremote.manager.server.SampleDataService
import org.openremote.manager.server.assets.AssetsService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.server.security.ManagerIdentityService
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
    MessageBrokerContext messageClientContext;
    ProducerTemplate messageProducer;
    MockEndpoint messageReceiver;

    static class MockRouteBuilder extends RouteBuilder {

        final String eventsUrl;

        MockRouteBuilder(String eventsUrl) {
            this.eventsUrl = eventsUrl
        }

        @Override
        public void configure() throws Exception {
            from("direct:sendEvent")
                    .to(eventsUrl);

            from(eventsUrl)
                    .to('log:EVENT_RECEIVED: ${body}')
                    .to("mock:eventReceiver");
        }
    }

    @Before
    setupContainer() {
        serverPort = findEphemeralPort();

        def config = [
                (WebService.WEBSERVER_LISTEN_PORT)               : Integer.toString(serverPort),
                (IdentityService.IDENTITY_NETWORK_HOST)          : IdentityService.KEYCLOAK_HOST_DEFAULT,
                (IdentityService.IDENTITY_NETWORK_WEBSERVER_PORT): Integer.toString(IdentityService.KEYCLOAK_PORT_DEFAULT)
        ];

        Stream<ContainerService> services = Stream.concat(
                Arrays.stream(getContainerServices()),
                Stream.of(
                        new ManagerWebService(),
                        new ManagerIdentityService(),
                        new MessageBrokerService(),
                        new EventService(),
                        new AssetsService(),
                        new MapService(),
                        new SampleDataService()
                )
        );

        container = new Container(config, services);

        ResteasyClientBuilder clientBuilder =
                new ResteasyClientBuilder()
                        .establishConnectionTimeout(2, SECONDS)
                        .socketTimeout(10, SECONDS)
                        .connectionPoolSize(10);

        client = prepareClient(WebClient.registerDefaults(container, clientBuilder)).build();

        serverApiUri = UriBuilder.fromUri("")
                .scheme("http").host("localhost").port(serverPort);

        container.startBackground();

        // Websocket/message broker test client using Camel
        messageClientContext = new MessageBrokerContext();

        messageProducer = messageClientContext.createProducerTemplate();
        messageProducer.setDefaultEndpoint(messageClientContext.getEndpoint("direct:sendEvent"));

        messageReceiver = messageClientContext.getEndpoint("mock:eventReceiver", MockEndpoint.class);
        messageReceiver.setResultWaitTime(5000);

        messageClientContext.addRoutes(new MockRouteBuilder(getWebsocketUrl(EventService.WEBSOCKET_EVENTS)));

        messageClientContext.start()
    }

    @After
    cleanupContainer() {
        if (messageClientContext)
            messageClientContext.stop();
        if (container)
            container.stop();
    }

    ObjectMapper getJSON() {
        container.JSON;
    }

    def prepareClient(ResteasyClientBuilder clientBuilder) {
        return clientBuilder;
    }

    def ResteasyWebTarget getClientTarget() {
        WebClient.getTarget(client, serverApiUri.clone().build());
    }

    def ResteasyWebTarget getClientTarget(String realm) {
        WebClient.getTarget(client, serverApiUri.clone().replacePath(realm).build());
    }

    def ResteasyWebTarget getClientTarget(String realm, String accessToken) {
        WebClient.getTarget(client, serverApiUri.clone().replacePath(realm).build(), accessToken);
    }

    def ResteasyWebTarget getClientTarget(String realm, String path, String accessToken) {
        WebClient.getTarget(client, serverApiUri.clone().replacePath(realm).path(path).build(), accessToken);
    }

    def getWebSocketScheme() {
        "ahc-ws";
    }

    def getWebsocketUrl(String endpoint) {
        getClientTarget().getUriBuilder().clone()
                .scheme(webSocketScheme)
                .path(MessageBrokerService.WEBSOCKET_PATH)
                .path(endpoint)
                .build().toString();
    }

    def findEphemeralPort() {
        ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLocalHost());
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    AccessTokenResponse authenticate(String realm, String clientId, String username, String password) {
        container.getService(ManagerIdentityService.class).getKeycloak()
                .getAccessToken(realm, new AuthForm(clientId, username, password));
    }

    def ContainerService[] getContainerServices() {
        []
    }
}
