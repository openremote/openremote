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

public abstract class AbstractValueHolder<T extends AbstractValueHolder> {

    final protected JsonObject jsonObject;

    public AbstractValueHolder(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public boolean hasValue() {
        return jsonObject.hasKey("value") && jsonObject.get("value").getType() != JsonType.NULL;
    }

    public String getValueAsString() {
        return hasValue() ? getValue_TODO_BUG_IN_JAVASCRIPT().asString() : null;
    }

    public Integer getValueAsInteger() {
        return hasValue()? Integer.valueOf(getValue_TODO_BUG_IN_JAVASCRIPT().asString()) : null;
    }

    public Double getValueAsDecimal() {
        return hasValue() ? getValue_TODO_BUG_IN_JAVASCRIPT().asNumber() : null;
    }

    public Boolean getValueAsBoolean() {
        // TODO This is also broken somehow...
        // return hasValue() ? getValue_TODO_BUG_IN_JAVASCRIPT().asBoolean() : null;
        return hasValue() ? jsonObject.getBoolean("value") : null;
    }

    public JsonObject getValueAsObject() {
        return hasValue() ? jsonObject.getObject("value") : null;
    }

    public JsonArray getValueAsArray() {
        return hasValue() ? jsonObject.getArray("value") : null;
    }

    public boolean isValueTrue() {
        return getValueAsBoolean() != null && getValueAsBoolean();
    }

    public boolean isValueFalse() {
        return getValueAsBoolean() != null && !getValueAsBoolean();
    }

    public void clearValue() {
        jsonObject.remove("value");
    }

    public T setValue(String value) {
        return setValue(Json.create(value));
    }

    public T setValue(int value) {
        return setValue(Json.create(value));
    }

    public T setValue(double value) {
        return setValue(Json.create(value));
    }

    public T setValue(boolean value) {
        return setValue(Json.create(value));
    }

    // You can NOT perform null-checks on whatever is returned here!
    // TODO https://github.com/gwtproject/gwt/issues/9484
    public JsonValue getValue_TODO_BUG_IN_JAVASCRIPT() {
        return hasValue() ? jsonObject.get("value") : Json.createNull();
    }

    public T setValue(JsonValue value) {
        jsonObject.put("value", value);
        return (T) this;
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
