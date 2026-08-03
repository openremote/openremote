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
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

RulesBuilder rules = binding.rules
Assets assets = binding.assets

rules.add()
        .name("Long running rule")
        .when(
                {facts ->
                    List<AttributeEvent> updates = Collections.synchronizedList(new ArrayList<>())
                    facts.matchFirstAssetState(new AssetQuery().names("CounterAsset")).ifPresent { attr ->
                        updates.add(new AttributeEvent(attr.id, attr.name, attr.value.orElse(0) + 1))
                    }
                    facts.bind("updates", updates)
                    return true
                })
        .then(
                { facts ->
                    def updates = facts.bound("updates") as List<AttributeEvent>
                    updates.forEach { event ->
                        assets.dispatch(event) }

                    // This used to be a Thread.sleep() but one test requires the wait to be non interruptible
                    long startTime = System.currentTimeMillis()
                    while ((System.currentTimeMillis() - startTime) < 600) {
                        // Busy wait for 600ms - cannot be interrupted
                    }
                })
