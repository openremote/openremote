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
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

import static org.openremote.model.Constants.PERSISTENCE_JSON_VALUE_TYPE;

/**
 * <p>
 * A datapoint is used to hold time series data of an entity attribute.
 * <p>
 */
@MappedSuperclass
@IdClass(Datapoint.class)
public abstract class Datapoint implements Serializable {

    @Id
    @Column(name = "ENTITY_ID", length = 27, nullable = false)
    protected String entityId;

    @Id
    @Column(name = "ATTRIBUTE_NAME", nullable = false)
    protected String attributeName;

    @Id
    @Column(name = "TIMESTAMP", nullable = false)
    protected long timestamp = System.currentTimeMillis();

    @Id
    @Column(name = "VALUE", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected JsonValue value;

    public Datapoint() {
    }

    public Datapoint(AttributeState attributeState) {
        this(attributeState.getAttributeRef(), attributeState.getValue());
    }

    public Datapoint(AttributeEvent stateEvent) {
        this(stateEvent.getAttributeState(), stateEvent.getTimestamp());
    }

    public Datapoint(AttributeState attributeState, long timestamp) {
        this(attributeState.getAttributeRef(), attributeState.getValue(), timestamp);
    }

    public Datapoint(AttributeRef attributeRef, JsonValue value) {
        this(attributeRef.getEntityId(), attributeRef.getAttributeName(), value);
    }

    public Datapoint(AttributeRef attributeRef, JsonValue value, long timestamp) {
        this(attributeRef.getEntityId(), attributeRef.getAttributeName(), value, timestamp);
    }

    public Datapoint(String entityId, String attributeName, JsonValue value) {
        this(entityId, attributeName, value, System.currentTimeMillis());
    }

    public Datapoint(String entityId, String attributeName, JsonValue value, long timestamp) {
        this.entityId = entityId;
        this.attributeName = attributeName;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public JsonValue getValue() {
        return value;
    }

    public void setValue(JsonValue value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "entityId='" + entityId + '\'' +
            ", attributeName='" + attributeName + '\'' +
            ", timestamp=" + timestamp +
            ", value=" + (value != null ? value.toJson() : "null") +
            '}';
    }
}
