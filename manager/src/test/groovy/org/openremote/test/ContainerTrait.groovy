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
package org.openremote.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.junit.After
import org.junit.Before
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
    }

    @After
    cleanupContainer() {
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
