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

import elemental.json.JsonValue;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;
import org.openremote.model.AttributeEvent;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ASSET_DATAPOINT")
@org.hibernate.annotations.Immutable
public class AssetDatapoint extends Datapoint {

    public AssetDatapoint() {
    }

    public AssetDatapoint(AttributeState attributeState) {
        super(attributeState);
    }

    public AssetDatapoint(AttributeEvent stateEvent) {
        super(stateEvent);
    }

    public AssetDatapoint(AttributeState attributeState, long timestamp) {
        super(attributeState, timestamp);
    }

    public AssetDatapoint(AttributeRef attributeRef, JsonValue value) {
        super(attributeRef, value);
    }

    public AssetDatapoint(AttributeRef attributeRef, JsonValue value, long timestamp) {
        super(attributeRef, value, timestamp);
    }

    public AssetDatapoint(String entityId, String attributeName, JsonValue value) {
        super(entityId, attributeName, value);
    }

    public AssetDatapoint(String entityId, String attributeName, JsonValue value, long timestamp) {
        super(entityId, attributeName, value, timestamp);
    }
}
