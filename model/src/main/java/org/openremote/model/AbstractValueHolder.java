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

    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    public String getValueAsString() {
        return jsonObject.hasKey("value") ? jsonObject.get("value").asString() : null;
    }

    public Boolean getValueAsBoolean() {
        return (jsonObject.hasKey("value") && jsonObject.get("value").getType() == JsonType.BOOLEAN)
            ? jsonObject.get("value").asBoolean()
            : null;
    }

    public boolean isValueTrue() {
        return getValueAsBoolean() != null && getValueAsBoolean();
    }

    public boolean isValueFalse() {
        return getValueAsBoolean() != null && !getValueAsBoolean();
    }

    public Double getValueAsNumber() {
        return jsonObject.hasKey("value") ? jsonObject.get("value").asNumber() : null;
    }

    public JsonObject getValueAsObject() {
        return jsonObject.hasKey("value") ? jsonObject.getObject("value") : null;
    }

    public JsonArray getValueAsArray() {
        return jsonObject.hasKey("value") ? jsonObject.getArray("value") : null;
    }

    public T setValue(JsonValue value) {
        jsonObject.put("value", value);
        return (T) this;
    }

    public T setValue(String value) {
        return setValue(Json.create(value));
    }

    public T setValue(Integer value) {
        return setValue(Json.create(value));
    }

    public T setValue(double value) {
        return setValue(Json.create(value));
    }

    public T setValue(boolean value) {
        return setValue(Json.create(value));
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
