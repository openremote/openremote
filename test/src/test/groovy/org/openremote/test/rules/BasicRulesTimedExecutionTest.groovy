/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TemporaryFact
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.setup.integration.ManagerTestSetup.DEMO_RULE_STATES_GLOBAL

@Ignore // TODO Implement timer/deferred actions in rules engine
class BasicRulesTimedExecutionTest extends Specification implements ManagerContainerTrait {

    RulesEngine globalEngine

    List<String> globalEngineFiredRules = new CopyOnWriteArrayList<>()

    def resetRuleExecutionLoggers() {
        globalEngineFiredRules.clear()
    }

    def "Check firing of timer rules with realtime clock"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "registered rules execution listeners"
        rulesService.ruleEngineExecutionListeners = { rulesEngine ->
            if (rulesEngine.id == RulesService.ID_GLOBAL_RULES_ENGINE) {
                return createRuleExecutionListener(globalEngineFiredRules)
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
            globalEngine = rulesService.globalEngine.get()
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

            }

    def "Check firing of timer rules with pseudo clock"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 30, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "registered rules execution listeners"
        rulesService.ruleEngineExecutionListeners = { rulesEngine ->
            if (rulesEngine.id == RulesService.ID_GLOBAL_RULES_ENGINE) {
                return createRuleExecutionListener(globalEngineFiredRules)
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
            globalEngine = rulesService.globalEngine.get()
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.knowledgeSession.factCount == DEMO_RULE_STATES_GLOBAL
        }

        and: "after a few seconds the rule engines should not have fired any rules"
        new PollingConditions(timeout: 5, initialDelay: 3).eventually {
            assert globalEngineFiredRules.size() == 0
        }

        when: "the clock is advanced by a few seconds"
        advancePseudoClock(100, SECONDS, container)
        // TODO Weird behavior of timer rules, pseudo clock, and active mode: More than 5 seconds is needed to fire rule twice
        // even more strange it stops at 2 even when advanced for more time. The same problems are with timer(cron: )
        // realtime clock seems to be OK

        then: "the rule engines should have fired the timed execution rule in the background"
        new PollingConditions(timeout: 5,initialDelay: 3).eventually {
            def expectedFiredRules = ["Log something every 2 seconds"]
            assert globalEngineFiredRules.size() > 1
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
        }

            }
}
