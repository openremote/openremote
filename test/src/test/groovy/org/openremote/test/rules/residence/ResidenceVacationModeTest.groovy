package org.openremote.test.rules.residence

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeExecuteStatus
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.DayOfWeek

import static java.util.concurrent.TimeUnit.DAYS
import static java.util.concurrent.TimeUnit.HOURS
import static org.openremote.manager.setup.SetupTasks.SETUP_IMPORT_DEMO_SCENES
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES

class ResidenceVacationModeTest extends Specification implements ManagerContainerTrait {

    def "Start and end vacation mode"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 20, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort) << [(SETUP_IMPORT_DEMO_SCENES): "true"], defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
                "Demo Apartment - Vacation Mode",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/demo/rules/DemoResidenceVacationMode.groovy").text,
                Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES

            // The macro should be ready
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            def executionStatus = AttributeExecuteStatus.fromString(
                    asset.getAttribute("awayScene").get().getValueAsString().get()
            ).get()
            assert executionStatus == AttributeExecuteStatus.READY
        }

        when: "the vacation days are set to 5"
        double fiveDaysInFuture = getClockTimeOf(container) + (5 * 24 * 60 * 60 * 1000)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1Id, "vacationUntil", Values.create(fiveDaysInFuture)
        ))

        then: "the AWAY scene should be executed and scene timers disabled"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            def executionStatus = AttributeExecuteStatus.fromString(
                    asset.getAttribute("awayScene").get().getValueAsString().get()
            ).get()
            assert executionStatus == AttributeExecuteStatus.COMPLETED
            assert !asset.getAttribute("sceneTimerEnabled").get().getValueAsBoolean().get()
            DayOfWeek.values().each {
                assert !asset.getAttribute("homeSceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert !asset.getAttribute("awaySceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert !asset.getAttribute("eveningSceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert !asset.getAttribute("nightSceneEnabled" + it.name()).get().getValueAsBoolean().get()
            }
        }

        when: "time advanced to the next day"
        advancePseudoClock(24, HOURS, container)

        then: "vacation mode is still on"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationUntil").get().getValueAsNumber().get() == fiveDaysInFuture
            assert !asset.getAttribute("sceneTimerEnabled").get().getValueAsBoolean().get()
            DayOfWeek.values().each {
                assert !asset.getAttribute("homeSceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert !asset.getAttribute("awaySceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert !asset.getAttribute("eveningSceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert !asset.getAttribute("nightSceneEnabled" + it.name()).get().getValueAsBoolean().get()
            }
        }

        when: "time advanced a few days"
        advancePseudoClock(5, DAYS, container)

        then: "vacation mode is off and scene timers are enabled"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert !asset.getAttribute("vacationUntil").get().getValue().isPresent()
            assert asset.getAttribute("sceneTimerEnabled").get().getValueAsBoolean().get()
            DayOfWeek.values().each {
                assert asset.getAttribute("homeSceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert asset.getAttribute("awaySceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert asset.getAttribute("eveningSceneEnabled" + it.name()).get().getValueAsBoolean().get()
                assert asset.getAttribute("nightSceneEnabled" + it.name()).get().getValueAsBoolean().get()
            }
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
