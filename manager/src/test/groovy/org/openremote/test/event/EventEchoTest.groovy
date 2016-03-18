package org.openremote.test.event

import org.openremote.manager.server.event.EventService
import org.openremote.manager.shared.event.Message
import org.openremote.test.ContainerTrait
import org.openremote.test.WebsocketClientTrait
import spock.lang.Specification

import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.server.Constants.MASTER_REALM

class EventEchoTest extends Specification implements ContainerTrait, WebsocketClientTrait {

    def "Ping/pong event service"() {
        given: "An authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(realm, MANAGER_CLIENT_ID, "test", "test")

        and: "A sample message"
        def sampleMessage = new Message("Hello World");
        and: "a test client"
        def testClient = new WebsocketClientTrait.TestClient(1);

        when: "connecting to the websocket"
        def session = connect(
                testClient,
                getClientTarget(),
                accessTokenResponse.getToken(),
                EventService.WEBSOCKET_EVENTS
        );

        and: "sending a message"
        session.basicRemote.sendText(JSON.writeValueAsString(sampleMessage));

        and: "waiting for the answers"
        testClient.awaitMessages(false);

        then: "the expected messages should eventually arrive"
        testClient.messages[0] == JSON.writeValueAsString(sampleMessage); // It's an echo

        when: "we do the same again after resetting the test client"
        testClient.reset(1);

        then: "no messages should exist"
        testClient.messages.size() == 0

        when: "sending a message"
        session.basicRemote.sendText(JSON.writeValueAsString(sampleMessage));

        and: "waiting for the answers"
        testClient.awaitMessages();

        then: "the expected messages should eventually arrive"
        testClient.messages[0] == JSON.writeValueAsString(sampleMessage); // It's an echo
    }
}
