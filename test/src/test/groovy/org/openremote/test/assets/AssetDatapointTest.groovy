package org.openremote.test.assets

import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.AssetPredictedDatapointResource
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.datapoint.query.AssetDatapointIntervalQuery
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.*
import static org.openremote.manager.datapoint.AssetDatapointService.OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.setup.integration.ManagerTestSetup.thingLightToggleAttributeName
import static spock.util.matcher.HamcrestMatchers.closeTo

class AssetDatapointTest extends Specification implements ManagerContainerTrait {

    def "Test number and toggle attribute storage, retrieval and purging"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the demo agent and thing have been deployed"
        def datapointPurgeDays = OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def agentService = container.getService(AgentService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)

        and: "the clock is stopped for testing purposes and advanced to the next hour"
        stopPseudoClock()
        advancePseudoClock(Instant.ofEpochMilli(getClockTimeOf(container)).truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS).toEpochMilli() - getClockTimeOf(container), TimeUnit.MILLISECONDS, container)

        and: "a resource client is created"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token
        // Resteasy client has issues with @Suspended annotation so not used for now
        //def datapointResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetDatapointResource.class)
        def predictedDatapointResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetPredictedDatapointResource.class)

        then: "the simulator protocol instance should have been initialised and attributes linked"
        conditions.eventually {
            assert agentService.protocolInstanceMap.get(managerTestSetup.agentId) != null
            assert ((SimulatorProtocol) agentService.protocolInstanceMap.get(managerTestSetup.agentId)).linkedAttributes.size() == 4
            assert ((SimulatorProtocol) agentService.protocolInstanceMap.get(managerTestSetup.agentId)).linkedAttributes.get(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption")).getValue(Double.class).orElse(0d) == 12.345d
        }

        when: "an attribute linked to the simulator agent receives some values"
        def simulatorProtocol = ((SimulatorProtocol) agentService.protocolInstanceMap.get(managerTestSetup.agentId))
        advancePseudoClock(60, SECONDS, container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), 13.3d)
        advancePseudoClock(60, SECONDS, container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), null)
        advancePseudoClock(60, SECONDS, container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), 13.3d)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValue(Double.class) }.orElse(null) == 13.3d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(60, SECONDS, container)
        def datapoint1ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), 13.5d)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValue(Double.class) }.orElse(null) == 13.5d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(60, SECONDS, container)
        def datapoint2ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), 14.4d)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValue(Double.class) }.orElse(null) == 14.4d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(60, SECONDS, container)
        def datapoint3ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), 15.5d)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert thing.getAttribute("light1PowerConsumption").flatMap { it.getValue(Double.class) }.orElse(null) == 15.5d
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(60, SECONDS, container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), null)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert !thing.getAttribute("light1PowerConsumption").flatMap { it.getValue() }.isPresent()
        }

        expect: "the datapoints to be stored"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, "light1PowerConsumption", null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"))
            // Can include initial value when container is re-used between tests
            assert datapoints.size() >= 5

            // Note that the "No value" sensor update should not have created a datapoint, the first
            // datapoint is the last sensor update with an actual value

            assert datapoints.any{ it.value == 15.5d && it.timestamp == datapoint3ExpectedTimestamp}
            assert datapoints.any{ it.value == 14.4d && it.timestamp == datapoint2ExpectedTimestamp}
            assert datapoints.any{ it.value == 13.5d && it.timestamp == datapoint1ExpectedTimestamp}
            assert datapoints.count{ it.value == 13.3d} == 2
        }

        and: "the aggregated datapoints should match"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            //def aggregatedDatapoints = datapointResource.getDatapoints(
            def aggregatedDatapoints = assetDatapointService.queryDatapoints(
                    thing.getId(),
                    "light1PowerConsumption",
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(getClockTimeOf(container)), ZoneId.systemDefault()).minus(1, ChronoUnit.HOURS),
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(getClockTimeOf(container)), ZoneId.systemDefault()),
                            "minute",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
            )
            assert aggregatedDatapoints.size() == 61
            assert aggregatedDatapoints[54].value == 13.3
            assert aggregatedDatapoints[55].value == null
            assert aggregatedDatapoints[56].value == 13.3
            assert aggregatedDatapoints[57].value == 13.5
            assert aggregatedDatapoints[58].value == 14.4
            assert aggregatedDatapoints[59].value == 15.5
            assert aggregatedDatapoints[60].value == null
        }

        and: "when the step size is set on the datapoint retrieval then the datapoints should match"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            //def aggregatedDatapoints = datapointResource.getDatapoints(
            def aggregatedDatapoints = assetDatapointService.queryDatapoints(
                    thing.getId(),
                    "light1PowerConsumption",
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(getClockTimeOf(container)), ZoneId.systemDefault()).minus(1, ChronoUnit.HOURS),
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(getClockTimeOf(container)), ZoneId.systemDefault()),
                            "5 minutes",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
            )
            assert aggregatedDatapoints.size() == 13
            assert aggregatedDatapoints[11].value, closeTo(13.36666, 0.0001)
            assert aggregatedDatapoints[12].value == 14.95
        }


        // ------------------------------------
        // Test boolean data point storage
        // ------------------------------------

        when: "a simulated boolean sensor receives a new value"
        advancePseudoClock(1, MINUTES, container)
        datapoint1ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName), false)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert !thing.getAttribute(thingLightToggleAttributeName).flatMap { it.getValue(Boolean.class) }.orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(1, MINUTES, container)
        datapoint2ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName), true)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert thing.getAttribute(thingLightToggleAttributeName).flatMap { it.getValue(Boolean.class) }.orElse(null)
        }

        when: "a simulated sensor receives a new value"
        advancePseudoClock(1, MINUTES, container)
        datapoint3ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName), false)

        then: "the attribute should be updated"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            assert !thing.getAttribute(thingLightToggleAttributeName).flatMap { it.getValue(Boolean.class) }.orElse(null)
        }

        expect: "the datapoints to be stored"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName, null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() >= 3

            assert !ValueUtil.getBoolean(datapoints.get(0).value).orElse(null)
            assert datapoints.get(0).timestamp == datapoint3ExpectedTimestamp

            assert datapoints.any {it.timestamp == datapoint3ExpectedTimestamp && !(it.value as Boolean)}
            assert datapoints.any {it.timestamp == datapoint2ExpectedTimestamp && (it.value as Boolean)}
            assert datapoints.any {it.timestamp == datapoint3ExpectedTimestamp && !(it.value as Boolean)}
        }

        and: "the aggregated datapoints should match"
        conditions.eventually {
            def thing = assetStorageService.find(managerTestSetup.thingId, true)
            //def aggregatedDatapoints = datapointResource.getDatapoints(
            def aggregatedDatapoints = assetDatapointService.queryDatapoints(
                    thing.getId(),
                    thingLightToggleAttributeName,
                    new AssetDatapointIntervalQuery(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(getClockTimeOf(container)), ZoneId.systemDefault()).minus(1, ChronoUnit.HOURS),
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(getClockTimeOf(container)), ZoneId.systemDefault()),
                            "MINUTE",
                            AssetDatapointIntervalQuery.Formula.AVG,
                            true
                    )
            )
            assert aggregatedDatapoints.size() == 61
            assert aggregatedDatapoints[58].value == 0
            assert aggregatedDatapoints[59].value == 1d
            assert aggregatedDatapoints[60].value == 0
        }

        // ------------------------------------
        // Test purging of data points
        // ------------------------------------

        when: "time moves forward by more than purge days"
        advancePseudoClock(datapointPurgeDays + 1, DAYS, container)

        and: "the power sensor with default max age receives a new value"
        def datapoint4ExpectedTimestamp = getClockTimeOf(container)
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"), 17.5d)

        and: "the toggle sensor with a custom max age of 7 days receives a new value"
        simulatorProtocol.updateSensor(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName), true)

        then: "the datapoints should be stored"
        conditions.eventually {
            //def powerDatapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, "light1PowerConsumption", null) as List
            //def toggleDatapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName, null) as List
            def powerDatapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"))
            def toggleDatapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))

            assert powerDatapoints.size() >= 6
            assert powerDatapoints.any {it.timestamp == datapoint4ExpectedTimestamp && it.value == 17.5 }

            assert toggleDatapoints.size() >= 4
            assert toggleDatapoints.any {it.timestamp == datapoint4ExpectedTimestamp && it.value}
        }

        when: "the clock advances to the next days purge routine execution time"
        advancePseudoClock(assetDatapointService.getFirstPurgeMillis(Instant.ofEpochMilli(getClockTimeOf(container))), TimeUnit.MILLISECONDS, container)

        and: "the purge routine runs"
        assetDatapointService.purgeDataPoints()

        then: "data points older than purge days should be purged for the power sensor"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, "light1PowerConsumption", null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"))
            assert datapoints.size() == 1
            assert ValueUtil.getValue(datapoints.get(0).value, Double.class).orElse(null) == 17.5d
            assert datapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        and: "no data points should have been purged for the toggle sensor"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName, null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() >= 4
            assert ValueUtil.getBoolean(datapoints.get(0).value).orElse(false)
            assert datapoints.any {it.timestamp == datapoint4ExpectedTimestamp && it.value}
        }

        when: "the clock advances 3 times the purge duration"
        advancePseudoClock(3 * datapointPurgeDays, DAYS, container)

        and: "the purge routine runs"
        assetDatapointService.purgeDataPoints()

        then: "all data points should be purged for power sensor"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, "light1PowerConsumption", null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"))
            assert datapoints.isEmpty()
        }

        and: "no data points should have been purged for the toggle sensor"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName, null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() >= 4
            assert datapoints.any {it.timestamp == datapoint4ExpectedTimestamp && it.value}
        }

        when: "the clock advances 3 times the purge duration"
        advancePseudoClock(3 * datapointPurgeDays, DAYS, container)

        and: "the purge routine runs"
        assetDatapointService.purgeDataPoints()

        then: "data points older than 7 days should have been purged for the toggle sensor"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName, null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.size() == 1
            assert ValueUtil.getBoolean(datapoints.get(0).value).orElse(false)
            assert datapoints.get(0).timestamp == datapoint4ExpectedTimestamp
        }

        when: "the clock advances by the purge duration"
        advancePseudoClock(datapointPurgeDays, DAYS, container)

        and: "the purge routine runs"
        assetDatapointService.purgeDataPoints()

        then: "all data points should have been purged for the toggle sensor"
        conditions.eventually {
            //def datapoints = datapointResource.getDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName, null) as List
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))
            assert datapoints.isEmpty()
        }

        when: "predicted data points are added"
        predictedDatapointResource.writePredictedDatapoints(null, managerTestSetup.thingId, "light1PowerConsumption",
            [
                new ValueDatapoint<>(getClockTimeOf(container)+60000, 10d),
                new ValueDatapoint<>(getClockTimeOf(container)+120000, 20d),
                new ValueDatapoint<>(getClockTimeOf(container)+180000, 30d),
                new ValueDatapoint<>(getClockTimeOf(container)+240000, 40d),
                new ValueDatapoint<>(getClockTimeOf(container)+300000, 50d)
            ] as ValueDatapoint<?>[]
        )
        predictedDatapointResource.writePredictedDatapoints(null, managerTestSetup.thingId, thingLightToggleAttributeName,
            [
                new ValueDatapoint<>(getClockTimeOf(container)+60000, true),
                new ValueDatapoint<>(getClockTimeOf(container)+120000, true),
                new ValueDatapoint<>(getClockTimeOf(container)+180000, true),
                new ValueDatapoint<>(getClockTimeOf(container)+240000, false),
                new ValueDatapoint<>(getClockTimeOf(container)+300000, false)
            ] as ValueDatapoint<?>[]
        )

        then: "the predicted data should be available"
        def predictedData = assetPredictedDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"))
        assert predictedData.size() == 5
        assert predictedData.any {it.value == 20d}

        when: "the predicted data is purged and then retrieved"
        assetPredictedDatapointService.purgeValues(managerTestSetup.thingId, "light1PowerConsumption")
        predictedData = assetPredictedDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, "light1PowerConsumption"))

        then: "no predicted data should remain for this attribute"
        assert predictedData.isEmpty()

        when: "other predicted data is retrieved"
        predictedData = assetPredictedDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, thingLightToggleAttributeName))

        then: "predicted data should remain for this attribute"
        assert predictedData.size() == 5
        assert predictedData.count {it.value == false} == 2
    }
}
