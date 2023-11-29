/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.datapoint;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;


public class AssetAnomalyDatapoint extends Datapoint {

    public AttributeAnomaly.AnomalyType anomalyType;
    public AssetAnomalyDatapoint() {
    }

    public AssetAnomalyDatapoint(AttributeState attributeState, long timestamp) {
        super(attributeState, timestamp);
        anomalyType = AttributeAnomaly.AnomalyType.Unchecked;
    }
    public AssetAnomalyDatapoint(AttributeState attributeState, long timestamp, AttributeAnomaly.AnomalyType anomalyType) {
        super(attributeState, timestamp);
        this.anomalyType = anomalyType;
    }

    public AssetAnomalyDatapoint(AttributeEvent stateEvent) {
        super(stateEvent);
    }

    public AssetAnomalyDatapoint(AttributeRef attributeRef, Object value, long timestamp) {
        super(attributeRef, value, timestamp);
    }
    public AssetAnomalyDatapoint(AttributeRef attributeRef, Object value, long timestamp, AttributeAnomaly.AnomalyType anomalyType) {
        super(attributeRef, value, timestamp);
        this.anomalyType = anomalyType;
    }

    public AssetAnomalyDatapoint(String assetId, String attributeName, Object value, long timestamp) {
        super(assetId, attributeName, value, timestamp);
    }
    public AssetAnomalyDatapoint(String assetId, String attributeName, Object value, long timestamp, AttributeAnomaly.AnomalyType anomalyType) {
        super(assetId, attributeName, value, timestamp);
        this.anomalyType = anomalyType;
    }
}
