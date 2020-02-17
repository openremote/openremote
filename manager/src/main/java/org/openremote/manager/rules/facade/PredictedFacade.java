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

import org.openremote.manager.predicted.AssetPredictedDatapointService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.rules.PredictedDatapoints;
import org.openremote.model.rules.Ruleset;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PredictedFacade<T extends Ruleset> extends PredictedDatapoints {

    private static final Logger LOG = Logger.getLogger(AssetsFacade.class.getName());

    protected final RulesEngineId<T> rulesEngineId;
    protected final AssetPredictedDatapointService assetPredictedDatapointService;

    public PredictedFacade(RulesEngineId<T> rulesEngineId, AssetPredictedDatapointService assetPredictedDatapointService) {
        this.rulesEngineId = rulesEngineId;
        this.assetPredictedDatapointService = assetPredictedDatapointService;
    }

    @Override
    public Stream<ValueDatapoint> getValueDataPoints(AssetAttribute attribute, long fromTimestamp, long toTimestamp) {
        return Arrays.stream(assetPredictedDatapointService.getValueDatapoints(attribute, fromTimestamp, toTimestamp));
    }
}
