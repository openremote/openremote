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
package org.openremote.model.geo;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;

public class GeoJSON {

    public static final GeoJSON EMPTY_FEATURE_COLLECTION = new GeoJSON().setType("FeatureCollection").setEmptyFeatures();

    protected JsonObject jsonObject;

    public GeoJSON() {
        this(Json.createObject());
    }

    public GeoJSON(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public GeoJSON setType(String type) {
        jsonObject.put("type", type);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
    }

    public GeoJSON setFeatures(GeoJSONFeature... features) {
        if (features != null) {
            for (int i = 0; i < features.length; i++) {
                GeoJSONFeature feature = features[i];
                JsonArray array = Json.createArray();
                array.set(i, feature.getJsonObject());
                jsonObject.put("features", array);
            }
        } else {
            return setEmptyFeatures();
        }
        return this;
    }

    public GeoJSON setEmptyFeatures() {
        jsonObject.put("features", Json.createArray());
        return this;
    }

    public GeoJSONFeature[] getFeatures() {
        if (jsonObject.hasKey("features") && jsonObject.get("features").getType() == JsonType.ARRAY) {
            JsonArray array = jsonObject.getArray("features");
            GeoJSONFeature[] result = new GeoJSONFeature[array.length()];
            for (int i = 0; i < array.length(); i++) {
                result[i] = new GeoJSONFeature(array.getObject(i));
            }
            return result;
        } else {
            return new GeoJSONFeature[0];
        }
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
