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

import java.time.Instant
import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.manager.datapoint.AssetDatapointService.DATA_POINTS_MAX_AGE_DAYS
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.thingLightToggleAttributeName

class AssetDatapointTest extends Specification implements ManagerContainerTrait {

    def "Test number and toggle attribute storage, retrieval and purging"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1, delay: 1)

        when: "the demo agent and thing have been deployed with a DATA_POINTS_MAX_AGE_DAYS value of 1"
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock defaultConfig(serverPort) << [(DATA_POINTS_MAX_AGE_DAYS): "1"], defaultServices()
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
            def aggregatedDatapoints = assetDatapointService.getValueDatapoints(
                    thing.getAttribute("light1PowerConsumption").orElseThrow({ new RuntimeException("Missing attribute")}),
                    DatapointInterval.HOUR,
                    getClockTimeOf(container)
            )
            assert aggregatedDatapoints.size() == 61
        }

        // ------------------------------------
        // Test boolean data point storage
        // ------------------------------------

        when: "a simulated boolean sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        datapoint1ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, thingLightToggleAttributeName, Values.create(false))

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert !thing.getAttribute(thingLightToggleAttributeName).flatMap { it.getValueAsBoolean() }.orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        datapoint2ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, thingLightToggleAttributeName, Values.create(true))

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert thing.getAttribute(thingLightToggleAttributeName).flatMap { it.getValueAsBoolean() }.orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(10, SECONDS, container)
        datapoint3ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, thingLightToggleAttributeName, Values.create(false))

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerDemoSetup.thingId, true)
            assert !thing.getAttribute(thingLightToggleAttributeName).flatMap { it.getValueAsBoolean() }.orElse(null)
        }

        expect: "the datapoints to be stored"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, thingLightToggleAttributeName))
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
            def aggregatedDatapoints = assetDatapointService.getValueDatapoints(
                    thing.getAttribute(thingLightToggleAttributeName).orElseThrow({ new RuntimeException("Missing attribute")}),
                    DatapointInterval.HOUR,
                    getClockTimeOf(container)
            )
            assert aggregatedDatapoints.size() == 61
        }

        // ------------------------------------
        // Test purging of data points
        // ------------------------------------

        when: "time moves forward by more than a day"
        advancePseudoClock(26, HOURS, container)

        and: "the power sensor with default max age receives a new value"
        def datapoint4ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.putValue(managerDemoSetup.thingId, "light1PowerConsumption", Values.create(17.5d))

        and: "the toggle sensor with a custom max age of 7 days receives a new value"
        simulatorProtocol.putValue(managerDemoSetup.thingId, thingLightToggleAttributeName, Values.create(true))

        then: "the datapoints should be stored"
        conditions.eventually {
            def powerDatapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1PowerConsumption"))
            def toggleDatapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, thingLightToggleAttributeName))

            assert powerDatapoints.size() > 4
            assert Values.getNumber(powerDatapoints.get(0).value).orElse(null) == 17.5d
            assert powerDatapoints.get(0).timestamp == datapoint4ExpectedTimestamp

            assert toggleDatapoints.size() == 4
            assert Values.getBoolean(toggleDatapoints.get(0).value).orElse(false)
            assert toggleDatapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        when: "the daily data point purge routine executes the next day"
        // Move clock to next days purge runtime
        advancePseudoClock(assetDatapointService.getFirstRunMillis(Instant.ofEpochMilli(getClockTimeOf(container))), TimeUnit.MILLISECONDS, container)
        assetDatapointService.purgeDataPoints()

        then: "data points older than 1 day should be purged for the power sensor"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1PowerConsumption"))
            assert datapoints.size() == 1
            assert Values.getNumber(datapoints.get(0).value).orElse(null) == 17.5d
            assert datapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        and: "no data points should have been purged for the toggle sensor"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() == 4
            assert Values.getBoolean(datapoints.get(0).value).orElse(false)
            assert datapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        when: "the daily data point purge routine executes 3 days later"
        advancePseudoClock(3, TimeUnit.DAYS, container)
        assetDatapointService.purgeDataPoints()

        then: "all data points should be purged for power sensor"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, "light1PowerConsumption"))
            assert datapoints.isEmpty()
        }

        and: "no data points should have been purged for the toggle sensor"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() == 4
            assert Values.getBoolean(datapoints.get(0).value).orElse(false)
            assert datapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        when: "the daily data point purge routine executes 3 days later"
        advancePseudoClock(3, TimeUnit.DAYS, container)
        assetDatapointService.purgeDataPoints()

        then: "data points older than 7 days should have been purged for the toggle sensor"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() == 1
            assert Values.getBoolean(datapoints.get(0).value).orElse(false)
            assert datapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        when: "the daily data point purge routine executes 1 day later"
        advancePseudoClock(1, TimeUnit.DAYS, container)
        assetDatapointService.purgeDataPoints()

        then: "all data points should have been purged for the toggle sensor"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerDemoSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.isEmpty()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
