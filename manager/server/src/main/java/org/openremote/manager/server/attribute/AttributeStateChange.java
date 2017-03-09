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
package org.openremote.manager.server.attribute;

import elemental.json.JsonValue;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeRef;

public class AttributeStateChange {
    protected Object attributeParent;
    protected Attribute attribute;
    protected AttributeRef attributeRef;
    protected JsonValue oldValue;
    protected JsonValue newValue;
    protected boolean rewind;
    protected boolean handled;
    protected boolean error;

    public AttributeStateChange(Object attributeParent, Attribute attribute, AttributeRef attributeRef, JsonValue oldValue, JsonValue newValue) {
        this.attributeParent = attributeParent;
        this.attribute = attribute;
        this.attributeRef = attributeRef;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    void setRewind() {
        rewind = true;
        JsonValue oldValue = this.oldValue;
        this.oldValue = newValue;
        this.newValue = oldValue;
    }

    public Object getAttributeParent() {
        return attributeParent;
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public JsonValue getOldValue() {
        return oldValue;
    }

    public JsonValue getNewValue() {
        return newValue;
    }

    public boolean isRewind() {
        return rewind;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public boolean isHandled() {
        return handled;
    }

    public boolean isError() {
        return error;
    }

    /**
     * Update the value of the
     * @param newValue
     */
    public void updateNewValue(JsonValue newValue) {
        if (attribute.getType().getJsonType() != newValue.getType()) {
            error = true;
            throw new RuntimeException("Update Attribute state change value failed as new value type '" + newValue.getType() + "' doesn't match attribute type '" + attribute.getType().getJsonType() + "'");
        }

        this.newValue = newValue;
    }

    /**
     * Marks the state change as consumed so the state change won't be passed to any
     * more consumers in the chain
     * @param handled
     */
    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    /**
     * Two attribute state changes are considered equal if their attribute ref, old value
     * and new value are equal
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        AttributeStateChange that = (AttributeStateChange) o;

        return that.getAttributeRef().equals(this.getAttributeRef()) &&
                that.getOldValue().equals(this.getOldValue()) &&
                that.getNewValue().equals(this.getNewValue());
    }

    @Override
    public int hashCode() {
        return attributeRef.hashCode() + (oldValue != null ? oldValue.hashCode() : 0) + (newValue != null ? newValue.hashCode() : 0);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "attribute=" + attribute +
                ", oldValue=" + (oldValue != null ? oldValue.asString() : "null") +
                ", newValue=" + (newValue != null ? newValue.asString() : "null") +
                ", handled=" + handled +
                ", error=" + error +
                '}';
    }
}
