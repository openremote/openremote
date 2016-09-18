package org.openremote.test.event

import org.openremote.manager.server.event.EventService
import org.openremote.manager.shared.event.Message
import org.openremote.test.ContainerTrait
import org.openremote.test.WebsocketClientTrait
import spock.lang.Ignore
import spock.lang.Specification

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

// TODO Fix this when we do Asset testing
@Ignore
class EventEchoTest extends Specification implements ContainerTrait, WebsocketClientTrait {

    def "Ping/pong event service"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(container, realm, MANAGER_CLIENT_ID, "test", "test")

        and: "a test client target"
        def client = createClient(container).build();
        def serverUri = serverUri(serverPort);
        def clientTarget = getClientTarget(client, serverUri, realm, accessTokenResponse.getToken());

        and: "A sample message"
        def sampleMessage = new Message("Hello World");
        and: "a test client"
        def testClient = new WebsocketClientTrait.TestClient(1);

        when: "connecting to the websocket"
        def session = connect(
                testClient,
                clientTarget,
                realm,
                accessTokenResponse.getToken(),
                EventService.WEBSOCKET_EVENTS
        );

        and: "sending a message"
        session.basicRemote.sendText(container.JSON.writeValueAsString(sampleMessage));

        and: "waiting for the answers"
        testClient.awaitMessages(false);

        then: "the expected messages should eventually arrive"
        container.JSON.readValue(testClient.messages[0] as String, Message.class)
                .getBody().startsWith("Hello from server");

        when: "we do the same again after resetting the test client"
        testClient.reset(1);

        then: "no messages should exist"
        testClient.messages.size() == 0

        when: "sending a message"
        session.basicRemote.sendText(container.JSON.writeValueAsString(sampleMessage));

        and: "waiting for the answers"
        testClient.awaitMessages();

        then: "the expected messages should eventually arrive"
        container.JSON.readValue(testClient.messages[0] as String, Message.class)
                .getBody().startsWith("Hello from server");

        and: "the server should be stopped"
        stopContainer(container);
    }
}
