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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.util.JsonUtil;

import java.util.Objects;
import java.util.Optional;

import static org.openremote.model.util.JsonUtil.*;

/**
 * The desired or current or past state of an {@link AttributeRef}.
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    creatorVisibility= JsonAutoDetect.Visibility.NONE,
    getterVisibility= JsonAutoDetect.Visibility.NONE,
    setterVisibility= JsonAutoDetect.Visibility.NONE,
    isGetterVisibility= JsonAutoDetect.Visibility.NONE
)
public class AttributeState {

    @JsonProperty
    protected AttributeRef attributeRef;
    @JsonProperty
    protected JsonValue jsonValue;

    protected AttributeState() {
    }

    public AttributeState(AttributeRef attributeRef) {
        this(attributeRef, Optional.empty());
    }

    public AttributeState(AttributeRef attributeRef, JsonValue jsonValue) {
        this(attributeRef, Optional.ofNullable(jsonValue));
    }

    public AttributeState(AttributeRef attributeRef, Optional<JsonValue> jsonValue) {
        Objects.requireNonNull(attributeRef);
        this.attributeRef = attributeRef;
        this.jsonValue = sanitizeJsonValue(jsonValue);
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public Optional<JsonValue> getValue() {
        return Optional.ofNullable(jsonValue);
    }

    public JsonValue toJsonValue() {
        JsonObject jsonObject = Json.createObject();
        jsonObject.put("attributeRef", getAttributeRef().toJsonValue());
        getValue().ifPresent(
            jsonValue -> jsonObject.put("value", sanitizeJsonValue(jsonValue))
        );
        return jsonObject;
    }

    @Override
    public String toString() {
        JsonValue value = sanitizeJsonValue(getValue());

        return getClass().getSimpleName() + "{" +
            "attributeRef=" + attributeRef +
            ", value=" + (value != null ? value.toJson() : "null") +
            '}';
    }

    public static boolean isAttributeState(JsonValue jsonValue) {
        return fromJsonValue(jsonValue) != null;
    }

    public static Optional<AttributeState> fromJsonValue(JsonValue jsonValue) {
        Optional<JsonObject> jsonObject = asJsonObject(jsonValue);
        Optional<JsonArray> jsonArray = jsonObject
            .flatMap(jsonObj -> asJsonArray((JsonValue)jsonObj.get("attributeRef")));

        if (!jsonObject.isPresent() || !jsonArray.isPresent() || !jsonObject.get().hasKey("value")) {
            return Optional.empty();
        }

        //noinspection ConstantConditions
        return jsonArray
            .flatMap(AttributeRef::fromJsonValue)
            .map(attributeRef ->
                new AttributeState(
                    attributeRef,
                    sanitizeJsonValue((JsonValue)jsonObject.get().get("value"))
                )
            );
    }

    public static JsonValue toJsonValue(AttributeState attributeState) {
        return attributeState.toJsonValue();
    }
}