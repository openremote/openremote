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

/**
 * Base class for all model classes which have to internally store data in a
 * {@link JsonObject} that has a <code>value</code> field that accepts any
 * {@link JsonValue}.
 */
public abstract class AbstractValueHolder<T extends AbstractValueHolder> {
    
    protected static final String VALUE_FIELD_NAME = "value";

    final protected JsonObject jsonObject;

    public AbstractValueHolder(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public boolean hasValue() {
        return jsonObject.hasKey(VALUE_FIELD_NAME) && jsonObject.get(VALUE_FIELD_NAME).getType() != JsonType.NULL;
    }

    public String getValueAsString() {
        return hasValue() ? getValue().asString() : null;
    }

    public Integer getValueAsInteger() {
        return hasValue() ? Integer.valueOf(getValue().asString()) : null;
    }

    public Double getValueAsDecimal() {
        return hasValue() ? getValue().asNumber() : null;
    }

    public Boolean getValueAsBoolean() {
        // TODO This is also broken somehow...
        // return hasValue() ? getValue_TODO_BUG_IN_JAVASCRIPT().asBoolean() : null;
        return hasValue() ? jsonObject.getBoolean(VALUE_FIELD_NAME) : null;
    }

    public JsonObject getValueAsObject() {
        return hasValue() ? jsonObject.getObject(VALUE_FIELD_NAME) : null;
    }

    public JsonArray getValueAsArray() {
        return hasValue() ? jsonObject.getArray(VALUE_FIELD_NAME) : null;
    }

    public boolean isValueTrue() {
        return getValueAsBoolean() != null && getValueAsBoolean();
    }

    public boolean isValueFalse() {
        return getValueAsBoolean() != null && !getValueAsBoolean();
    }

    public void clearValue() {
        jsonObject.remove(VALUE_FIELD_NAME);
    }

    public T setValueAsString(String value) {
        return setValue(value == null ? Json.createNull() : Json.create(value));
    }

    public T setValueAsInteger(Integer value) {
        return setValue(value == null ? Json.createNull() : Json.create(value));
    }

    public T setValueAsDecimal(Double value) {
        return setValue(value == null ? Json.createNull() : Json.create(value));
    }

    public T setValueAsBoolean(Boolean value) {
        return setValue(value == null ? Json.createNull() : Json.create(value));
    }

    public T setValueAsObject(JsonObject value) {
        return setValue(value == null ? Json.createNull() : value);
    }

    public T setValueAsArray(JsonArray value) {
        return setValue(value == null ? Json.createNull() : value);
    }

    /**
     * You can NOT perform null-checks on whatever is returned here!
     * <p>
     * TODO https://github.com/gwtproject/gwt/issues/9484
     */
    public JsonValue getValue() {
        return hasValue() ? jsonObject.get(VALUE_FIELD_NAME) : Json.createNull();
    }

    /**
     * @throws IllegalArgumentException if the given value is invalid and failed constraint checking.
     */
    @SuppressWarnings("unchecked")
    public T setValue(JsonValue value) throws IllegalArgumentException {

        if (value == null) {
            value = Json.createNull();
        } else if (value.getType() != JsonType.NULL){
            // TODO Avoid unboxing problems by parsing again https://github.com/gwtproject/gwt/issues/9484
            value = Json.instance().parse(value.toJson());
        }

        if (!isValidValue(value)) {
            String typeString = value != null ? value.getType().name() : "null";
            String valueString = value != null ? value.toJson() : "null";
            throw new IllegalArgumentException("Invalid value of type " + typeString + ": " + valueString);
        }

        jsonObject.put(VALUE_FIELD_NAME, value);

        return (T) this;
    }

    /**
     * Use this in Javascript, can't do null checks on JsonValue.
     */
    public void setValueUnchecked(JsonValue value) {
        jsonObject.put(VALUE_FIELD_NAME, value);
    }

    /**
     * Override to implement constraints.
     */
    protected boolean isValidValue(JsonValue value) {
        return value != null; // Don't allow literal nulls
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
