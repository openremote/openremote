package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.shared.rules.GlobalRuleset
import org.openremote.manager.shared.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.container.timer.TimerService.Clock.PSEUDO
import static org.openremote.container.timer.TimerService.TIMER_CLOCK_TYPE
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_GLOBAL
import static org.openremote.test.RulesTestUtil.createRulesExecutionListener

class BasicRulesTimedExecutionTest extends Specification implements ManagerContainerTrait {

    RulesEngine globalEngine

    List<String> globalEngineFiredRules = []

    def resetRuleExecutionLoggers() {
        globalEngineFiredRules.clear()
    }

    def "Check firing of timer rules with realtime clock"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoScenesOrRules(defaultConfig(serverPort), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "registered rules execution listeners"
        rulesService.rulesEngineListeners = { rulesEngine ->
            if (rulesEngine.id == RulesService.ID_GLOBAL_RULES_ENGINE) {
                return createRulesExecutionListener(globalEngineFiredRules)
            }
        }

        and: "some test rulesets have been imported"
        Ruleset ruleset = new GlobalRuleset(
                "Some timer rules",
                getClass().getResource("/org/openremote/test/rules/BasicTimedExecutionRules.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            globalEngine = rulesService.globalEngine
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.knowledgeSession.factCount == DEMO_RULE_STATES_GLOBAL
        }

        and: "after a few seconds the rule engines should have fired the timed execution rule in the background"
        new PollingConditions(initialDelay: 22).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() > 10
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Check firing of timer rules with pseudo clock"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 30, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoScenesOrRules(defaultConfig(serverPort) << [(TIMER_CLOCK_TYPE): PSEUDO.name()], defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "registered rules execution listeners"
        rulesService.rulesEngineListeners = { rulesEngine ->
            if (rulesEngine.id == RulesService.ID_GLOBAL_RULES_ENGINE) {
                return createRulesExecutionListener(globalEngineFiredRules)
            }
        }

        and: "some test rulesets have been imported"
        Ruleset ruleset = new GlobalRuleset(
                "Some timer rules",
                getClass().getResource("/org/openremote/test/rules/BasicTimedExecutionRules.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            globalEngine = rulesService.globalEngine
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.knowledgeSession.factCount == DEMO_RULE_STATES_GLOBAL
        }

        and: "after a few seconds the rule engines should not have fired any rules"
        new PollingConditions(initialDelay: 3).eventually {
            assert globalEngineFiredRules.size() == 0
        }

        when: "the clock is advanced by a few seconds"
        advancePseudoClocks(100, SECONDS, container, globalEngine)
        // TODO Weird behavior of timer rules, pseudo clock, and active mode: More than 5 seconds is needed to fire rule twice
        // even more strange it stops at 2 even when advanced for more time. The same problems are with timer(cron: )
        // realtime clock seems to be OK

        then: "the rule engines should have fired the timed execution rule in the background"
        new PollingConditions(initialDelay: 3).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() > 1
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
