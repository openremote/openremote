package org.openremote.model.asset;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to encapsulate the result of a read asset tree request.
 * It contains the optimized assets {@link AssetTreeAsset}, the limit, the
 * offset, and whether there are more assets to fetch.
 */
public class AssetTree {

    private List<AssetTreeAsset> assets;
    private int limit;
    private int offset;
    private boolean hasMore;

    /**
     * Construct an asset tree using a list of unmapped assets.
     * 
     * @param assets      List of assets to be converted to asset tree assets.
     * @param limit       The limit of the assets to be returned.
     * @param offset      The offset of the assets to be returned.
     * @param hasMore     Whether there are more assets to fetch.
     * @param hasChildren Map of asset IDs with the respective hasChildren flag.
     */
    public AssetTree(List<Asset<?>> assets, int limit, int offset, boolean hasMore, Map<String, Boolean> hasChildren) {
        // Map the assets to asset tree assets
        this.assets = assets.stream().map(asset -> AssetTreeAsset.fromAsset(asset, hasChildren.get(asset.getId())))
                .collect(Collectors.toList());
        this.limit = limit;
        this.offset = offset;
        this.hasMore = hasMore;
    }

    public List<AssetTreeAsset> getAssets() {
        return assets;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public boolean hasMore() {
        return hasMore;
    }

    @Override
    public String toString() {
        return "AssetTree{" +
                "assets=" + assets.stream().map(AssetTreeAsset::toString).collect(Collectors.joining(", ")) +
                ", limit=" + limit +
                ", offset=" + offset +
                ", hasMore=" + hasMore +
                '}';
    }
}
