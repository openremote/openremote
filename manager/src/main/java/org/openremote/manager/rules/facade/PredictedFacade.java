/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.rules.facade;

import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.rules.PredictedDatapoints;
import org.openremote.model.rules.Ruleset;

import java.time.LocalDateTime;

public class PredictedFacade<T extends Ruleset> extends PredictedDatapoints {

    protected final RulesEngineId<T> rulesEngineId;
    protected final AssetPredictedDatapointService assetPredictedDatapointService;

    public PredictedFacade(RulesEngineId<T> rulesEngineId, AssetPredictedDatapointService assetPredictedDatapointService) {
        this.rulesEngineId = rulesEngineId;
        this.assetPredictedDatapointService = assetPredictedDatapointService;
    }


    @Override
    public ValueDatapoint<?>[] getValueDatapoints(AttributeRef attributeRef, DatapointInterval interval, LocalDateTime fromTimestamp, LocalDateTime toTimestamp) {
        return assetPredictedDatapointService.getValueDatapoints(attributeRef, interval, null, fromTimestamp, toTimestamp);
    }

    @Override
    public ValueDatapoint<?>[] getValueDatapoints(AttributeRef attributeRef, DatapointInterval interval, int stepSize, LocalDateTime fromTimestamp, LocalDateTime toTimestamp) {
        return assetPredictedDatapointService.getValueDatapoints(attributeRef, interval, stepSize, fromTimestamp, toTimestamp);
    }

    @Override
    public void updateValue(String assetId, String attributeName, Object value, LocalDateTime timestamp) {
        assetPredictedDatapointService.updateValue(assetId, attributeName, value, timestamp);
    }

    @Override
    public void updateValue(AttributeRef attributeRef, Object value, LocalDateTime timestamp) {
        updateValue(attributeRef.getId(), attributeRef.getName(), value, timestamp);
    }
}
