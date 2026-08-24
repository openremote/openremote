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
package org.openremote.setup.load1

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.impl.WeatherAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users

import java.util.logging.Logger
import java.util.stream.Collectors

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Users users = binding.users
Notifications notifications = binding.notifications
Assets assets = binding.assets

// This rule updates an attribute called calculated for WeatherAssets by adding the rainfall and temperature attributes
// (no practical use for this calculation, it is just for testing purposes). Whenever either of these values change
// for an asset then the calculated attribute is updated.

rules.add()
        .name("Calculate weather asset calculated attribute")
        .when({ facts ->

          //LOG.info("Calculations start")

          Map<String, Double> lastValues = facts.get("lastValues") as Map<String, Double>
          if (lastValues == null) {
            lastValues = new HashMap<>()
          }

          // Get weather asset rainfall and/or temperature states that have changed value
          List<AttributeEvent> updates = facts.matchAssetState(
                          new AssetQuery()
                          .types(WeatherAsset)
                          .attributeNames(WeatherAsset.TEMPERATURE.name, WeatherAsset.RAINFALL.name)
                          ).collect(Collectors.groupingBy{state -> state.id})
                  .entrySet().parallelStream().map {entry ->
                    if (entry.value.size() != 2) {
                      return null
                    }
                    def value1 = entry.value[0].value.orElse(0) as double
                    def value2 = entry.value[1].value.orElse(0) as double
                    def calc = Double.valueOf(value1+value2)
                    def lastValue = lastValues.get(entry.key)
                    return !Objects.equals(lastValue, calc) ? new AttributeEvent(entry.key, "calculated", calc) : null
                  }.filter {it != null}
                  .toList()

          if (!updates.isEmpty()) {
            LOG.info("New values calculated for ${updates.size()} assets")
            facts.bind("updates", updates)
            facts.bind("lastValues", lastValues)
            updates.forEach {lastValues.put(it.id, it.value.orElseThrow() as Double)}
            return true
          }

          //LOG.info("Calculations end")
          // Trigger the rule action if we have one or more changes to process
          return false
        })
        .then({ facts ->
          def updates = facts.bound("updates") as List<AttributeEvent>
          def lastValues = facts.bound("lastValues")
          facts.put("lastValues", lastValues)
          LOG.info("Update start")
          assets.dispatch(updates.get(0))
          updates.forEach {
            assets.dispatch(it)
          }
          LOG.info("Update end")
        })
