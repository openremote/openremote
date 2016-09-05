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
package org.openremote.manager.shared.attribute;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class MetadataElement {

    final protected String name;
    final protected JsonObject jsonObject;

    public MetadataElement(String name) {
        this(name, Json.createObject());
    }

    public MetadataElement(String name, String type) {
        this(name, Json.createObject());
        setType(type);
    }

    public MetadataElement(String name, String type, JsonValue value) {
        this(name, Json.createObject());
        setType(type);
        setValue(value);
    }

    public MetadataElement(String name, JsonObject jsonObject) {
        this.name = name;
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getName() {
        return name;
    }

    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    public MetadataElement setValue(JsonValue value) {
        jsonObject.put("value", value);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
    }

    public MetadataElement setType(String type) {
        jsonObject.put("type", type);
        return this;
    }

    public MetadataElement copy() {
        return new MetadataElement(getName(), Json.parse(getJsonObject().toJson()));
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }

}
