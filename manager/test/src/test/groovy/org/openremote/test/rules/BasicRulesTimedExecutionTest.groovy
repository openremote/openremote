package org.openremote.test.rules

import org.drools.core.time.impl.PseudoClockScheduler
import org.kie.api.runtime.conf.ClockTypeOption
import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.shared.rules.GlobalRuleset
import org.openremote.manager.shared.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.test.RulesTestUtil.attachRuleExecutionLogger

class BasicRulesTimedExecutionTest extends Specification implements ManagerContainerTrait {

    RulesDeployment globalEngine

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
        }

        when: "the execution logger is attached"
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)

        then: "after a few seconds the rule engines should have fire the timed execution rule in the background"
        new PollingConditions(initialDelay: 5).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() > 1
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Check firing of timer rules with pseudo clock"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10)

        and: "a pseudo rules engine clock"
        RulesDeployment.DefaultClockType = ClockTypeOption.get("pseudo")
        PseudoClockScheduler sessionClock = null

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
            sessionClock = globalEngine.sessionClock as PseudoClockScheduler
        }

        when: "the execution logger is attached"
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)

        then: "after a few seconds the rule engines should not have fired any rules"
        new PollingConditions(initialDelay: 3).eventually {
            assert globalEngineFiredRules.size() == 0
        }

        when: "the clock is advanced by a few seconds"
        sessionClock.advanceTime(5, SECONDS)

        then: "the rule engines should have fire the timed execution rule in the background"
        new PollingConditions(initialDelay: 1).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

        cleanup: "the server should be stopped"
        RulesDeployment.DefaultClockType = null
        stopContainer(container)
    }
}
