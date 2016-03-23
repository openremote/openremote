package org.openremote.test

import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.keycloak.representations.AccessTokenResponse
import org.openremote.container.Container
import org.openremote.container.ContainerService
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

import javax.ws.rs.core.UriBuilder
import java.util.stream.Stream

import static java.util.concurrent.TimeUnit.SECONDS

// TODO We should migrate all tests to use this init technique instead of the @Before etc. setup
trait ManualContainerTrait {

    static Map<String, String> defaultConfig(int serverPort) {
        [
                (WebService.WEBSERVER_LISTEN_PORT)               : Integer.toString(serverPort),
                (IdentityService.IDENTITY_NETWORK_HOST)          : IdentityService.KEYCLOAK_HOST_DEFAULT,
                (IdentityService.IDENTITY_NETWORK_WEBSERVER_PORT): Integer.toString(IdentityService.KEYCLOAK_PORT_DEFAULT)
        ]
    };

    static Stream<ContainerService> defaultServices(ContainerService... additionalServices) {
        Stream.concat(
                Arrays.stream(additionalServices),
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
    }

    static Container startContainer(Map<String, String> config, Stream<ContainerService> services) {
        def container = new Container(config, services);
        container.startBackground();
        container;
    }

    static ResteasyClientBuilder createClient(Container container) {
        ResteasyClientBuilder clientBuilder =
                new ResteasyClientBuilder()
                        .establishConnectionTimeout(2, SECONDS)
                        .socketTimeout(10, SECONDS)
                        .connectionPoolSize(10);
        WebClient.registerDefaults(container, clientBuilder);
    }

    static UriBuilder serverUri(int serverPort) {
        UriBuilder.fromUri("")
                .scheme("http").host("localhost").port(serverPort);
    }

    static void stopContainer(Container container) {
        container.stop();
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client, UriBuilder serverUri) {
        WebClient.getTarget(client, serverUri.clone().build());
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client, UriBuilder serverUri, String realm) {
        WebClient.getTarget(client, serverUri.clone().replacePath(realm).build());
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client,UriBuilder serverUri, String realm, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(realm).build(), accessToken);
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client,UriBuilder serverUri, String realm, String path, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(realm).path(path).build(), accessToken);
    }

    static int findEphemeralPort() {
        ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLocalHost());
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    static AccessTokenResponse authenticate(Container container, String realm, String clientId, String username, String password) {
        container.getService(ManagerIdentityService.class).getKeycloak()
                .getAccessToken(realm, new AuthForm(clientId, username, password));
    }
}
