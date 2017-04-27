package org.openremote.test

import org.kie.api.event.rule.AfterMatchFiredEvent
import org.kie.api.event.rule.DefaultAgendaEventListener
import org.openremote.manager.server.rules.RulesEngine

class RulesTestUtil {

    static attachRuleExecutionLogger(RulesEngine ruleEngine, List<String> executedRules) {
        def session = ruleEngine.getKnowledgeSession()
        if (session == null) {
            return
        }
        session.addEventListener(new DefaultAgendaEventListener() {
            @Override
            void afterMatchFired(AfterMatchFiredEvent event) {
                def rule = event.getMatch().getRule()
                def ruleName = rule.getName()
                executedRules.add(ruleName)
            }
        })
    }

}
