package org.openremote.test.event

import org.openremote.manager.shared.event.Message
import org.openremote.test.ContainerTrait
import spock.lang.Specification

class EventEchoTest extends Specification implements ContainerTrait {

    def "Ping/pong event service"() {
        given: "A sample message"
        def sampleMessage = new Message("Hello World");
        and: "the expected answer messages"
        messageReceiver.reset();
        messageReceiver.expectedBodiesReceived(
                JSON.writeValueAsString(sampleMessage)
        )

        when: "sending the message"
        messageProducer.sendBody(
                JSON.writeValueAsString(sampleMessage)
        );

        then: "the expected messages should eventually arrive"
        messageReceiver.assertIsSatisfied();
    }
}
