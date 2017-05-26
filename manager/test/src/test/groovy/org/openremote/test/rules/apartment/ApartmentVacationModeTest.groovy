package org.openremote.test.rules.apartment

import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.*
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1

class ApartmentVacationModeTest extends Specification implements ManagerContainerTrait {

    def "Decrement vacation days at midnight"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
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
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
            assert apartment1Engine.knowledgeSession.factCount == DEMO_RULE_STATES_APARTMENT_1
        }

        when: "the vacation days are set to 5"
        setPseudoClocksToRealTime(container, apartment1Engine)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(
                managerDemoSetup.apartment1Id, "vacationDays", Values.create(5), getClockTimeOf(container)
        ))

        then: "that value should be stored"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationDays").get().getValueAsInteger().isPresent()
            assert asset.getAttribute("vacationDays").get().getValueAsInteger().get() == 5
        }

        when: "time advanced to the next day, which should trigger the cron rule"
        advancePseudoClocks(24, HOURS, container, apartment1Engine)

        then: "the vacation days should be decremented"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationDays").get().getValueAsInteger().isPresent()
            assert asset.getAttribute("vacationDays").get().getValueAsInteger().get() == 4
        }

        when: "time advanced again (to test that the rule only fires once per day)"
        advancePseudoClocks(10, SECONDS, container, apartment1Engine)

        then: "the vacation days should NOT be decremented"
        new PollingConditions(initialDelay: 2).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
            assert asset.getAttribute("vacationDays").get().getValueAsInteger().isPresent()
            assert asset.getAttribute("vacationDays").get().getValueAsInteger().get() == 4
        }

        expect: "the remaining vacation days to be decremented with each passing day"
        int remainingDays = 4
        while (remainingDays > 0) {

            remainingDays--

            advancePseudoClocks(1, DAYS, container, apartment1Engine)
            conditions.eventually {
                def asset = assetStorageService.find(managerDemoSetup.apartment1Id, true)
                assert asset.getAttribute("vacationDays").get().getValueAsInteger().get() == remainingDays
            }
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
