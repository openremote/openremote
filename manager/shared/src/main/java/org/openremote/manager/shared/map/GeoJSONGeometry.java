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
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class GeoJSONGeometry {

    protected JsonObject jsonObject;

    public GeoJSONGeometry() {
        this(Json.createObject());
    }

    public GeoJSONGeometry(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public GeoJSONGeometry setType(String type) {
        jsonObject.put("type", type);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
    }

    public GeoJSONGeometry setPoint(double[] coordinates) {
        setType("Point");
        JsonArray array = Json.createArray();
        for (int i = 0; i < coordinates.length; i++) {
            array.set(i, coordinates[i]);
        }
        jsonObject.put("coordinates", array);
        return this;
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
