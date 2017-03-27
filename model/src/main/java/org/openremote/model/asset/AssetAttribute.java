/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.asset;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.AttributeType;

public class AssetAttribute extends AbstractAssetAttribute<AssetAttribute> {

    public AssetAttribute() {
    }

    public AssetAttribute(String assetId) {
        super(assetId);
    }

    public AssetAttribute(String name, AttributeType type) {
        super(name, type);
    }

    public AssetAttribute(String name, JsonObject jsonObject) {
        super(name, jsonObject);
    }

    public AssetAttribute(String name, AttributeType type, JsonValue value) {
        super(name, type, value);
    }

    public AssetAttribute(String assetId, String name) {
        super(assetId, name);
    }

    public AssetAttribute(String assetId, String name, AttributeType type) {
        super(assetId, name, type);
    }

    public AssetAttribute(String assetId, String name, JsonObject jsonObject) {
        super(assetId, name, jsonObject);
    }

    public AssetAttribute(String assetId, String name, AttributeType type, JsonValue value) {
        super(assetId, name, type, value);
    }

    public AssetAttribute(AbstractAssetAttribute attribute) {
        super(attribute);
    }

    @Override
    public AssetAttribute copy() {
        return new AssetAttribute(assetId, getName(), Json.parse(getJsonObject().toJson()));
    }

}
