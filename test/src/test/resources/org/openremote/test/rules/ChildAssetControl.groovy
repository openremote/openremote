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
/*
* An example rule for writing to child asset attributes by writing to the parent asset attributes; just specify the
* parent asset ID below. The parent and child attributes must have the RULE_STATE configuration items set.
*/

package org.openremote.setup.integration.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Users users = binding.users
Notifications notifications = binding.notifications
Assets assets = binding.assets

// Put the asset ID of the parent whose children should be controlled in here
String parentAssetId = "3amk07u2gMoRlfzThYfwgT"

rules.add()
        .name("Control children by controlling attributes on the parent asset itself")
        .when(
                { facts ->

                    List<AttributeInfo> changes = Collections.synchronizedList(new ArrayList<>())

                    def childUpdates = facts.matchAssetState(
                            new AssetQuery().ids(parentAssetId)
                    ).filter { state ->
                        def changed = false

                        // Get previous state from facts
                        def previous = facts.matchFirst(state.id + state.name) as Optional<AttributeInfo>

                        if (previous.isEmpty()) {
                            // Store initial state to avoid triggering on first run
                            changes.add(state)
                        } else if (state.timestamp > previous.map { it.timestamp }.orElse(0)) {
                            // State has been updated (value may be the same but still push this to the children)
                            changed = true
                            changes.add(state)
                        }

                        return changed
                    }.flatMap { changedState ->
                        // Create attribute event for each child asset
                        LOG.info("Parent asset attribute changed: " + changedState)

                        // Get all child assets, create attribute events and bind these for the then trigger
                        facts.matchAssetState(new AssetQuery().parents(parentAssetId).attributeName(changedState.name))
                        .filter(childState -> childState.type == changedState.type)
                        .map{childState -> new AttributeEvent(childState.id, childState.name, changedState.value, changedState.timestamp)}
                    }.toList()

                    if (!changes.isEmpty()) {
                        facts.bind("changes", changes)

                        if (!childUpdates.isEmpty()) {
                            facts.bind("updates", childUpdates)
                            LOG.info("Child asset update count: " + childUpdates.size())
                        }
                    }

                    // Trigger the rule action if we have one or more changes to process
                    return !changes.isEmpty()
                })
        .then(
        { facts ->
            def changes = facts.bound("changes") as List<AttributeInfo>
            def updates = facts.bound("updates") as List<AttributeEvent>

            // Create fact for state of attribute
            changes.forEach {state ->
                facts.put(state.id + state.name, state)
            }

            if (updates != null) {
                updates.forEach {
                    assets.dispatch(it)
                }
            }
        })
