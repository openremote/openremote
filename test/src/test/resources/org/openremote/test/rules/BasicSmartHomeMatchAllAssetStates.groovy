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
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.query.AssetQuery
import static org.openremote.model.value.ValueType.*

RulesBuilder rules = binding.rules

rules.add()
        .name("Living Room All")
        .when(
        { facts ->
            !facts.matchFirst("Living Room All").isPresent() &&
                    facts.matchFirstAssetState(new AssetQuery().names("Living Room 1")).isPresent()
        })
        .then(
        { facts ->
            facts.put("Living Room All", "fired")
        })

rules.add()
        .name("Kitchen All")
        .when(
        { facts ->
            !facts.matchFirst("Kitchen All").isPresent() &&
                    facts.matchFirstAssetState(new AssetQuery().names("Kitchen 1")).isPresent()
        })
        .then(
        { facts ->
            facts.put("Kitchen All", "fired")
        })

rules.add()
        .name("Kitchen Number Attributes")
        .when(
        { facts ->
            !facts.matchFirst("Kitchen Number Attributes").isPresent() &&
                    facts.matchAssetState(new AssetQuery().names("Kitchen 1"))
                            .filter({ assetState -> assetState.type == NUMBER })
                            .findFirst().isPresent()
        })
        .then(
        { facts ->
            facts.put("Kitchen Number Attributes", "fired")
        })

rules.add()
        .name("Boolean attributes")
        .when(
        { facts ->
            !facts.matchFirst("Boolean attributes").isPresent() &&
                    facts.matchAssetState(new AssetQuery())
                            .filter({ assetState -> assetState.type == BOOLEAN })
                            .findFirst().isPresent()
        })
        .then(
        { facts ->
            facts.put("Boolean attributes", "fired")
        })

rules.add()
        .name("String attributes")
        .when(
        { facts ->
            !facts.matchFirst("String Attributes").isPresent() &&
                    facts.matchAssetState(new AssetQuery())
                            .filter({ assetState -> assetState.type == TEXT })
                            .findFirst().isPresent()
        })
        .then(
        { facts ->
            facts.put("String Attributes", "fired")
        })

rules.add()
        .name("Number value types")
        .when(
        { facts ->
            !facts.matchFirst("Number value types").isPresent() &&
                    facts.matchAssetState(new AssetQuery())
                            .filter({ assetState -> assetState.value.isPresent() })
                            .findFirst().isPresent()
        })
        .then(
        { facts ->
            facts.put("Number value types", "fired")
        })

rules.add()
        .name("Asset Type Room")
        .when(
        { facts ->
            !facts.matchFirst("Asset Type Room").isPresent() &&
                    facts.matchAssetState(new AssetQuery().types(RoomAsset))
                            .findFirst().isPresent()
        })
        .then(
        { facts ->
            facts.put("Asset Type Room", "fired")
        })

// This is never matched, living room doesn't have child assets - testing negative
rules.add()
        .name("Living Room as Parent")
        .when(
        { facts ->

            facts.matchAssetState(new AssetQuery().names("Living Room")).findFirst()
                    .map{livingRoomState ->
                            !facts.matchFirst("Living Room as Parent").isPresent() &&
                                    facts.matchAssetState(new AssetQuery())
                                            .filter({ assetState -> assetState.parentId == livingRoomState.id})
                                            .findFirst().isPresent()
                    }.orElse(false)

        })
        .then(
        { facts ->
            facts.put("Living Room as Parent", "fired")
        })
