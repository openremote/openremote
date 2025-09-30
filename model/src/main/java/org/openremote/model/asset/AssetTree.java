/*
 * Copyright 2025, OpenRemote Inc.
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Asset tree data for the frontend asset tree.
 * Contains a list of {@link AssetTreeAsset} objects and minimal
 * pagination details indicating whether there are more assets to fetch.
 */
public class AssetTree {

    private List<AssetTreeAsset> assets;
    private boolean hasMore;

    public AssetTree() {
    }

    public AssetTree(List<Asset<?>> assets, boolean hasMore, Map<String, Boolean> hasChildren) {
        this.assets = assets.stream()
                // map list of assets to list of asset tree assets
                .map(asset -> AssetTreeAsset.fromAsset(asset,
                        hasChildren != null ? hasChildren.getOrDefault(asset.getId(), false) : false))
                .collect(Collectors.toList());
        this.hasMore = hasMore;
    }

    public List<AssetTreeAsset> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetTreeAsset> assets) {
        this.assets = assets;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    @Override
    public String toString() {
        return "AssetTree{" +
                "assets=" + assets.stream().map(AssetTreeAsset::toString).collect(Collectors.joining(", ")) +
                ", hasMore=" + hasMore +
                '}';
    }
}
