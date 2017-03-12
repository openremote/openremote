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
package org.openremote.model;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import elemental.json.JsonValue;

/**
 * A timestamped {@link AttributeState}. Two attribute events
 * are equal if their timestamps and attribute states are equal.
 */
@JsonIgnoreType
public class AttributeEvent extends Event {

    final protected AttributeState attributeState;

    public AttributeEvent(AttributeState attributeState) {
        this.attributeState = attributeState;
    }

    public AttributeEvent(String entityId, String attributeName, JsonValue value) {
        this(new AttributeState(new AttributeRef(entityId, attributeName), value));
    }

    public AttributeEvent(AttributeRef attributeRef, JsonValue value) {
        this(new AttributeState(attributeRef, value));
    }

    public AttributeEvent(AttributeRef attributeRef, JsonValue value, long timestamp) {
        this(new AttributeState(attributeRef, value), timestamp);
    }

    public AttributeEvent(AttributeState attributeState, long timestamp) {
        super(timestamp);
        this.attributeState = attributeState;
    }

    public AttributeState getAttributeState() {
        return attributeState;
    }

    public AttributeRef getAttributeRef() {
        return getAttributeState().getAttributeRef();
    }

    public String getEntityId() {
        return getAttributeRef().getEntityId();
    }

    public String getAttributeName() {
        return getAttributeRef().getAttributeName();
    }

    public JsonValue getValue() {
        return getAttributeState().getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeEvent that = (AttributeEvent) o;

        return this.timestamp == that.timestamp &&
            attributeState.equals(that.attributeState);
    }

    @Override
    public int hashCode() {
        int result = new Long(timestamp).intValue();
        result = 31 * result + attributeState.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp=" + timestamp +
            ", attributeState=" + attributeState +
            "}";
    }
}
