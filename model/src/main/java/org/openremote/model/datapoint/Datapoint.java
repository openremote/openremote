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

import org.openremote.model.attribute.*;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

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

    /**
     * @return <code>true</code> if datapoints can be stored and analyzed/displayed for the given value type.
     */
    public static boolean isDatapointsCapable(ValueType valueType) {
        switch (valueType) {
            case NUMBER:
            case BOOLEAN:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDatapointsCapable(Attribute attribute) {
        return attribute.getType().map(attributeType -> isDatapointsCapable(attributeType.getValueType())).orElse(false);
    }

    @Id
    @Column(name = "ENTITY_ID", length = 36, nullable = false)
    protected String entityId;

    @Id
    @Column(name = "ATTRIBUTE_NAME", nullable = false)
    protected String attributeName;

    @Id
    @Column(name = "TIMESTAMP", nullable = false)
    protected long timestamp;

    @Id
    @Column(name = "VALUE", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected Value value;

    public Datapoint() {
    }

    public Datapoint(AttributeState attributeState, long timestamp) {
        this(attributeState.getAttributeRef(), attributeState.getValue().orElse(null), timestamp);
    }

    public Datapoint(AttributeEvent stateEvent) {
        this(stateEvent.getAttributeState(), stateEvent.getTimestamp());
    }

    public Datapoint(AttributeRef attributeRef, Value value, long timestamp) {
        this(attributeRef.getEntityId(), attributeRef.getAttributeName(), value, timestamp);
    }

    public Datapoint(String entityId, String attributeName, Value value, long timestamp) {
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

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Datapoint datapoint = (Datapoint) o;

        return timestamp == datapoint.timestamp
            && entityId.equals(datapoint.entityId)
            && attributeName.equals(datapoint.attributeName)
            && value.equals(datapoint.value);
    }

    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + attributeName.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + value.hashCode();
        return result;
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
