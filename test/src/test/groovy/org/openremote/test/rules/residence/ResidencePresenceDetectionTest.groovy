package org.openremote.test.rules.residence

import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.MINUTES
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.model.attribute.AttributeEvent.Source.SENSOR

class ResidencePresenceDetectionTest extends Specification implements ManagerContainerTrait {

    def "Presence detection with motion sensor"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
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
                "Demo Apartment - Presence Detection with motion and CO2 sensors", Ruleset.Lang.GROOVY, getClass().getResource("/demo/rules/DemoResidencePresenceDetection.groovy").text,
                managerDemoSetup.apartment1Id, false
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the presence detected flag and timestamp should not be set"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert !apartment.getAttribute("presenceDetected").get().getValue().isPresent()
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("presenceDetected").get().getValue().isPresent()
            assert !kitchen.getAttribute("lastPresenceDetected").get().getValue().isPresent()
            def hallway = assetStorageService.find(managerDemoSetup.apartment1HallwayId, true)
            assert !hallway.getAttribute("presenceDetected").get().getValue().isPresent()
            assert !hallway.getAttribute("lastPresenceDetected").get().getValue().isPresent()
        }

        when: "motion sensor is triggered in all rooms"
        double expectedLastPresenceDetected = getClockTimeOf(container)
        def kitchenMotionSensorEvent = new AttributeEvent(
                managerDemoSetup.apartment1KitchenId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(kitchenMotionSensorEvent)
        def hallwayMotionSensorEvent = new AttributeEvent(
                managerDemoSetup.apartment1HallwayId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(hallwayMotionSensorEvent)

        then: "presence should be detected in all rooms"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert kitchen.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
            def hallway = assetStorageService.find(managerDemoSetup.apartment1HallwayId, true)
            assert hallway.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert hallway.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "time advances"
        advancePseudoClock(5, MINUTES, container)

        and: "motion sensor is triggered a room again"
        double expectedLastPresenceDetected2 = getClockTimeOf(container)
        kitchenMotionSensorEvent = new AttributeEvent(
                managerDemoSetup.apartment1KitchenId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(kitchenMotionSensorEvent)

        then: "presence should be detected in all rooms and timestamp should be updated of one room"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert kitchen.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected2
            def hallway = assetStorageService.find(managerDemoSetup.apartment1HallwayId, true)
            assert hallway.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert hallway.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "time advances"
        advancePseudoClock(5, MINUTES, container)

        and: "motion sensor is not triggered in a room"
        kitchenMotionSensorEvent = new AttributeEvent(
                managerDemoSetup.apartment1KitchenId, "motionSensor", Values.create(0)
        )
        simulatorProtocol.putValue(kitchenMotionSensorEvent)

        then: "some presence should still be detected and timestamps are still available"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert kitchen.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected2
            def hallway = assetStorageService.find(managerDemoSetup.apartment1HallwayId, true)
            assert hallway.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert hallway.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "time advances"
        advancePseudoClock(5, MINUTES, container)

        and: "motion sensor is not triggered in another room"
        hallwayMotionSensorEvent = new AttributeEvent(
                managerDemoSetup.apartment1HallwayId, "motionSensor", Values.create(0)
        )
        simulatorProtocol.putValue(hallwayMotionSensorEvent)

        then: "no presence should be detected but the last timestamp still available"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert !apartment.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert kitchen.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected2
            def hallway = assetStorageService.find(managerDemoSetup.apartment1HallwayId, true)
            assert !hallway.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert hallway.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Presence detection with motion sensor and confirmation with CO2 level"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Presence Detection with motion sensor", Ruleset.Lang.GROOVY, getClass().getResource("/demo/rules/DemoResidencePresenceDetection.groovy").text,
                managerDemoSetup.apartment1Id, false
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the presence detected flag and timestamp of the room should not be set"
        conditions.eventually {
            def roomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !roomAsset.getAttribute("presenceDetected").get().getValue().isPresent()
            assert !roomAsset.getAttribute("lastPresenceDetected").get().getValue().isPresent()
        }

        when: "motion sensor is triggered"
        double expectedLastPresenceDetected = getClockTimeOf(container)
        def motionSensorTrigger = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(motionSensorTrigger)

        /* See rules, this leads to many false negatives when windows are open in the room
        then: "presence should not be detected"
        new PollingConditions(initialDelay: 3, timeout: 5, delay: 1).eventually {
            def roomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !roomAsset.getAttribute("presenceDetected").get().getValue().isPresent()
            assert !roomAsset.getAttribute("lastPresenceDetected").get().getValue().isPresent()
        }

        when: "time advances"
        advancePseudoClock(5, MINUTES, container)

        and: "the CO2 level increases"
        // The CO2 level increments 3 times, 5 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(400 + i)
            )
            simulatorProtocol.putValue(co2LevelIncrement)

            // Wait for event to be processed
            conditions.eventually {
                assert apartment1Engine.assetEvents.any() {
                    it.fact.matches(co2LevelIncrement, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
            }

            advancePseudoClock(5, MINUTES, container)
        }
        */

        then: "presence should be detected and the last motion sensor trigger is the last detected timestamp"
        conditions.eventually {
            def roomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert roomAsset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert roomAsset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "motion sensor is not triggered (someone might be resting in the room)"
        def motionSensorNoTrigger = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(0)
        )
        simulatorProtocol.putValue(motionSensorNoTrigger)

        and: "the CO2 level increases (someone breathing in the room)"
        // The CO2 level increments 3 times, 5 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(400 + i)
            )
            simulatorProtocol.putValue(co2LevelIncrement)

            // Wait for event to be processed
            conditions.eventually {
                assert apartment1Engine.assetEvents.any() {
                    it.fact.matches(co2LevelIncrement, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
            }

            advancePseudoClock(5, MINUTES, container)
        }

        then: "presence should be detected and the last motion sensor trigger is the last detected timestamp"
        conditions.eventually {
            def roomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert roomAsset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert roomAsset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "there is no CO2 level increase for a while (nobody resting in the room)"
        advancePseudoClock(20, MINUTES, container)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def roomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !roomAsset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert roomAsset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

        /* TODO Migrate to JS, didn't compile even with Drools
        def "Presence prediction rules compilation"() {

            given: "the container environment is started"
            def conditions = new PollingConditions(timeout: 100, delay: 1)
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
                    "Demo Apartment - Presence Detection with motion sensor",
                    managerDemoSetup.apartment1Id,
                    getClass().getResource("/demo/rules/LegacyDemoApartmentPresencePrediction.drl").text
            )
            rulesetStorageService.merge(ruleset)

            expect: "the rule engines to become available and be running"
            conditions.eventually {
                apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
                assert apartment1Engine != null
                assert apartment1Engine.isRunning()
    //            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
            }

            cleanup: "the server should be stopped"
            stopContainer(container)
        }
        */

}
