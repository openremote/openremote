// Renamed rules so they can be deployed again

package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.query.AssetQuery

RulesBuilder rules = binding.rules

rules.add()
        .name("All")
        .when(
        { facts ->
            !facts.matchFirst("All").isPresent() &&
                    facts.matchFirstAssetState(new AssetQuery()).isPresent()
        })
        .then(
        { facts ->
            facts.put("All", "fired")
        })
