/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.setup.integration.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.LocationAttributePredicate
import org.openremote.model.query.filter.RadialGeofencePredicate
import org.openremote.model.query.filter.RectangularGeofencePredicate

RulesBuilder rules = binding.rules

rules.add()
        .name("Another radial location predicate")
        .when({ facts ->
          facts.matchFirstAssetState(new AssetQuery().attributes(new LocationAttributePredicate(new RadialGeofencePredicate(150, 10, 40)))).isPresent() &&
          !facts.matchFirst("RadialLocation2").isPresent()
        })
        .then({ facts ->
          facts.put("RadialLocation2", "fired")
        })

rules.add()
        .name("Duplicate radial location predicate")
        .when({ facts ->
          facts.matchFirstAssetState(new AssetQuery().attributes(new LocationAttributePredicate(new RadialGeofencePredicate(100, ManagerTestSetup.SMART_BUILDING_LOCATION.y, ManagerTestSetup.SMART_BUILDING_LOCATION.x)))).isPresent() &&
          !facts.matchFirst("Location").isPresent()
        })
        .then({ facts ->
          facts.put("DuplicateLocation", "fired")
        })

rules.add()
        .name("Rectangular location predicate")
        .when({ facts ->
          facts.matchFirstAssetState(new AssetQuery().attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(0, 50, 50, 100)))).isPresent() &&
          !facts.matchFirst("RectLocation").isPresent()
        })
        .then({ facts ->
          facts.put("RectLocation", "fired")
        })
