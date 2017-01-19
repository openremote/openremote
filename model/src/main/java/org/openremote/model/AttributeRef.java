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

import elemental.json.Json;
import elemental.json.JsonArray;

public class AttributeRef {

    final protected String entityId;
    final protected String attributeName;

    public AttributeRef(String entityId, String attributeName) {
        this.entityId = entityId;
        this.attributeName = attributeName;
    }

    public AttributeRef(JsonArray array) {
        this(array.get(0).asString(), array.get(1).asString());
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public JsonArray asJsonValue() {
        JsonArray jsonArray = Json.createArray();
        jsonArray.set(0, Json.create(getEntityId()));
        jsonArray.set(1, Json.create(getAttributeName()));
        return jsonArray;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeRef that = (AttributeRef) o;
        return entityId.equals(that.entityId) && attributeName.equals(that.attributeName);
    }

    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + attributeName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "entityId='" + entityId + '\'' +
            ", attributeName='" + attributeName + '\'' +
            '}';
    }
}
