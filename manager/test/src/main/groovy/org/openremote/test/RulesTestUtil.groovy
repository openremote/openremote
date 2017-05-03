package org.openremote.test

import org.kie.api.event.rule.AfterMatchFiredEvent
import org.kie.api.event.rule.DefaultAgendaEventListener
import org.openremote.manager.server.rules.RulesEngine

class RulesTestUtil {

    static boolean attachRuleExecutionLogger(RulesEngine ruleEngine, List<String> executedRules) {
        if (ruleEngine == null)
            return false

        def session = ruleEngine.getKnowledgeSession()
        def counter = 0
        while (session == null && counter < 20) {
            Thread.sleep(100)
            session = ruleEngine.getKnowledgeSession()
            counter++
        }

        if (session == null) {
            return false
        }

        session.addEventListener(new DefaultAgendaEventListener() {
            @Override
            void afterMatchFired(AfterMatchFiredEvent event) {
                def rule = event.getMatch().getRule()
                def ruleName = rule.getName()
                executedRules.add(ruleName)
            }
        })

        return true
    }

}
