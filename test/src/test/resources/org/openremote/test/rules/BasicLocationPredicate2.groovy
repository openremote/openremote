package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.BaseAssetQuery

RulesBuilder rules = binding.rules

rules.add()
        .name("Radial location predicate")
        .when(
        { facts ->
            facts.matchFirstAssetState(new AssetQuery().location(new BaseAssetQuery.RadialLocationPredicate(200, 0, 50))).isPresent() &&
                !facts.matchFirst("RadialLocation").isPresent()
        })
        .then(
        { facts ->
            facts.put("RadialLocation", "fired")
        })

rules.add()
        .name("Rectangular location predicate")
        .when(
        { facts ->
            facts.matchFirstAssetState(new AssetQuery().location(new BaseAssetQuery.RectangularLocationPredicate(0, 50, 50, 100))).isPresent() &&
                !facts.matchFirst("RectLocation").isPresent()
        })
        .then(
        { facts ->
            facts.put("RectLocation", "fired")
        })
