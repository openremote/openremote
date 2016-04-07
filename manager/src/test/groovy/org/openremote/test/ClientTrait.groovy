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
import org.glassfish.tyrus.client.ClientManager
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.openremote.container.message.MessageBrokerService
import org.openremote.manager.client.event.MessageReceivedEvent
import org.openremote.manager.client.event.ServerSendEvent
import org.openremote.manager.client.event.bus.EventBus
import org.openremote.manager.shared.event.Event
import org.openremote.manager.shared.event.Message
import org.openremote.manager.shared.http.RequestParams
import org.openremote.manager.shared.http.SuccessStatusCode
import org.spockframework.mock.IMockMethod
import spock.lang.Shared

import javax.websocket.*
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.UriBuilder
import java.lang.reflect.Method

trait ClientTrait {

    @Shared
    WebSocketContainer websocketContainer = ClientManager.createClient();

    static class EventEndpoint extends Endpoint {
        final EventBus eventBus;
        final ObjectMapper eventMapper;
        Session session;

        EventEndpoint(EventBus eventBus, ObjectMapper eventMapper) {
            this.eventBus = eventBus
            this.eventMapper = eventMapper;

            eventBus.register(ServerSendEvent.class, { serverSendEvent ->
                if (session == null)
                    return;
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

    def getWebsocketServerUrl(UriBuilder uriBuilder, String realm, String endpointPath, String accessToken) {
        uriBuilder.clone()
                .scheme("ws")
                .replacePath(MessageBrokerService.WEBSOCKET_PATH)
                .path(endpointPath)
                .queryParam("realm", realm)
                .queryParam("Authorization", "Bearer " + accessToken)
                .build();
    }

    def Session connect(EventEndpoint endpoint, WebTarget webTarget, String realm, String accessToken, String endpointPath) {
        def uri = getWebsocketServerUrl(webTarget.getUriBuilder(), realm, endpointPath, accessToken);
        def config = ClientEndpointConfig.Builder.create().build();
        websocketContainer.connectToServer(endpoint, config, uri);
    }

    def void callResourceProxy(String realm, def mockInvocation) {
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
            def resourceProxy = getClientTarget(realm).proxy(mockedResourceType);

            // Try to find out what the expected success status code is
            SuccessStatusCode successStatusCode =
                    mockedResourceMethod.getDeclaredAnnotation(SuccessStatusCode.class);
            int statusCode = successStatusCode != null ? successStatusCode.value() : 200;

            // Call the proxy
            def result = resourceProxy."$mockMethod.name"(args);

            // Pass the result to the callback, so it looks asynchronous for client code
            requestParams.callback.call(statusCode, null, result);
        }
    }

    abstract def ResteasyWebTarget getClientTarget(String realm);

}
