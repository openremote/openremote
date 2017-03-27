package org.openremote.test.agent

import elemental.json.Json
import elemental.json.JsonArray
import elemental.json.JsonType
import org.openremote.agent3.protocol.simulator.SimulatorProtocol
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeEvent
import org.openremote.model.asset.AssetAttributes
import org.openremote.model.units.ColorRGB
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class AgentDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check agent and thing deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, initialDelay: 3)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1Toggle").asBoolean()
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1Dimmer").getType() == JsonType.NULL
            // No initial value!
            assert new ColorRGB(simulatorProtocol.getState(managerDemoSetup.thingId, "light1Color") as JsonArray) == new ColorRGB(88, 123, 88)
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1PowerConsumption").asNumber() == 12.345d
        }

        when: "a client wants to change a thing attributes' value, triggering an actuator"
        conditions = new PollingConditions(timeout: 3, initialDelay: 2)
        def light1DimmerChange = new AttributeEvent(
                managerDemoSetup.thingId, "light1Dimmer", Json.create(66), getClass()
        )
        assetStorageService.updateAttributeValue(light1DimmerChange)

        then: "the simulator state and thing attribute value should be updated (simulator reflects actuator write as sensor read)"
        conditions.eventually {
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1Dimmer").asNumber() == 66
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            def attributes = new AssetAttributes(thing.getAttributes())
            assert attributes.get("light1Dimmer").getValue().getType() == JsonType.NUMBER
            assert attributes.get("light1Dimmer").getValueAsInteger() == 66
        }

        when: "a simulated sensor changes its value"
        conditions = new PollingConditions(timeout: 3, initialDelay: 2)
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1Dimmer", Json.create(77))

        then: "the thing attribute value should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            def attributes = new AssetAttributes(thing.getAttributes())
            assert attributes.get("light1Dimmer").getValue().getType() == JsonType.NUMBER
            assert attributes.get("light1Dimmer").getValueAsInteger() == 77
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
