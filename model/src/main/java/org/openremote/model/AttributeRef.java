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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

import java.util.Optional;

import static org.openremote.model.util.JsonUtil.asJsonArray;
import static org.openremote.model.util.JsonUtil.isOfTypeString;

/**
 * A reference to an entity and an {@link Attribute}.
 * <p>
 * The {@link #entityId} and {@link #attributeName} are required to identify
 * an entity's attribute.
 * <p>
 * Two attribute references are {@link #equals} if they reference the same entity
 * and attribute.
 */
public class AttributeRef {

    protected String entityId;
    protected String attributeName;

    /**
     * Needed for deserialisation
     */
    protected AttributeRef() {}

    public AttributeRef(String entityId, String attributeName) {
        this.entityId = entityId;
        this.attributeName = attributeName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAttributeName() {
        return attributeName;
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

    public JsonArray toJsonValue() {
        JsonArray jsonArray = Json.createArray();
        jsonArray.set(0, Json.create(getEntityId()));
        jsonArray.set(1, Json.create(getAttributeName()));
        return jsonArray;
    }

    public static boolean isAttributeRef(AbstractValueHolder valueHolder) {
        return valueHolder != null && valueHolder.getValueAsJsonArray()
            .map(AttributeRef::isAttributeRef)
            .orElse(false);
    }

    public static boolean isAttributeRef(JsonValue jsonValue) {
        return asJsonArray(jsonValue)
            .map(jsonArray ->
                jsonArray.length() == 2
                    && isOfTypeString(jsonArray.get(0))
                    && isOfTypeString(jsonArray.get(1))
                    && !jsonArray.getString(0).isEmpty()
                    && !jsonArray.getString(1).isEmpty()
            )
            .orElse(false);
    }

    public static Optional<AttributeRef> fromJsonValue(JsonValue jsonValue) {
        return asJsonArray(jsonValue)
            .map(jsonArray -> !isAttributeRef(jsonArray)
                    ? null
                    : new AttributeRef(jsonArray.getString(0), jsonArray.getString(1))
            );
    }

    public static JsonValue toJsonValue(AttributeRef attributeRef) {
        return attributeRef.toJsonValue();
    }
}