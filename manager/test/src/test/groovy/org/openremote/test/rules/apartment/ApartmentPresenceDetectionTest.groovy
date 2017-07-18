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
import static org.openremote.model.attribute.AttributeEvent.Source.SENSOR

class ApartmentPresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Motion detection with motion sensor"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null
        List<AssetEvent> insertedAssetEvents = []

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Presence Detection with motion sensor",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoApartmentPresenceDetection.drl").text
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
        }

        and: "the motion detected flag and timestamp of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("motionDetected").get().getValue().isPresent()
        assert !livingRoomAsset.getAttribute("lastMotionDetected").get().getValue().isPresent()

        when: "someone enters and leaves the room a few times"
        insertedAssetEvents = []
        // The motion sensor triggers 5 times, 3 minutes apart
        for (i in 1..5) {
            // Use the loop i as value so we can identify and test if we have processed this event
            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(i), getClockTimeOf(apartment1Engine)
            )

            // Use the simulator to inject the update
            simulatorProtocol.putValue(motionSensorTrigger)

            // Wait until rules engine completes work asynchronously, must have processed the event
            new PollingConditions(timeout: 5, delay: 0.5).eventually {
                assert insertedAssetEvents.any() { it.matches(motionSensorTrigger, SENSOR, true) }
                // And wait until we have processed resulting events due to rules firing
                assert noEventProcessedIn(assetProcessingService, 500)
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
        // The motion sensor triggers 5 times, 30 seconds apart
        for (i in 1..5) {

            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putValue(motionSensorTrigger)

            new PollingConditions(timeout: 5, delay: 0.5).eventually {
                assert insertedAssetEvents.any() {
                    expectedLastMotionTimestamp = it.valueTimestamp
                    it.matches(motionSensorTrigger, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
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

        when: "there is no motion sensed for a while"
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

    def "Motion detection with motion sensor and presence confirmation with CO2 level"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null
        List<AssetEvent> insertedAssetEvents = []

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Presence Detection with motion sensor",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoApartmentPresenceDetection.drl").text
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
        }

        and: "the presence detected flag of the room should not be set"
        def livingRoomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !livingRoomAsset.getAttribute("presenceDetected").get().getValue().isPresent()

        when: "someone is moving around in the room"
        insertedAssetEvents = []
        double expectedLastMotionTimestamp = 0d
        // The motion sensor triggers 5 times, 30 seconds apart
        for (i in 1..5) {

            def motionSensorTrigger = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(i), getClockTimeOf(apartment1Engine)
            )
            simulatorProtocol.putValue(motionSensorTrigger)

            new PollingConditions(timeout: 5, delay: 0.5).eventually {
                assert insertedAssetEvents.any() {
                    expectedLastMotionTimestamp = it.valueTimestamp
                    it.matches(motionSensorTrigger, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
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

            new PollingConditions(timeout: 5, delay: 0.5).eventually {
                assert insertedAssetEvents.any() {
                    it.matches(co2LevelIncrement, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
            }

            advancePseudoClocks(5, MINUTES, container, apartment1Engine)
        }

        then: "presence should be detected and the last motion sensor trigger is the last detected timestamp"
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

            new PollingConditions(timeout: 5, delay: 0.5).eventually {
                assert insertedAssetEvents.any() {
                    it.matches(co2LevelIncrement, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
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
}
