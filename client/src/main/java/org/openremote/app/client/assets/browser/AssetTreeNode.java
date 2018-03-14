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
package org.openremote.app.client.assets.browser;

import org.openremote.model.asset.Asset;

public class AssetTreeNode extends BrowserTreeNode {

    final protected Asset asset;

    public AssetTreeNode(Asset asset) {
        super(asset.getName());
        this.asset = asset;
    }

    @Override
    public String getId() {
        return asset.getId();
    }

    public Asset getAsset() {
        return asset;
    }

    @Override
    public boolean isLeaf() {
        return false;
        // RT: Removed as mentioned in #43
        //return Asset.isAssetTypeEqualTo(asset, AssetType.THING);
    }

    @Override
    public String getIcon() {
        return asset.getWellKnownType().getIcon();
    }
}
