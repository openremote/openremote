/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.rules.facade;

import java.time.LocalDateTime;

import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.rules.PredictedDatapoints;
import org.openremote.model.rules.Ruleset;

public class PredictedFacade<T extends Ruleset> extends PredictedDatapoints {

  protected final RulesEngineId<T> rulesEngineId;
  protected final AssetPredictedDatapointService assetPredictedDatapointService;

  public PredictedFacade(
      RulesEngineId<T> rulesEngineId,
      AssetPredictedDatapointService assetPredictedDatapointService) {
    this.rulesEngineId = rulesEngineId;
    this.assetPredictedDatapointService = assetPredictedDatapointService;
  }

  @Override
  public ValueDatapoint<?>[] getValueDatapoints(
      AttributeRef attributeRef, AssetDatapointQuery query) {
    return assetPredictedDatapointService
        .queryDatapoints(attributeRef.getId(), attributeRef.getName(), query)
        .toArray(ValueDatapoint[]::new);
  }

  @Override
  public void updateValue(
      String assetId, String attributeName, Object value, LocalDateTime timestamp) {
    assetPredictedDatapointService.updateValue(assetId, attributeName, value, timestamp);
  }

  @Override
  public void updateValue(AttributeRef attributeRef, Object value, LocalDateTime timestamp) {
    updateValue(attributeRef.getId(), attributeRef.getName(), value, timestamp);
  }
}
