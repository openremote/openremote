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
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class MetadataItem {

    final protected JsonObject jsonObject;

    public MetadataItem(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public MetadataItem(String name) {
        this(name, null);
    }

    public MetadataItem(String name, JsonValue value) {
        this.jsonObject = Json.createObject();
        setName(name);
        setValue(value);
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getName() {
        return jsonObject.hasKey("name") ? jsonObject.get("name").asString() : null;
    }

    public MetadataItem setName(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Name can not be empty");
        jsonObject.put("name", name);
        return this;
    }

    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    public MetadataItem setValue(JsonValue value) {
        if (value != null) {
            jsonObject.put("value", value);
        }
        return this;
    }


    public MetadataItem copy() {
        return new MetadataItem(Json.parse(getJsonObject().toJson()));
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }

}
