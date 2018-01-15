package org.openremote.test.flow

import org.openremote.model.flow.Flow
import org.openremote.model.flow.Node
import org.openremote.model.flow.Wire
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

class FlowModelTest extends Specification implements ManagerContainerTrait {

    def "Ignore duplicate wires"() {

        given: "a flow"
        Flow flow = new Flow("Test Flow", "123")

        when: "duplicate wires are added"
        flow.addWire(new Wire("a", "b"))
        flow.addWire(new Wire("a", "b"))

        then: "only one wire should be present"
        flow.getWires().length == 1

    }

    def "Throw when duplicate constructor wires"() {

        when: "duplicate wires are provided as constructor arguments"
        new Flow("foo", "123", new Node[0], [new Wire("a", "b"), new Wire("a", "b")] as Wire[])

        then: "an exception should be thrown"
        thrown(IllegalArgumentException)
    }

}
