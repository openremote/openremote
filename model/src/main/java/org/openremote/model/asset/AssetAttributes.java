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

public class AssetAttributes extends AbstractAssetAttributes<AssetAttributes, AssetAttribute> {

    public AssetAttributes() {
    }

    public AssetAttributes(JsonObject jsonObject) {
        super(jsonObject);
    }

    public AssetAttributes(AssetAttributes attributes) {
        super(attributes);
    }

    public AssetAttributes(String assetId) {
        super(assetId);
    }

    public AssetAttributes(String assetId, JsonObject jsonObject) {
        super(assetId, jsonObject);
    }

    public AssetAttributes(String assetId, AssetAttributes attributes) {
        super(assetId, attributes);
    }

    public AssetAttributes(Asset asset) {
        super(asset);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AssetAttributes copy() {
        return new AssetAttributes(assetId, Json.parse(getJsonObject().toJson()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AssetAttribute createAttribute(String name, JsonObject jsonObject) {
        return new AssetAttribute(assetId, name, jsonObject);
    }
}
