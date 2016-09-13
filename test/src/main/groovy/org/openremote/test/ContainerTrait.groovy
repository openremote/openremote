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
import elemental.json.JsonValue
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
import org.openremote.container.persistence.PersistenceService
import org.openremote.container.security.AuthForm
import org.openremote.container.security.IdentityService
import org.openremote.container.web.WebClient
import org.openremote.container.web.WebService
import org.openremote.manager.client.event.MessageReceivedEvent
import org.openremote.manager.client.event.ServerSendEvent
import org.openremote.manager.client.event.bus.EventBus
import org.openremote.manager.server.SampleDataService
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.agent.ConnectorService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.server.i18n.I18NService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.web.ManagerWebService
import org.openremote.manager.shared.event.Event
import org.openremote.manager.shared.event.Message
import org.openremote.manager.shared.http.Request
import org.openremote.manager.shared.http.RequestParams
import org.openremote.manager.shared.http.SuccessStatusCode
import org.spockframework.mock.IMockMethod

import javax.websocket.*
import javax.ws.rs.ClientErrorException
import javax.ws.rs.Produces
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import java.lang.reflect.Method
import java.util.stream.Stream

import static java.util.concurrent.TimeUnit.SECONDS
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

trait ContainerTrait {

    static class EventEndpoint extends Endpoint {
        final EventBus eventBus;
        final ObjectMapper eventMapper;
        Session session;

        EventEndpoint(EventBus eventBus, ObjectMapper eventMapper) {
            this.eventBus = eventBus
            this.eventMapper = eventMapper;

            eventBus.register(ServerSendEvent.class, { serverSendEvent ->
                if (session == null) {
                    return;
                }
                session.basicRemote.sendText(
                        eventMapper.writeValueAsString(serverSendEvent.event)
                );
            });
        }

        @Override
        void onOpen(Session session, EndpointConfig config) {
            this.session = session;
            session.addMessageHandler new MessageHandler.Whole<String>() {
                @Override
                void onMessage(String data) {
                    Event event = eventMapper.readValue(data, Event.class);
                    if (event.getType().equals(Event.getType(Message.class))) {
                        Message message = (Message) event;
                        eventBus.dispatch(new MessageReceivedEvent(message));
                    } else {
                        eventBus.dispatch(event);
                    }
                }
            }
        }

        @Override
        void onClose(Session session, CloseReason closeReason) {
            this.session = null;
        }

        public void close() {
            if (session) {
                session.close()
                Thread.sleep 250 // Give the server a chance to end the session before we move on to the next test
            }
        }
    }

    static class UnsupportedXMLHttpRequest implements Request.XMLHttpRequest {
        @Override
        String getResponseHeader(String header) {
            throw new UnsupportedOperationException("Can't access response headers in test framework")
        }
    }

    static class ResponseWrapperXMLHttpRequest implements Request.XMLHttpRequest {
        final Response response;

        ResponseWrapperXMLHttpRequest(Response response) {
            this.response = response
        }

        @Override
        String getResponseHeader(String header) {
            return response.getHeaderString(header);
        }
    }

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
                        new I18NService(),
                        new ManagerWebService(),
                        new ManagerIdentityService(),
                        new MessageBrokerService(),
                        new PersistenceService(),
                        new EventService(),
                        new ConnectorService(),
                        new AgentService(),
                        new AssetService(),
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
        return WebClient.registerDefaults(container, clientBuilder);
    }

    static UriBuilder serverUri(int serverPort) {
        UriBuilder.fromUri("")
                .scheme("http").host("localhost").port(serverPort);
    }

    static void stopContainer(Container container) {
        if (container != null) {
            container.stop();
        }
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client, UriBuilder serverUri) {
        WebClient.getTarget(client, serverUri.clone().build());
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client, UriBuilder serverUri, String realm) {
        WebClient.getTarget(client, serverUri.clone().replacePath(realm).build());
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client, UriBuilder serverUri, String realm, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(realm).build(), accessToken);
    }

    static ResteasyWebTarget getClientTarget(ResteasyClient client, UriBuilder serverUri, String realm, String path, String accessToken) {
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

    static ProducerTemplate getMessageProducerTemplate(Container container) {
        return container.getService(MessageBrokerService.class).getContext().createProducerTemplate();
    }

    static void addRoutes(Container container, RouteBuilder routeBuilder) {
        container.getService(MessageBrokerService.class).getContext().addRoutes(routeBuilder);
    }

    static def WebSocketContainer createWebsocketContainer() {
        return ClientManager.createClient()
    };

    static def getWebsocketServerUrl(UriBuilder uriBuilder, String realm, String endpointPath, String accessToken) {
        uriBuilder.clone()
                .scheme("ws")
                .replacePath(MessageBrokerService.WEBSOCKET_PATH)
                .path(endpointPath)
                .queryParam("realm", realm)
                .queryParam("Authorization", "Bearer " + accessToken);
    }

    static Session connect(WebSocketContainer websocketContainer, EventEndpoint endpoint, WebTarget webTarget, String realm, String accessToken, String endpointPath) {
        def websocketUrl = getWebsocketServerUrl(webTarget.getUriBuilder(), realm, endpointPath, accessToken);
        def config = ClientEndpointConfig.Builder.create().build();
        websocketContainer.connectToServer(endpoint, config, websocketUrl.build());
    }

    // This emulates how a GWT client calls a REST service, we intercept and route through  Resteasy Client proxy
    static def void callResourceProxy(ObjectMapper jsonMapper, ResteasyWebTarget clientTarget, def mockInvocation) {
        // If the first parameter of the method we want to call is RequestParams
        List<Object> args = mockInvocation.getArguments();
        IMockMethod mockMethod = mockInvocation.getMethod();
        if (args[0] instanceof RequestParams) {
            RequestParams requestParams = (RequestParams) args[0];

            // Get a Resteasy client proxy for the resource
            Class mockedResourceType = mockInvocation.getMockObject().getType();
            Method mockedResourceMethod = mockedResourceType.getDeclaredMethod(
                    mockMethod.name,
                    mockMethod.parameterTypes.toArray(new Class[mockMethod.parameterTypes.size()])
            );
            def resourceProxy = clientTarget.proxy(mockedResourceType);

            // Try to find out what the expected success status code is
            SuccessStatusCode successStatusCode =
                    mockedResourceMethod.getDeclaredAnnotation(SuccessStatusCode.class);
            int statusCode = successStatusCode != null ? successStatusCode.value() : 200;

            // Call the proxy
            Object result;
            boolean resultPassthrough = false;
            // Fake an XMLHttpRequest for later (we can only access headers if there is an exception...)
            Request.XMLHttpRequest xmlHttpRequest = new UnsupportedXMLHttpRequest();
            try {
                result = resourceProxy."$mockMethod.name"(args);
            } catch (ClientErrorException ex) {
                statusCode = ex.getResponse().getStatus();
                xmlHttpRequest = new ResponseWrapperXMLHttpRequest(ex.getResponse());
                try {
                    result = ex.getResponse().readEntity(String.class);
                } catch (IllegalStateException ise) {
                    // Ignore, this happens when the response is closed already
                }
                resultPassthrough = true;
            }

            String responseText = null;

            if (result != null) {
                // If the method produces JSON, we need to turn whatever the proxy delivered back into JSON string
                Produces producesAnnotation = mockedResourceMethod.getDeclaredAnnotation(Produces.class);
                if (!resultPassthrough
                        && producesAnnotation != null
                        && producesAnnotation.value()[0] != null
                        && producesAnnotation.value()[0].equals(APPLICATION_JSON)) {
                    // Handle elemental JsonValue special, don't use Jackson
                    if (result instanceof JsonValue) {
                        JsonValue jsonValue = (JsonValue) result;
                        responseText = jsonValue.toJson();
                    } else {
                        responseText = jsonMapper.writeValueAsString(result);
                    }
                } else {
                    responseText = result.toString();
                }
            }

            // Pass the result to the callback, so it looks asynchronous for client code
            requestParams.callback.call(statusCode, xmlHttpRequest, responseText);
        }
    }


}
