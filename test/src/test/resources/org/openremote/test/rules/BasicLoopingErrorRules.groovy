package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder

RulesBuilder rules = binding.rules

rules.add()
        .name("Another radial location predicate")
        .when(
                { facts ->
                    true
                })
        .then(
                { facts ->
                    facts.put("fired!")
                })

