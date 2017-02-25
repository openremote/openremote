package org.openremote.test.agent

import elemental.json.Json
import elemental.json.JsonObject
import elemental.json.JsonType
import org.openremote.agent3.protocol.simulator.SimulatorProtocol
import org.openremote.container.message.MessageBrokerService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.server.setup.ManagerDemoSetup
import org.openremote.manager.server.setup.SetupService
import org.openremote.model.AttributeRef
import org.openremote.model.AttributeValueChange
import org.openremote.model.Attributes
import org.openremote.model.asset.Color
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.agent3.protocol.Protocol.ACTUATOR_TOPIC

class AgentDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check agent and thing deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, initialDelay: 3)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetService = container.getService(AssetService.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1Toggle").asBoolean()
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1Dimmer").getType() == JsonType.NULL // No initial value!
            assert new Color(simulatorProtocol.getState(managerDemoSetup.thingId, "light1Color") as JsonObject) == new Color(88, 123, 88)
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1PowerConsumption").asNumber() == 12.345d
        }

        when: "a client wants to change a thing attributes' value, triggering an actuator"
        conditions = new PollingConditions(timeout: 3, initialDelay: 2)
        /* TODO What's the call path for thing attribute value updates and therefore the asset client API?

            Proposal:

                1. Receive AttributeValueChange on client API

                2. Handle security and verify the message is for a linked agent/thing/attribute in the asset database

                3. Send the AttributeValueChange to the Protocol through ACTUATOR_TOPIC and trigger device/service call

                4a. We assume that most protocol implementations are not using fire-and-forget but request/reply
                    communication. Thus, an AttributeValueChange has no further consequences besides triggering an
                    actuator. We expect that a sensor "response" will let us know "soon" if the call was successful.
                    Only a sensor value change will result in an update of the asset database state. The window of
                    inconsistency we can accept depends on how "soon" a protocol typically responds.

                4b. Alternatively, if the updated attribute is configured with the "forceUpdate" meta item, we write
                    the new attribute value directly into the asset database after triggering the actuator. This flag
                    is useful if the protocol does not reflect actuator changes "immediately". For example, if we
                    send a value to a light dimmer actuator, does the light dimmer also have a sensor that responds
                    quickly with the new value? If the device/service does not reply to value changes, we can force
                    an update of the "current state" in our database and simply assume that the actuator call was
                    successful.

                4c. Should "forceUpdate" be the default behavior?
        */
        def light1DimmerChange = new AttributeValueChange(
                new AttributeRef(managerDemoSetup.thingId, "light1Dimmer"), Json.create(66)
        )
        container.getService(MessageBrokerService.class).getProducerTemplate().sendBody(
                ACTUATOR_TOPIC, light1DimmerChange
        )

        then: "the simulator state should be updated"
        conditions.eventually {
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1Dimmer").asNumber() == 66
        }

        when: "a simulated sensor changes its value"
        conditions = new PollingConditions(timeout: 3, initialDelay: 2)
        simulatorProtocol.putState(
                new AttributeRef(managerDemoSetup.thingId, "light1Dimmer"),
                Json.create(77),
                true
        )

        then: "the thing attribute value should be updated"
        conditions.eventually {
            def thing = assetService.get(managerDemoSetup.thingId)
            def attributes = new Attributes(thing.getAttributes())
            assert attributes.get("light1Dimmer").getValue_TODO_BUG_IN_JAVASCRIPT().getType() == JsonType.NUMBER
            assert attributes.get("light1Dimmer").getValueAsInteger() == 77
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
