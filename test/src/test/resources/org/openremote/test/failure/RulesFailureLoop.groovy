package org.openremote.test.failure

import org.openremote.manager.rules.RulesBuilder

RulesBuilder rules = binding.rules

rules.add()
        .name("Condition loops")
        .when(
        { facts ->
            true
        })
        .then(
        { facts ->
            // Never called
        })
