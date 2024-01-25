package org.openremote.test.rules.residence

import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.MINUTES
import static org.openremote.setup.integration.ManagerTestSetup.DEMO_RULE_STATES_APARTMENT_1

// Ignore this test as temporary facts (rule events) cause the rule engine to continually fire, need to decide if
// we support rule events or should it just be something rules do internally
@Ignore
class ResidenceAutoVentilationTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Auto ventilation with CO2 detection"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Demo Apartment - Auto Ventilation",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/ResidenceAutoVentilation.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available, running and settled"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "the ventilation should be off"
        conditions.eventually {
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert !apartment.getAttribute("ventilationAuto").get().getValue().isPresent()
            assert !apartment.getAttribute("ventilationLevel").get().getValue().isPresent()
        }

        when: "auto ventilation is turned on"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerTestSetup.apartment1Id, "ventilationAuto", true)
        )

        then: "auto ventilation should be on"
        conditions.eventually {
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationAuto").flatMap{it.value}.orElse(false)
            assert !apartment.getAttribute("ventilationLevel").get().getValue().isPresent()
        }

        when: "CO2 is increasing in a room"
        // The CO2 level increments 3 times, 2 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerTestSetup.apartment1LivingroomId, "co2Level", 700 + i
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
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").flatMap{it.value}.orElse(0d) == 128d
        }

        when: "CO2 is decreasing in a room"
        def co2LevelDecrement = new AttributeEvent(
                managerTestSetup.apartment1LivingroomId, "co2Level", 500
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
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").flatMap{it.value}.orElse(0d) == 64d
        }

        when: "CO2 is increasing in a room"
        // The CO2 level increments 3 times, 2 minutes apart
        for (i in 1..3) {

            def co2LevelIncrement = new AttributeEvent(
                    managerTestSetup.apartment1LivingroomId, "co2Level", 1000 + i
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
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").flatMap{it.value}.orElse(0d) == 255d
        }

        when: "CO2 is decreasing in a room"
        def co2LevelDecrement2 = new AttributeEvent(
                managerTestSetup.apartment1LivingroomId, "co2Level", 800
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
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").flatMap{it.value}.orElse(0d) == 128d
        }

        when: "CO2 is decreasing in a room"
        def co2LevelDecrement3 = new AttributeEvent(
                managerTestSetup.apartment1LivingroomId, "co2Level", 500
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
            def apartment = assetStorageService.find(managerTestSetup.apartment1Id, true)
            assert apartment.getAttribute("ventilationLevel").flatMap{it.value}.orElse(0d) == 64d
        }

            }
}
