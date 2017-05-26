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
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.*
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_2
import static org.openremote.model.attribute.AttributeEvent.Source.SENSOR

class ApartmentPresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Detect presence with motion counter"() {

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
        }

        and: "the presence detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().getValue().isPresent()
        assert !livingRoomAsset.getAttribute("lastPresenceDetected").get().getValue().isPresent()

        when: "we have an events consumer for collecting test data"
        List<AssetEvent> insertedAssetEvents = []
        apartment1Engine.assetEventsConsumer = { assetEvent ->
            insertedAssetEvents << assetEvent
        }

        and: "several motion counter increments occur in the room, but not enough for presence detection"
        insertedAssetEvents = []
        setPseudoClocksToRealTime(container, apartment1Engine)
        // The motion counter increments 5 times, 3 minutes apart
        for (i in 1..5) {
            def motionCountIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCount", Values.create(i), getClockTimeOf(apartment1Engine)
            )
            // Use the simulator to inject the update
            simulatorProtocol.putState(motionCountIncrement)
            // Wait until rules engine has the asset event
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() { it.matches(motionCountIncrement, SENSOR, true) }
            }
            advancePseudoClocks(3, MINUTES, container, apartment1Engine)
        }

        then: "presence should not be detected"
        new PollingConditions(initialDelay: 3).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValue().isPresent()
            assert !asset.getAttribute("lastPresenceDetected").get().getValue().isPresent()
        }

        when: "time is advanced enough for event expiration"
        advancePseudoClocks(2, HOURS, container, apartment1Engine)

        then: "the events should have been expired and retracted automatically from the knowledge session"
        conditions.eventually {
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        when: "several motion counter increments occur in the room, fast enough for presence detection"
        insertedAssetEvents = []
        double expectedLastPresenceTimestamp = 0d
        // The motion counter increments 5 times, 1 minute apart
        for (i in 1..5) {
            def motionCountIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCount", Values.create(i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putState(motionCountIncrement)
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() {
                    expectedLastPresenceTimestamp = it.valueTimestamp
                    it.matches(motionCountIncrement, SENSOR, true)
                }
            }
            advancePseudoClocks(1, MINUTES, container, apartment1Engine)
        }

        then: "presence should be detected"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceTimestamp
        }

        when: "time moves on and we keep incrementing the motion counter in short intervals "
        insertedAssetEvents = []
        // The motion counter increments 20 times, 90 seconds apart
        for (i in 1..20) {
            def motionCountIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionCount", Values.create(i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putState(motionCountIncrement)
            new PollingConditions(timeout: 3).eventually {
                assert insertedAssetEvents.any() {
                    expectedLastPresenceTimestamp = it.valueTimestamp
                    it.matches(motionCountIncrement, SENSOR, true)
                }
            }
            advancePseudoClocks(90, SECONDS, container, apartment1Engine)
        }

        then: "presence should still be detected and the timestamp updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceTimestamp
        }

        when: "time moves on without the motion counter incrementing"
        advancePseudoClocks(20, MINUTES, container, apartment1Engine)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().isPresent()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceTimestamp
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

    def "Detect presence with motion sensor"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
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
        // Send 20 triggers each 90 seconds apart
        for (i in 1..20) {
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
            advancePseudoClocks(90, SECONDS, container, apartment2Engine)
        }

        then: "presence should still be detected and the timestamp updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
            asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
        }

        when: "time moves on without the motion sensor being triggered"
        advancePseudoClocks(20, MINUTES, container, apartment2Engine)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
            asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().orElse(null) == expectedLastPresenceTimestamp
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
