package org.openremote.test

import org.glassfish.tyrus.client.ClientManager
import org.openremote.container.message.MessageBrokerService
import spock.lang.Shared

import javax.websocket.*
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.UriBuilder
import java.util.concurrent.TimeoutException

trait WebsocketClientTrait {

    @Shared
    WebSocketContainer websocketContainer = ClientManager.createClient();

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

    def getWebsocketServerUrl(UriBuilder uriBuilder, String realm, String endpointPath, String accessToken) {
        uriBuilder.clone()
                .scheme("ws")
                .replacePath(MessageBrokerService.WEBSOCKET_PATH)
                .path(endpointPath)
                .queryParam("realm", realm)
                .queryParam("Authorization", "Bearer " + accessToken)
                .build();
    }

    def Session connect(TestClient client, WebTarget webTarget, String realm, String accessToken, String endpointPath) {
        def uri = getWebsocketServerUrl(webTarget.getUriBuilder(), realm, endpointPath, accessToken);
        def config = ClientEndpointConfig.Builder.create().build();
        websocketContainer.connectToServer(client, config, uri);
    }
}
