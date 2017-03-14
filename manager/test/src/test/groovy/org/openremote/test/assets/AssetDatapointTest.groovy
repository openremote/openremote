package org.openremote.test.assets

import elemental.json.Json
import org.openremote.agent3.protocol.simulator.SimulatorProtocol
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.datapoint.AssetDatapointService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.AttributeRef
import org.openremote.model.Attributes
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class AssetDatapointTest extends Specification implements ManagerContainerTrait {

    def "Receive sensor values and store asset datapoints"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, initialDelay: 3)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            assert simulatorProtocol.getState(managerDemoSetup.thingId, "light1PowerConsumption").asNumber() == 12.345d
        }

        when: "a simulated sensor changes its value several times"
        conditions = new PollingConditions(timeout: 3, initialDelay: 2)
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1PowerConsumption", Json.create(13.3))
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1PowerConsumption", Json.create(14.4))
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1PowerConsumption", Json.create(15.5))

        then: "the thing attribute value should be updated and the datapoints stored"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId)
            def attributes = new Attributes(thing.getAttributes())
            assert attributes.get("light1PowerConsumption").getValueAsDecimal() == 15.5d
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1PowerConsumption"))
            datapoints.size() == 3
            datapoints.get(0).value.asNumber() == 13.3d
            datapoints.get(0).timestamp < datapoints.get(1).timestamp
            datapoints.get(1).value.asNumber() == 14.4d
            datapoints.get(1).timestamp < datapoints.get(2).timestamp
            datapoints.get(2).value.asNumber() == 15.5d
            datapoints.get(2).timestamp < System.currentTimeMillis()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
