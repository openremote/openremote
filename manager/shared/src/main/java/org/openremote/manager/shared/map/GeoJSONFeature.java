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
package org.openremote.manager.shared.map;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class GeoJSONFeature {

    protected JsonObject jsonObject;

    public GeoJSONFeature() {
        this(Json.createObject());
    }

    public GeoJSONFeature(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public GeoJSONFeature setType(String type) {
        jsonObject.put("type", type);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
    }

    public GeoJSONFeature setProperty(String name, String value) {
        return setProperty(name, Json.create(value));
    }

    public GeoJSONFeature setProperty(String name, double value) {
        return setProperty(name, Json.create(value));
    }

    public GeoJSONFeature setProperty(String name, boolean value) {
        return setProperty(name, Json.create(value));
    }

    protected GeoJSONFeature setProperty(String name, JsonValue value) {
        JsonObject properties = jsonObject.hasKey("properties") ? jsonObject.getObject("properties") : Json.createObject();
        properties.put(name, value);
        if (!jsonObject.hasKey("properties"))
            jsonObject.put("properties", properties);
        return this;
    }

    public GeoJSONFeature setGeometry(GeoJSONGeometry geometry) {
        if (geometry != null) {
            jsonObject.put("geometry", geometry.getJsonObject());
        } else {
            jsonObject.remove("geometry");
        }
        return this;
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
