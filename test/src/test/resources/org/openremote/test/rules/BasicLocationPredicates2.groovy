package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.LocationAttributePredicate
import org.openremote.model.query.filter.RadialGeofencePredicate
import org.openremote.model.query.filter.RectangularGeofencePredicate

RulesBuilder rules = binding.rules

rules.add()
        .name("Another radial location predicate")
        .when(
        { facts ->
            facts.matchFirstAssetState(new AssetQuery().attributes(new LocationAttributePredicate(new RadialGeofencePredicate(150, 10, 40)))).isPresent() &&
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
        facts.matchFirstAssetState(new AssetQuery().attributes(new LocationAttributePredicate(new RadialGeofencePredicate(100, ManagerTestSetup.SMART_BUILDING_LOCATION.y, ManagerTestSetup.SMART_BUILDING_LOCATION.x)))).isPresent() &&
            !facts.matchFirst("Location").isPresent()
    })
    .then(
    { facts ->
        facts.put("DuplicateLocation", "fired")
    })

rules.add()
        .name("Rectangular location predicate")
        .when(
        { facts ->
            facts.matchFirstAssetState(new AssetQuery().attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(0, 50, 50, 100)))).isPresent() &&
                !facts.matchFirst("RectLocation").isPresent()
        })
        .then(
        { facts ->
            facts.put("RectLocation", "fired")
        })
