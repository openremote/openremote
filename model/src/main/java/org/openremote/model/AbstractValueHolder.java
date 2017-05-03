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

import elemental.json.*;

import java.util.Objects;
import java.util.Optional;

import static org.openremote.model.util.JsonUtil.*;

/**
 * Base class for all model classes which have to internally store data in a
 * {@link JsonObject} that has a <code>value</code> field that accepts any
 * {@link JsonValue}.
 */
public abstract class AbstractValueHolder {
    
    protected static final String VALUE_FIELD_NAME = "value";

    final protected JsonObject jsonObject;

    public AbstractValueHolder(JsonObject jsonObject) {
        Objects.requireNonNull(jsonObject);
        this.jsonObject = jsonObject;
    }

    public boolean hasValue() {
        return getValue().isPresent();
    }

    public Optional<String> getValueAsString() {
        return asString(getValue());
    }

    public Optional<Integer> getValueAsInteger() {
        return asInteger(getValue());
    }

    public Optional<Double> getValueAsDecimal() {
        return asDecimal(getValue());
    }

    public Optional<Boolean> getValueAsBoolean() {
        return asBoolean(getValue());
    }

    public Optional<JsonObject> getValueAsJsonObject() {
        return asJsonObject(getValue());
    }

    public Optional<JsonArray> getValueAsJsonArray() {
        return asJsonArray(getValue());
    }

    public void clearValue() {
        jsonObject.remove(VALUE_FIELD_NAME);
    }

    public void setValue(String value) {
        doSetValue(asJsonValue(value));
    }

    public void setValue(Integer value) {
        setValue(asJsonValue(value));
    }

    public void setValue(Double value) {
        doSetValue(asJsonValue(value));
    }

    public void setValue(Boolean value) {
        doSetValue(asJsonValue(value));
    }

    /**
     * @throws IllegalArgumentException if the given value is invalid and failed constraint checking.
     */
    public void setValue(JsonValue value) throws IllegalArgumentException {
        doSetValue(sanitizeJsonValue(value));
    }

    /**
     * @throws IllegalArgumentException if the given value is invalid and failed constraint checking.
     */
    public void setValue(Optional<JsonValue> value) {
        doSetValue(sanitizeJsonValue(value));
    }

    protected void doSetValue(JsonValue jsonValue) throws IllegalArgumentException {
        if (!isValidValue(jsonValue)) {
            throw new IllegalArgumentException("Invalid value: " + (jsonValue != null ? jsonValue.toJson() : "null"));
        }

        if (jsonValue != null) {
            jsonObject.put(VALUE_FIELD_NAME, jsonValue);
        } else {
            clearValue();
        }

//        value = JsonUtil.replaceJsonNull(value)
//            .map(
//
//                val -> Json.instance().parse(val.toJson())
//            )
//            .map(val -> {
//                if (!isValidValue(val)) {
//                    //noinspection ConstantConditions
//                    throw new IllegalArgumentException("Invalid value of type " + val.getType() + ": " + val.toJson());
//                }
//                return val;
//            });
//
//        if (value.isPresent()) {
//            jsonObject.put(VALUE_FIELD_NAME, value.get());
//        } else {
//            clearValue();
//        }
    }

    public Optional<JsonValue> getValue() {
        return Optional.ofNullable(sanitizeJsonValue((JsonValue)jsonObject.get(VALUE_FIELD_NAME)));
    }

    /**
     * Override to implement constraints.
     */
    public boolean isValidValue(JsonValue value) {
        return true;
    }

    /**
     * Indicate that the jsonObject contains the mandatory data for this type of attribute
     * override in attribute sub classes as required
     */
    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }
}
