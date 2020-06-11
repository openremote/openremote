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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test

import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.glassfish.tyrus.client.ClientManager
import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.keycloak.representations.AccessTokenResponse
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.security.PasswordAuthForm
import org.openremote.container.security.IdentityService
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.web.DefaultWebsocketComponent
import org.openremote.container.web.WebClient
import org.openremote.manager.web.ManagerWebService
import org.openremote.model.Constants

import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.Session
import javax.websocket.WebSocketContainer
import javax.ws.rs.core.UriBuilder

import static java.util.concurrent.TimeUnit.SECONDS

trait ContainerTrait {

    private static Container container

    static Container startContainer(Map<String, String> config, Iterable<ContainerService> services) {
        container = new Container(config, services)
        container.startBackground()
        container
    }

    static boolean isContainerRunning() {
        container.running
    }

    static ResteasyClientBuilder createClient() {
        ResteasyClientBuilder clientBuilder =
                new ResteasyClientBuilder()
                        .establishConnectionTimeout(2, SECONDS)
                        .socketTimeout(15, SECONDS)
                        .connectionPoolSize(10)
        return WebClient.registerDefaults(clientBuilder)
    }

    static UriBuilder serverUri(int serverPort) {
        UriBuilder.fromUri("")
                .scheme("http").host("localhost").port(serverPort)
    }

    static void stopContainer(Container container) {
        if (container != null) {
            container.stop()
        }
    }

    static ResteasyWebTarget getClientTarget(UriBuilder serverUri, String accessToken) {
        WebClient.getTarget(createClient().build(), serverUri.build(), accessToken, null, null)
    }

    static ResteasyWebTarget getClientApiTarget(UriBuilder serverUri, String realm) {
        WebClient.getTarget(createClient().build(), serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).build(), null, null, null)
    }

    static ResteasyWebTarget getClientApiTarget(UriBuilder serverUri, String realm, String accessToken) {
        WebClient.getTarget(createClient().build(), serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).build(), accessToken, null, null)
    }

    static ResteasyWebTarget getClientApiTarget(UriBuilder serverUri, String realm, String path, String accessToken) {
        WebClient.getTarget(createClient().build(), serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).path(path).build(), accessToken, null, null)
    }

    static ResteasyWebTarget getClientApiTarget(ResteasyClient client, UriBuilder serverUri, String realm, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).build(), accessToken, null, null)
    }

    static ResteasyWebTarget getClientApiTarget(ResteasyClient client, UriBuilder serverUri, String realm, String path, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).path(path).build(), accessToken, null, null)
    }

    static int findEphemeralPort() {
        ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLoopbackAddress())
        int port = socket.getLocalPort()
        socket.close()
        return port
    }

    static AccessTokenResponse authenticate(Container container, String realm, String clientId, String username, String password) {
        ((KeycloakIdentityProvider)container.getService(IdentityService.class).getIdentityProvider()).getKeycloak()
                .getAccessToken(realm, new PasswordAuthForm(clientId, username, password))
    }

    static ProducerTemplate getMessageProducerTemplate(Container container) {
        return container.getService(MessageBrokerService.class).getContext().createProducerTemplate()
    }

    static void addRoutes(Container container, RouteBuilder routeBuilder) {
        container.getService(MessageBrokerService.class).getContext().addRoutes(routeBuilder)
    }

    static WebSocketContainer createWebsocketClient() {
        return ClientManager.createClient()
    }

    static getWebsocketServerUrl(UriBuilder uriBuilder, String endpointPath, String realm, String accessToken) {
        uriBuilder.clone()
                .scheme("ws")
                .replacePath(DefaultWebsocketComponent.WEBSOCKET_PATH)
                .path(endpointPath)
                .queryParam(Constants.REQUEST_HEADER_REALM, realm)
                .queryParam("Authorization", "Bearer " + accessToken)
    }

    static Session connect(WebSocketContainer websocketContainer, Endpoint endpoint, UriBuilder serverUri, String endpointPath, String realm, String accessToken) {
        def websocketUrl = getWebsocketServerUrl(serverUri, endpointPath, realm, accessToken)
        def config = ClientEndpointConfig.Builder.create().build()
        websocketContainer.connectToServer(endpoint, config, websocketUrl.build())
    }
}
