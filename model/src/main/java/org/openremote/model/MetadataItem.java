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
import org.openremote.model.asset.AssetAttributeMeta;

public class MetadataItem extends AbstractValueHolder<MetadataItem> {

    public MetadataItem() {
        this(Json.createObject());
    }

    public MetadataItem(JsonObject jsonObject) {
        super(jsonObject);
    }

    public MetadataItem(AssetAttributeMeta assetAttributeMeta) {
        this(assetAttributeMeta, null);
    }

    public MetadataItem(String name) {
        this(name, null);
    }

    public MetadataItem(AssetAttributeMeta assetAttributeMeta, JsonValue value) {
        this(assetAttributeMeta.getName(), value);
    }

    public MetadataItem(String name, JsonValue value) {
        super(Json.createObject());
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
        jsonObject.put("name", name);
        return this;
    }

    public MetadataItem copy() {
        return new MetadataItem(Json.parse(getJsonObject().toJson()));
    }

    public boolean isValid() {
        return getName() != null && getName().length() > 0 && hasValue();
    }
}
