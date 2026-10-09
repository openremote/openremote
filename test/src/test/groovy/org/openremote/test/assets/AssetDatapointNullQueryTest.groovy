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
package org.openremote.test.assets

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import java.time.temporal.ChronoUnit

class AssetDatapointNullQueryTest extends Specification implements ManagerContainerTrait {

  AssetStorageService assetStorageService
  AssetDatapointService historicalService
  AssetPredictedDatapointService predictedService
  ThingAsset asset
  String realm
  long start

  def setup() {
    def container = startContainer(defaultConfig(), defaultServices())
    stopPseudoClock()
    assetStorageService = container.getService(AssetStorageService.class)
    historicalService = container.getService(AssetDatapointService.class)
    predictedService = container.getService(AssetPredictedDatapointService.class)
    realm = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class).realmBuilding.name
    start = getInstantTimeOf(container).truncatedTo(ChronoUnit.SECONDS).toEpochMilli()
  }

  def cleanup() {
    if (asset?.id) {
      assert assetStorageService.delete([asset.id])
    }
  }

  def "numeric All query preserves #description for #storage #valueType.name datapoints"() {
    given: "numeric datapoints containing explicitly stored JSON nulls"
    asset = assetStorageService.merge(new ThingAsset("Null query test")
            .setRealm(realm).addOrReplaceAttributes(new Attribute<>("value", valueType)))
    def service = storage == "historical" ? historicalService : predictedService
    def timestamps = (0..<values.size()).collect { start + it * 60_000L }
    def datapoints = values.withIndex().collect { value, index ->
      new ValueDatapoint<>(timestamps[index], value)
    }
    // The query must include both boundary timestamps and exclude their neighbours.
    datapoints.add(new ValueDatapoint<>(timestamps.first() - 60_000L, 111G))
    datapoints.add(new ValueDatapoint<>(timestamps.last() + 60_000L, 222G))
    service.upsertValues(asset.id, "value", datapoints)

    when: "the inclusive range is queried without aggregation"
    def result = service.queryDatapoints(asset.id, "value",
            new AssetDatapointAllQuery(timestamps.first(), timestamps.last()))

    then: "every timestamp is returned newest first, retaining unavailable values"
    result.collect { it.timestamp } == timestamps.reverse()
    result.collect { it.value } == values.reverse()

    where:
    [storage, valueType, description, values] << ["historical", "predicted"].collectMany { storage ->
      [ValueType.NUMBER, ValueType.BIG_NUMBER].collectMany { type ->
        [
          [storage, type, "mixed values", [-5G, 0G, null, 90.125G]],
          [storage, type, "an entirely null range", [null, null, null]]
        ]
      }
    }
  }
}
