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
package org.openremote.model.rules;

import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.util.TsIgnore;

/**
 * Facade for predicted datapoints in rules
 */
@TsIgnore
public abstract class PredictedDatapoints {


    public abstract ValueDatapoint<?>[] getValueDatapoints(AttributeRef attributeRef,
                                                        String truncate,
                                                        String interval,
                                                        long fromTimestamp,
                                                        long toTimestamp);


    public abstract ValueDatapoint<?>[] getValueDatapoints(AttributeRef attributeRef,
                                                        DatapointInterval interval,
                                                        long fromTimestamp,
                                                        long toTimestamp);

    public abstract void updateValue(String assetId, String attributeName, Object value, long timestamp);
    public abstract void updateValue(AttributeRef attributeRef, Object value, long timestamp);
}
