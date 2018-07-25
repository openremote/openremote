package org.openremote.test.failure

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.query.AssetQuery

RulesBuilder rules = binding.rules

rules.add()
        .name("The when condition is illegal, it's returning an Optional instead of a boolean")
        .when(
        { facts ->
            facts.matchLastAssetEvent(new AssetQuery())
        })
        .then(
        { facts ->
            // Never called
        })
