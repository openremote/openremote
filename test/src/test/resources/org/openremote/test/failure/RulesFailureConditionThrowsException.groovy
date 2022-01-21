package org.openremote.test.failure

import org.openremote.manager.rules.RulesBuilder

RulesBuilder rules = binding.rules

rules.add()
        .name("Condition always throws exception")
        .when(
        { facts ->
            throw new RuntimeException("Oops")
        })
        .then(
        { facts ->
            // Never called
        })
