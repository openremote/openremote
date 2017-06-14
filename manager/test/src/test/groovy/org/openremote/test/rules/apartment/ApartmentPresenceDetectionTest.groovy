package org.openremote.test.rules.apartment

import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetEvent
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.*
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_2
import static org.openremote.model.attribute.AttributeEvent.Source.SENSOR

class ApartmentPresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Motion detection with motion counter"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null
        List<AssetEvent> insertedAssetEvents = []

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Presence Detection with motion counter",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoApartmentPresenceDetectionCounter.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
            apartment1Engine.assetEventsConsumer = { assetEvent ->
                insertedAssetEvents << assetEvent
            }
            setPseudoClocksToRealTime(container, apartment1Engine)
        }

        and: "the motion detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("motionDetected").get().getValue().isPresent()
        assert !livingRoomAsset.getAttribute("lastMotionDetected").get().getValue().isPresent()

        when: "someone enters and leaves the room a few times"
        insertedAssetEvents = []
        // The motion counter increments 5 times, 3 minutes apart
        for (i in 1..5) {
            def motionCountIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCount", Values.create(i), getClockTimeOf(apartment1Engine)
            )
            // Use the simulator to inject the update
            simulatorProtocol.putValue(motionCountIncrement)
            // Wait until rules engine has the asset event
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() { it.matches(motionCountIncrement, SENSOR, true) }
            }
            advancePseudoClocks(3, MINUTES, container, apartment1Engine)
        }

        then: "motion should not be detected"
        new PollingConditions(initialDelay: 3).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("motionDetected").get().getValue().isPresent()
            assert !asset.getAttribute("lastMotionDetected").get().getValue().isPresent()
            assert !asset.getAttribute("presenceDetected").get().getValue().isPresent()
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment1Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        when: "someone is moving around in the room"
        insertedAssetEvents = []
        double expectedLastMotionTimestamp = 0d
        // The motion counter increments 5 times, 30 seconds apart
        for (i in 1..5) {

            // Increment the motion counter
            def motionCountIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCount", Values.create(100+i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putValue(motionCountIncrement)
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() {
                    expectedLastMotionTimestamp = it.valueTimestamp
                    it.matches(motionCountIncrement, SENSOR, true)
                }
            }

            advancePseudoClocks(30, SECONDS, container, apartment1Engine)
        }

        then: "motion should be detected and the last motion counter increment is the last motion timestamp"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("motionDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().get() == expectedLastMotionTimestamp
            assert !asset.getAttribute("presenceDetected").get().getValue().isPresent()
        }

        when: "there is no motion detected for a while"
        advancePseudoClocks(20, MINUTES, container, apartment1Engine)

        then: "no motion should be detected but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("motionDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().get() == expectedLastMotionTimestamp
            assert !asset.getAttribute("presenceDetected").get().getValue().isPresent()
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment1Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Motion detection with motion counter and presence confirmation with CO2 level"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null
        List<AssetEvent> insertedAssetEvents = []

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Presence Detection with motion counter",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoApartmentPresenceDetectionCounter.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
            apartment1Engine.assetEventsConsumer = { assetEvent ->
                insertedAssetEvents << assetEvent
            }
            setPseudoClocksToRealTime(container, apartment1Engine)
        }

        and: "the presence detected flag of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().getValue().isPresent()

        when: "someone is moving around in the room"
        insertedAssetEvents = []
        double expectedLastMotionTimestamp = 0d
        // The motion counter increments 5 times, 30 seconds apart
        for (i in 1..5) {

            // Increment the motion counter
            def motionCountIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCount", Values.create(100+i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putValue(motionCountIncrement)
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() {
                    expectedLastMotionTimestamp = it.valueTimestamp
                    it.matches(motionCountIncrement, SENSOR, true)
                }
            }

            advancePseudoClocks(30, SECONDS, container, apartment1Engine)
        }

        and: "someone is not moving but still present in the room"
        // The CO2 level increments 3 times, 5 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(400 + i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putValue(co2LevelIncrement)
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() {
                    it.matches(co2LevelIncrement, SENSOR, true)
                }
            }

            advancePseudoClocks(5, MINUTES, container, apartment1Engine)
        }

        then: "presence should be detected and the last motion counter increment is the last detected timestamp"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("motionDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().get() == expectedLastMotionTimestamp
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
        }

        when: "someone stays in the room but doesn't move"
        insertedAssetEvents = []
        // The CO2 level increments 5 times, 5 minutes apart
        for (i in 1..5) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(500 + i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putValue(co2LevelIncrement)
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() {
                    it.matches(co2LevelIncrement, SENSOR, true)
                }
            }

            advancePseudoClocks(5, MINUTES, container, apartment1Engine)
        }

        then: "presence should still be detected but no motion"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("motionDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().get() == expectedLastMotionTimestamp
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
        }

        when: "there is no CO2 level increase for a while"
        advancePseudoClocks(20, MINUTES, container, apartment1Engine)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("motionDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastMotionDetected").get().getValueAsNumber().get() == expectedLastMotionTimestamp
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment1Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    // TODO This should be a very simple test that only shows how to accumulate and count events in rules, maybe move to new test clas
    @Ignore
    def "Detect presence with motion sensor"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 60, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment2Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Presence Detection with motion PIR",
                managerDemoSetup.apartment2Id,
                getClass().getResource("/demo/rules/DemoApartmentPresenceDetectionPIR.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
            assert apartment2Engine != null
            assert apartment2Engine.isRunning()
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        and: "the presence detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
        assert !livingRoomAsset.getAttribute("lastPresenceDetected").get().getValueAsNumber().isPresent()
        assert !livingRoomAsset.getAttribute("firstPresenceDetected").get().getValueAsNumber().isPresent()
        def bathRoomAsset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
        assert !bathRoomAsset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
        assert !bathRoomAsset.getAttribute("lastPresenceDetected").get().getValueAsNumber().isPresent()
        // TODO: Fix this check - value actually contains -1.0 (as inserted by the Init firstPresentDetected rule)
        assert !bathRoomAsset.getAttribute("firstPresenceDetected").get().getValueAsNumber().isPresent()

        when: "we have an events consumer for collecting test data"
        List<AssetEvent> insertedAssetEvents = []
        apartment2Engine.assetEventsConsumer = { assetEvent ->
            insertedAssetEvents << assetEvent
        }

        and: "several motion sensor events are triggered in the room, but not enough for presence detection"
        insertedAssetEvents = []
        setPseudoClocksToRealTime(container, apartment2Engine)
        def firstPresenceDetectedTimeStamp = 0;
        // Send 5 triggers each 3 minutes apart
        for (i in 1..5) {
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment2BathroomId, "motionSensor", Values.create(true), getClockTimeOf(apartment2Engine)
            )
            assetProcessingService.sendAttributeEvent(motionSensorTrigger)
            if (i == 1)
                firstPresenceDetectedTimeStamp = motionSensorTrigger.timestamp
            new PollingConditions(timeout: 25, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTrigger) }
            }
            advancePseudoClocks(3, MINUTES, container, apartment2Engine)
        }

        then: "presence should not be detected"
        new PollingConditions(initialDelay: 3).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert !asset.getAttribute("lastPresenceDetected").get().getValue().isPresent()
            // First presence should be fetch
            // TODO this test sometimes fails as the rule "Fetch the first presence detected time stamp" is triggered twice, which is wrong as it should trigger only once
            // assert asset.getAttribute("firstPresenceDetected").get().getValueAsNumber().orElse(null) == firstPresenceDetectedTimeStamp
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment2Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
            assert !bathRoomAsset.getAttribute("firstPresenceDetected").get().getValueAsNumber().isPresent()
        }

        when: "several motion sensor events are triggered in the room, fast enough for presence detection"
        insertedAssetEvents = []
        double expectedLastPresenceTimestampBathroom = 0
        def expectedLastPresenceTimestampLivingroom = 0
        // Send 5 triggers each 1 minute apart
        for (i in 1..5) {
            def motionSensorTriggerLivingroom = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionSensor", Values.create(true), getClockTimeOf(apartment2Engine)
            )
            def motionSensorTriggerBathroom = new AttributeEvent(
                    managerDemoSetup.apartment2BathroomId, "motionSensor", Values.create(true), getClockTimeOf(apartment2Engine)
            )
            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "co2Level", Values.create(400 + i), getClockTimeOf(apartment2Engine)
            )
            expectedLastPresenceTimestampBathroom = motionSensorTriggerBathroom.timestamp
            assetProcessingService.sendAttributeEvent(motionSensorTriggerBathroom)
            if (i == 1) {
                // Trigger motion sensor
                assetProcessingService.sendAttributeEvent(motionSensorTriggerLivingroom)
                assetProcessingService.sendAttributeEvent(co2LevelIncrement)
                expectedLastPresenceTimestampLivingroom = motionSensorTriggerLivingroom.timestamp
            } else {
                // Increase CO2 because person is not moving but breathing
                assetProcessingService.sendAttributeEvent(co2LevelIncrement)
            }
            new PollingConditions(timeout: 25, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTriggerBathroom) }
                if (i > 1) {
                    // TODO why the following test fails?
                    // assert insertedAssetEvents.any() { it.matches(co2LevelIncrement) }
                } else
                    assert insertedAssetEvents.any() { it.matches(motionSensorTriggerLivingroom) }
            }
            advancePseudoClocks(1, MINUTES, container, apartment2Engine)
        }

        then: "presence should be detected"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestampBathroom
            asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestampLivingroom
        }

        when: "time moves on and we keep triggering the motion sensor in short intervals "
        def expectedLastPresenceTimestamp
        insertedAssetEvents = []
        // Send 20 triggers each 10 seconds apart
        for (i in 1..20) {
            advancePseudoClocks(10, SECONDS, container, apartment2Engine)
            def motionSensorTriggerLivingroom = new AttributeEvent(
                    managerDemoSetup.apartment2LivingroomId, "motionSensor", Values.create(true), getClockTimeOf(apartment2Engine)
            )
            def motionSensorTriggerBathroom = new AttributeEvent(
                    managerDemoSetup.apartment2BathroomId, "motionSensor", Values.create(true), getClockTimeOf(apartment2Engine)
            )
            expectedLastPresenceTimestamp = motionSensorTriggerBathroom.timestamp
            assetProcessingService.sendAttributeEvent(motionSensorTriggerBathroom)
            assetProcessingService.sendAttributeEvent(motionSensorTriggerLivingroom)
            new PollingConditions(timeout: 25, initialDelay: 0.2, delay: 0.2).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTriggerBathroom) }
                assert insertedAssetEvents.any() { it.matches(motionSensorTriggerLivingroom) }
            }
        }
        def co2LevelIncrement = new AttributeEvent(
                managerDemoSetup.apartment2LivingroomId, "co2Level", Values.create(500), getClockTimeOf(apartment2Engine)
        )
        assetProcessingService.sendAttributeEvent(co2LevelIncrement)

        then: "presence should still be detected and the timestamp updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            // TODO it sometimes fails with the following:
//            asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
//            |     |                                    |     |                  |            |  |
//            |     |                                    |     |                  |            |  1495967135894
//            |     |                                    |     |                  |            false
//            |     |                                    |     |                  1.495967045894E12
//            |     |                                    |     Optional[1.495967045894E12]
//            |     |                                    AssetAttribute{assetId='6DWae4YmRn64I1eyYSi2qA', name='lastPresenceDetected'} {"meta":[{"name":"urn:openremote:asset:meta:label","value":"Last Presence Timestamp"},{"name":"urn:openremote:asset:meta:description","value":"Timestamp of last detected presence"},{"name":"urn:openremote:asset:meta:ruleState","value":true}],"type":"TIMESTAMP_MILLIS","value":1.495967045894E12,"valueTimestamp":1.495967135871E12}
//            |     Optional[AssetAttribute{assetId='6DWae4YmRn64I1eyYSi2qA', name='lastPresenceDetected'} {"meta":[{"name":"urn:openremote:asset:meta:label","value":"Last Presence Timestamp"},{"name":"urn:openremote:asset:meta:description","value":"Timestamp of last detected presence"},{"name":"urn:openremote:asset:meta:ruleState","value":true}],"type":"TIMESTAMP_MILLIS","value":1.495967045894E12,"valueTimestamp":1.495967135871E12}]
//            ServerAsset{id='6DWae4YmRn64I1eyYSi2qA', name='Living Room', type ='urn:openremote:asset:room'}

//            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
            asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
//            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
        }

        when: "time moves on without the motion sensor being triggered"
        advancePseudoClocks(9, MINUTES, container, apartment2Engine)

        then: "presence should be gone but the last timestamp still available"
        // First in the bathroom after exactly 10m since last movement
        conditions.eventually {
            advancePseudoClocks(1, MINUTES, container, apartment2Engine)
            def asset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
//            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
            // TODO check if this is exactly after 10m
        }
        advancePseudoClocks(9, MINUTES, container, apartment2Engine)

        and: "In the living room after 20m since CO2 increment"
        conditions.eventually {
            advancePseudoClocks(1, MINUTES, container, apartment2Engine)
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
//            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
            // TODO check if this is exactly after 20m
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment2Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment2Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_2
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}
