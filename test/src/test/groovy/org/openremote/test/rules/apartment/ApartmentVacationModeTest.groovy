package org.openremote.test.rules.apartment

import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
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
import static org.openremote.manager.server.setup.SetupTasks.SETUP_IMPORT_DEMO_SCENES
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES

class ApartmentVacationModeTest extends Specification implements ManagerContainerTrait {

    def "Start and end vacation mode"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 1)
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
                getClass().getResource("/demo/rules/DemoApartmentVacationMode.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1_WITH_SCENES
            setPseudoClocksToRealTime(container, apartment1Engine)
        }

        and: "scene timers should be enabled"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("sceneTimerEnabled").orElse(null).getValueAsBoolean().orElse(null)
            DayOfWeek.values().each {
                assert asset.getAttribute("homeSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert asset.getAttribute("awaySceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert asset.getAttribute("eveningSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert asset.getAttribute("nightSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
            }
        }

        when: "the vacation days are set to 5"
        setPseudoClocksToRealTime(container, apartment1Engine)
        double fiveDaysInFuture = getClockTimeOf(container) + (5 * 24 * 60 * 60 * 1000)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1Id, "vacationUntil", Values.create(fiveDaysInFuture)
        ))

        then: "the AWAY scene should be executed and scene timers disabled"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            def executionStatus = AttributeExecuteStatus.fromString(
                    asset.getAttribute("awayScene").orElse(null).getValueAsString().orElse(null)
            ).orElse(null)
            assert executionStatus == AttributeExecuteStatus.COMPLETED
            assert !asset.getAttribute("sceneTimerEnabled").orElse(null).getValueAsBoolean().orElse(null)
            DayOfWeek.values().each {
                assert !asset.getAttribute("homeSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert !asset.getAttribute("awaySceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert !asset.getAttribute("eveningSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert !asset.getAttribute("nightSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
            }
        }

        when: "time advanced to the next day"
        advancePseudoClocks(24, HOURS, container, apartment1Engine)

        then: "vacation mode is still on"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationUntil").orElse(null).getValueAsNumber().orElse(null) == fiveDaysInFuture
            assert !asset.getAttribute("sceneTimerEnabled").orElse(null).getValueAsBoolean().orElse(null)
            DayOfWeek.values().each {
                assert !asset.getAttribute("homeSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert !asset.getAttribute("awaySceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert !asset.getAttribute("eveningSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert !asset.getAttribute("nightSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
            }
        }

        when: "time advanced a few days"
        advancePseudoClocks(5, DAYS, container, apartment1Engine)

        then: "vacation mode is off and scene timers are enabled"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert !asset.getAttribute("vacationUntil").orElse(null).getValue().isPresent()
            assert asset.getAttribute("sceneTimerEnabled").orElse(null).getValueAsBoolean().orElse(null)
            DayOfWeek.values().each {
                assert asset.getAttribute("homeSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert asset.getAttribute("awaySceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert asset.getAttribute("eveningSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
                assert asset.getAttribute("nightSceneEnabled" + it.name()).orElse(null).getValueAsBoolean().orElse(null)
            }
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
