package org.openremote.test.assets

import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.DatapointInterval
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.SECONDS

class AssetDatapointTest extends Specification implements ManagerContainerTrait {

    def "Receive number sensor values and store asset datapoints"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1, delay: 1)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            def state = simulatorProtocol.getValue(managerDemoSetup.thingId, "light1PowerConsumption")
            assert Values.getNumber(state.orElse(null)).orElse(null) == 12.345d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        def datapoint1ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(13.3d))
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValueAsNumber() }.orElse(null) == 13.3d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        def datapoint2ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(14.4d))
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValueAsNumber() }.orElse(null) == 14.4d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        def datapoint3ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(15.5d))
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValueAsNumber() }.orElse(null) == 15.5d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1PowerConsumption", null) // No value!
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert !thing.getAttribute("light1PowerConsumption").flatMap { it.getValue() }.isPresent()
        }

        expect: "the datapoints to be stored"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1PowerConsumption"))
            assert datapoints.size() > 3

            // Note that the "No value" sensor update should not have created a datapoint, the first
            // datapoint is the last sensor update with an actual value

            assert Values.getNumber(datapoints.get(0).value).orElse(null) == 15.5d
            assert datapoints.get(0).timestamp == datapoint3ExpectedTimestamp

            assert Values.getNumber(datapoints.get(1).value).orElse(null) == 14.4d
            assert datapoints.get(1).timestamp == datapoint2ExpectedTimestamp

            assert Values.getNumber(datapoints.get(2).value).orElse(null) == 13.3d
            assert datapoints.get(2).timestamp == datapoint1ExpectedTimestamp
        }

        and: "the aggregated datapoints should match"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            def aggregatedDatapoints = assetDatapointService.aggregateDatapoints(
                    thing.getAttribute("light1PowerConsumption").orElseThrow({ new RuntimeException("Missing attribute")}),
                    DatapointInterval.HOUR,
                    getClockTimeOf(container)
            )
            assert aggregatedDatapoints.size() == 61
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Receive boolean sensor values and store asset datapoints"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1, delay: 1)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            def state = simulatorProtocol.getValue(managerDemoSetup.thingId, "light1Toggle")
            assert Values.getBoolean(state.orElse(null)).orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        def datapoint1ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1Toggle", Values.create(false))
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert !thing.getAttribute("light1Toggle").flatMap { it.getValueAsBoolean() }.orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        def datapoint2ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1Toggle", Values.create(true))
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert thing.getAttribute("light1Toggle").flatMap { it.getValueAsBoolean() }.orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        def datapoint3ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1Toggle", Values.create(false))
        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert !thing.getAttribute("light1Toggle").flatMap { it.getValueAsBoolean() }.orElse(null)
        }

        expect: "the datapoints to be stored"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1Toggle"))
            assert datapoints.size() == 3

            assert !Values.getBoolean(datapoints.get(0).value).orElse(null)
            assert datapoints.get(0).timestamp == datapoint3ExpectedTimestamp

            assert Values.getBoolean(datapoints.get(1).value).orElse(null)
            assert datapoints.get(1).timestamp == datapoint2ExpectedTimestamp

            assert !Values.getBoolean(datapoints.get(2).value).orElse(null)
            assert datapoints.get(2).timestamp == datapoint1ExpectedTimestamp
        }

        and: "the aggregated datapoints should match"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            def aggregatedDatapoints = assetDatapointService.aggregateDatapoints(
                    thing.getAttribute("light1Toggle").orElseThrow({ new RuntimeException("Missing attribute")}),
                    DatapointInterval.HOUR,
                    getClockTimeOf(container)
            )
            assert aggregatedDatapoints.size() == 61
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
