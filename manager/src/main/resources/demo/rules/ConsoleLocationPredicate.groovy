package demo.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.BaseAssetQuery

RulesBuilder rules = binding.rules

rules.add()
        .name("Radial location predicate")
        .when(
        { facts ->
            facts.matchFirstAssetState(new AssetQuery().location(new BaseAssetQuery.RadialLocationPredicate(100, 50, 100))).isPresent() &&
                    !facts.matchFirst("Location").isPresent()
        })
        .then(
        { facts ->
            facts.put("RadialLocation", "fired")
        })
