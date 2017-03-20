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
package org.openremote.manager.client.assets.browser;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetInfo;
import org.openremote.model.asset.AssetType;

public class AssetTreeNode extends BrowserTreeNode {

    final protected AssetInfo assetInfo;

    final protected String tenantDisplayName;

    public AssetTreeNode(Asset asset, String tenantDisplayName) {
        this(new AssetInfo(asset), tenantDisplayName);
    }
    public AssetTreeNode(AssetInfo assetInfo, String tenantDisplayName) {
        super(assetInfo.getName());
        this.assetInfo = assetInfo;
        this.tenantDisplayName = tenantDisplayName;
    }

    public AssetInfo getAssetInfo() {
        return assetInfo;
    }

    public String getTenantDisplayName() {
        return tenantDisplayName;
    }

    @Override
    public String getId() {
        return assetInfo.getId();
    }

    @Override
    public boolean isLeaf() {
        return assetInfo.isWellKnownType(AssetType.THING);
    }

    @Override
    public String getIcon() {
        if (assetInfo.getWellKnownType() == null)
            return "cube";
        switch (assetInfo.getWellKnownType()) {
            case BUILDING:
                return "building";
            case RESIDENCE:
                return "cubes";
            case FLOOR:
                return "server";
            case AGENT:
                return "gears";
            case THING:
                return "gear";
            default:
                return "cube";
        }
    }
}
