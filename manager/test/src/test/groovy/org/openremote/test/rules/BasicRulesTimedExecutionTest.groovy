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
import static org.openremote.test.RulesTestUtil.attachRuleExecutionLogger
import static org.openremote.manager.server.setup.builtin.ManagerDemoSetup.*

class BasicRulesTimedExecutionTest extends Specification implements ManagerContainerTrait {

    RulesEngine globalEngine

    List<String> globalEngineFiredRules = []

    def resetRuleExecutionLoggers() {
        globalEngineFiredRules.clear()
    }

    def "Check firing of timer rules with realtime clock"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerWithoutDemoRules(defaultConfig(serverPort), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "some test rulesets have been imported"
        Ruleset ruleset = new GlobalRuleset(
                "Some timer rules",
                getClass().getResource("/org/openremote/test/rules/BasicTimedExecutionRules.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            globalEngine = rulesService.globalDeployment
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.knowledgeSession.factCount == DEMO_RULE_STATES_GLOBAL
        }

        when: "the execution logger is attached"
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)

        then: "after a few seconds the rule engines should have fired the timed execution rule in the background"
        new PollingConditions(initialDelay: 5).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() > 1
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Check firing of timer rules with pseudo clock"() {

        given: "the container environment is started"
        enablePseudoClock()
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithoutDemoRules(defaultConfig(serverPort), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "some test rulesets have been imported"
        Ruleset ruleset = new GlobalRuleset(
                "Some timer rules",
                getClass().getResource("/org/openremote/test/rules/BasicTimedExecutionRules.drl").text
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            globalEngine = rulesService.globalDeployment
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.knowledgeSession.factCount == DEMO_RULE_STATES_GLOBAL
        }

        when: "the execution logger is attached"
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)

        then: "after a few seconds the rule engines should not have fired any rules"
        new PollingConditions(initialDelay: 3).eventually {
            assert globalEngineFiredRules.size() == 0
        }

        when: "the clock is advanced by a few seconds"
        withClockOf(globalEngine) { it.advanceTime(10, SECONDS) }
        // TODO Weird behavior of timer rules, pseudo clock, and active mode: More than 5 seconds is needed to fire rule twice

        then: "the rule engines should have fired the timed execution rule in the background"
        new PollingConditions(initialDelay: 3).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() > 1
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

        cleanup: "the server should be stopped"
        disablePseudoClock()
        stopContainer(container)
    }
}
