package org.openremote.test

import org.kie.api.event.rule.AfterMatchFiredEvent
import org.kie.api.event.rule.AgendaEventListener
import org.kie.api.event.rule.DefaultAgendaEventListener

class RulesTestUtil {

    static AgendaEventListener createRulesExecutionListener(List<String> executedRules) {
        return new DefaultAgendaEventListener() {
            @Override
            void afterMatchFired(AfterMatchFiredEvent event) {
                def rule = event.getMatch().getRule()
                def ruleName = rule.getName()
                executedRules.add(ruleName)
            }
        }
    }

}
