package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.BaseAssetQuery

RulesBuilder rules = binding.rules

rules.add()
        .name("Another radial location predicate")
        .when(
        { facts ->
            facts.matchFirstAssetState(new AssetQuery().location(new BaseAssetQuery.RadialLocationPredicate(50, 0, -60))).isPresent() &&
                !facts.matchFirst("RadialLocation2").isPresent()
        })
        .then(
        { facts ->
            facts.put("RadialLocation2", "fired")
        })

rules.add()
    .name("Duplicate radial location predicate")
    .when(
    { facts ->
        facts.matchFirstAssetState(new AssetQuery().location(new BaseAssetQuery.RadialLocationPredicate(100, ManagerDemoSetup.SMART_HOME_LOCATION.y, ManagerDemoSetup.SMART_HOME_LOCATION.x))).isPresent() &&
            !facts.matchFirst("DuplicateLocation").isPresent()
    })
    .then(
    { facts ->
        facts.put("DuplicateLocation", "fired")
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
