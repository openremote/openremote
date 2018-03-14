package org.openremote.test.failure

import org.openremote.manager.rules.RulesBuilder

RulesBuilder rules = binding.rules

rules.add()
        .name("Action always throws exception")
        .when(
        { facts ->
            true
        })
        .then(
        { facts ->
            throw new RuntimeException("Oops")
        })
