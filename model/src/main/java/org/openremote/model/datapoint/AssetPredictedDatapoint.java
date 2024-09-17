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
package org.openremote.model.datapoint;

import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.datapoint.Datapoint;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = AssetPredictedDatapoint.TABLE_NAME)
@org.hibernate.annotations.Immutable
public class AssetPredictedDatapoint extends Datapoint {
    public static final String TABLE_NAME = "asset_predicted_datapoint";

    public AssetPredictedDatapoint() {
    }

    public AssetPredictedDatapoint(AttributeState attributeState, long timestamp) {
        super(attributeState, timestamp);
    }

    public AssetPredictedDatapoint(AttributeEvent stateEvent) {
        super(stateEvent);
    }

    public AssetPredictedDatapoint(AttributeRef attributeRef, Object value, long timestamp) {
        super(attributeRef, value, timestamp);
    }

    public AssetPredictedDatapoint(String assetId, String attributeName, Object value, long timestamp) {
        super(assetId, attributeName, value, timestamp);
    }
}
