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

    public boolean hasMore() {
        return hasMore;
    }

    @Override
    public String toString() {
        return "AssetTree{" +
                "assets=" + assets.stream().map(AssetTreeAsset::toString).collect(Collectors.joining(", ")) +
                ", hasMore=" + hasMore +
                '}';
    }
}
