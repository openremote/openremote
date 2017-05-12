package org.openremote.test.assets

import org.openremote.model.value.Values
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.datapoint.AssetDatapointService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeRef
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class AssetDatapointTest extends Specification implements ManagerContainerTrait {

    def "Receive sensor values and store asset datapoints"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, initialDelay: 1)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoScenesOrRules(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            assert Values.getNumber(simulatorProtocol.getState(managerDemoSetup.thingId, "light1PowerConsumption")).orElse(null) == 12.345d
        }

        when: "a simulated sensor changes its value several times"
        conditions = new PollingConditions(timeout: 3, initialDelay: 1)
        sleep(10)
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(13.3))
        sleep(10)
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(14.4))
        sleep(10)
        simulatorProtocol.putState(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(15.5))

        then: "the thing attribute value should be updated and the datapoints stored"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValueAsNumber() }.orElse(null) == 15.5d
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1PowerConsumption"))
            datapoints.size() > 3
            Values.getNumber(datapoints.get(0).value).orElse(null) == 13.3d
            datapoints.get(0).timestamp < datapoints.get(1).timestamp
            Values.getNumber(datapoints.get(1).value).orElse(null) == 14.4d
            datapoints.get(1).timestamp < datapoints.get(2).timestamp
            Values.getNumber(datapoints.get(2).value).orElse(null) == 15.5d
            datapoints.get(2).timestamp < System.currentTimeMillis()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
