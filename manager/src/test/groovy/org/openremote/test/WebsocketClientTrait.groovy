package org.openremote.test

import org.glassfish.tyrus.client.ClientManager
import org.openremote.container.message.MessageBrokerService
import spock.lang.Shared

import javax.websocket.ClientEndpointConfig
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.Session
import javax.websocket.WebSocketContainer
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.UriBuilder
import java.util.concurrent.TimeoutException

trait WebsocketClientTrait {

    @Shared
    WebSocketContainer websocketContainer = ClientManager.createClient();

    static class BearerAuthConfigurator extends ClientEndpointConfig.Configurator {

        final protected String accessToken;

        BearerAuthConfigurator(String accessToken) {
            this.accessToken = accessToken
        }

        public void beforeRequest(Map<String, List<String>> headers) {
            if (accessToken != null)
                headers.put("Authorization", ["Bearer " + accessToken]);
        }
    }

    public static class TestClient extends Endpoint {

        def TIMEOUT_MILLIS = 5000;
        def startTime;
        def session;
        def messages = [];

        protected int expectedMessageCount;

        TestClient(int expectedMessageCount) {
            this.expectedMessageCount = expectedMessageCount
        }

        @Override
        void onOpen(Session session, EndpointConfig config) {
            this.session = session;
            session.addMessageHandler new MessageHandler.Whole<String>() {
                @Override
                void onMessage(String message) {
                    TestClient.this.messages << message
                }
            }
        }

        @Override
        void onClose(Session session, CloseReason closeReason) {
            this.session = null;
        }

        def reset(int expectedMessageCount) {
            this.expectedMessageCount = expectedMessageCount;
            this.messages = [];
            this.startTime = 0;
        }

        def awaitMessages() {
            awaitMessages(true);
        }

        def awaitMessages(boolean closeSessionOnCompletion) {
            startTime = System.currentTimeMillis();
            try {
                while (!messages || messages.size() < expectedMessageCount) {
                    if (isTimeout()) {
                        throw new TimeoutException("Timeout while waiting for expected websocket messages: " + expectedMessageCount)
                    }
                    Thread.sleep 250
                }
            } finally {
                if (session && closeSessionOnCompletion) {
                    session.close();
                    // Give the server a chance to end the session properly, we clear the member in onClose()
                    Thread.sleep 250
                    startTime = System.currentTimeMillis();
                    while (session) {
                        if (isTimeout()) {
                            throw new TimeoutException("Timeout while waiting for session to close")
                        }
                        Thread.sleep 250
                    }
                }
            }
        }

        def isTimeout() {
            System.currentTimeMillis() > (startTime + TIMEOUT_MILLIS);
        }
    }

    def getWebsocketServerUrl(UriBuilder uriBuilder, String endpointPath) {
        uriBuilder.clone()
                .scheme("ws")
                .path(MessageBrokerService.WEBSOCKET_PATH)
                .path(endpointPath)
                .build();
    }

    def ClientEndpointConfig getWebsocketClientEndpointConfig(String accessToken) {
        return ClientEndpointConfig.Builder.create()
                .configurator(new BearerAuthConfigurator(accessToken))
                .build();
    }

    def Session connect(TestClient client, WebTarget webTarget, String accessToken, String endpointPath) {
        def uri = getWebsocketServerUrl(webTarget.getUriBuilder(), endpointPath);
        def config = getWebsocketClientEndpointConfig(accessToken);
        websocketContainer.connectToServer(client, config, uri);
    }
}
