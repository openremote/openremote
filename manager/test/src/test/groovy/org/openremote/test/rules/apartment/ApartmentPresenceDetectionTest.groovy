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

    def "Presence detection with motion sensor"() {

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
        }

        and: "the presence detected flag and timestamp of the room should not be set"
        def roomAsset = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
        assert !roomAsset.getAttribute("presenceDetected").get().getValue().isPresent()
        assert !roomAsset.getAttribute("lastPresenceDetected").get().getValue().isPresent()

        when: "motion sensor is triggered"
        double expectedLastPresenceDetected = getClockTimeOf(apartment1Engine)
        def motionSensorTrigger = new AttributeEvent(
                managerDemoSetup.apartment1KitchenId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(motionSensorTrigger)

        then: "presence should be detected"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "time advances"
        advancePseudoClocks(5, MINUTES, container, apartment1Engine)

        and: "motion sensor is triggered"
        expectedLastPresenceDetected = getClockTimeOf(apartment1Engine)
        motionSensorTrigger = new AttributeEvent(
                managerDemoSetup.apartment1KitchenId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(motionSensorTrigger)

        then: "presence should be detected and timestamp updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "time advances"
        advancePseudoClocks(5, MINUTES, container, apartment1Engine)

        and: "motion sensor is not triggered"
        def motionSensorNoTrigger = new AttributeEvent(
                managerDemoSetup.apartment1KitchenId, "motionSensor", Values.create(0)
        )
        simulatorProtocol.putValue(motionSensorNoTrigger)

        then: "no presence should be detected but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Presence detection with motion sensor and confirmation with CO2 level"() {

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

        and: "the presence detected flag and timestamp of the room should not be set"
        def roomAsset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
        assert !roomAsset.getAttribute("presenceDetected").get().getValue().isPresent()
        assert !roomAsset.getAttribute("lastPresenceDetected").get().getValue().isPresent()

        when: "motion sensor is triggered"
        double expectedLastPresenceDetected = getClockTimeOf(apartment1Engine)
        def motionSensorTrigger = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(1)
        )
        simulatorProtocol.putValue(motionSensorTrigger)

        then: "presence should not be detected"
        new PollingConditions(initialDelay: 3, timeout: 5, delay: 1).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValue().isPresent()
            assert !asset.getAttribute("lastPresenceDetected").get().getValue().isPresent()
        }

        when: "time advances"
        advancePseudoClocks(5, MINUTES, container, apartment1Engine)

        and: "the CO2 level increases"
        insertedAssetEvents = []
        // The CO2 level increments 3 times, 5 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(400 + i)
            )
            simulatorProtocol.putValue(co2LevelIncrement)

            // Wait for event and rule consequences to be processed
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
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
        }

        when: "motion sensor is not triggered"
        def motionSensorNoTrigger = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "motionSensor", Values.create(0)
        )
        simulatorProtocol.putValue(motionSensorNoTrigger)

        and: "there is no CO2 level increase for a while"
        advancePseudoClocks(20, MINUTES, container, apartment1Engine)

        then: "presence should be gone but the last timestamp still available"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1LivingroomId, true)
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
            assert asset.getAttribute("lastPresenceDetected").get().getValueAsNumber().get() == expectedLastPresenceDetected
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
