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
package org.openremote.model;

import elemental.json.JsonValue;

/**
 * The desired or current or past state of an {@link AttributeRef}.
 */
public class AttributeState {

    final protected AttributeRef attributeRef;
    final protected JsonValue value;

    public AttributeState(AttributeRef attributeRef, JsonValue value) {
        this.attributeRef = attributeRef;
        this.value = value;
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public JsonValue getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeRef=" + attributeRef +
            ", value=" + (value != null ? value.asString() : "null") +
            '}';
    }
}
