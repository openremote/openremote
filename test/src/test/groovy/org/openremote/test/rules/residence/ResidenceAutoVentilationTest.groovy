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
import org.openremote.model.rules.TemporaryFact
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.MINUTES
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1
import static org.openremote.model.attribute.AttributeEvent.Source.SENSOR

class ResidenceAutoVentilationTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Auto ventilation with CO2 detection"() {

        given: "the container environment is started"
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Demo Apartment - Auto Ventilation",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/demo/rules/DemoResidenceAutoVentilation.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the ventilation should be off"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert !apartment.getAttribute("ventilationAuto").get().getValue().isPresent()
            assert !apartment.getAttribute("ventilationLevel").get().getValue().isPresent()
        }

        when: "auto ventilation is turned on"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1Id, "ventilationAuto", Values.create(true))
        )

        then: "auto ventilation should be on"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationAuto").get().getValueAsBoolean().get()
            assert !apartment.getAttribute("ventilationLevel").get().getValue().isPresent()
        }

        when: "CO2 is increasing in a room"
        // The CO2 level increments 3 times, 2 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(700 + i)
            )
            simulatorProtocol.putValue(co2LevelIncrement)

            // Wait for event to be processed
            conditions.eventually {
                assert apartment1Engine.assetEvents.any() {
                    it.fact.matches(co2LevelIncrement, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
            }

            advancePseudoClock(2, MINUTES, container)
        }

        then: "ventilation level of the apartment should be MEDIUM"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").get().getValueAsNumber().get() == 128d
        }

        when: "CO2 is decreasing in a room"
        def co2LevelDecrement = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(500)
        )
        simulatorProtocol.putValue(co2LevelDecrement)

        then: "the decreasing CO2 should have been detected in rules"
        conditions.eventually {
            assert apartment1Engine.assetEvents.any() {
                it.fact.matches(co2LevelDecrement, SENSOR, true)
            }
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        when: "time advances"
        advancePseudoClock(35, MINUTES, container)

        then: "ventilation level of the apartment should be LOW"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").get().getValueAsNumber().get() == 64d
        }

        when: "CO2 is increasing in a room"
        // The CO2 level increments 3 times, 2 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(1000 + i)
            )
            simulatorProtocol.putValue(co2LevelIncrement)

            // Wait for event to be processed
            conditions.eventually {
                assert apartment1Engine.assetEvents.any() {
                    it.fact.matches(co2LevelIncrement, SENSOR, true)
                }
                assert noEventProcessedIn(assetProcessingService, 500)
            }

            advancePseudoClock(2, MINUTES, container)
        }

        then: "ventilation level of the apartment should be HIGH"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").get().getValueAsNumber().get() == 255d
        }

        when: "CO2 is decreasing in a room"
        def co2LevelDecrement2 = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(800)
        )
        simulatorProtocol.putValue(co2LevelDecrement2)
        conditions.eventually {
            assert apartment1Engine.assetEvents.any() {
                it.fact.matches(co2LevelDecrement2, SENSOR, true)
            }
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        and: "time advances"
        advancePseudoClock(20, MINUTES, container)

        then: "ventilation level of the apartment should be MEDIUM"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").get().getValueAsNumber().get() == 128d
        }

        when: "CO2 is decreasing in a room"
        def co2LevelDecrement3 = new AttributeEvent(
                managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(500)
        )
        simulatorProtocol.putValue(co2LevelDecrement3)
        conditions.eventually {
            assert apartment1Engine.assetEvents.any() {
                it.fact.matches(co2LevelDecrement3, SENSOR, true)
            }
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        and: "time advances"
        advancePseudoClock(15, MINUTES, container)

        then: "ventilation level of the apartment should be LOW"
        conditions.eventually {
            def apartment = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").get().getValueAsNumber().get() == 64d
        }

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }

}
