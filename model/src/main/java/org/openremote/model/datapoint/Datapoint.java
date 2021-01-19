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

import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

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
    @Column(name = "ENTITY_ID", length = 36, nullable = false)
    protected String assetId;

    @Id
    @Column(name = "ATTRIBUTE_NAME", nullable = false)
    protected String attributeName;

    @Id
    @Column(name = "TIMESTAMP", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    protected Date timestamp;

    @Column(name = "VALUE", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected Object value;

    public Datapoint() {
    }

    public Datapoint(AttributeState attributeState, long timestamp) {
        this(attributeState.getRef(), attributeState.getValue().orElse(null), timestamp);
    }

    public Datapoint(AttributeEvent stateEvent) {
        this(stateEvent.getAttributeState(), stateEvent.getTimestamp());
    }

    public Datapoint(AttributeRef attributeRef, Object value, long timestamp) {
        this(attributeRef.getId(), attributeRef.getName(), value, timestamp);
    }

    public Datapoint(String assetId, String attributeName, Object value, long timestamp) {
        this.assetId = assetId;
        this.attributeName = attributeName;
        this.timestamp = new Date(timestamp);
        this.value = value;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public long getTimestamp() {
        return timestamp != null ? timestamp.getTime() : 0L;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = new Date(timestamp);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Datapoint datapoint = (Datapoint) o;

        return timestamp == datapoint.timestamp
            && assetId.equals(datapoint.assetId)
            && attributeName.equals(datapoint.attributeName)
            && value.equals(datapoint.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, attributeName, timestamp, value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetId='" + assetId + '\'' +
            ", attributeName='" + attributeName + '\'' +
            ", timestamp=" + timestamp +
            ", value=" + (value != null ? value : "null") +
            '}';
    }
}
