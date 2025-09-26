package org.openremote.model.asset;

import java.util.Arrays;

public class AssetTree {

    private AssetTreeAsset[] assets;
    private int limit;
    private int offset;
    private boolean hasMore;

    /**
     * Construct an asset tree using a list of mapped asset tree assets.
     * @param assets
     * @param limit
     * @param offset
     * @param hasMore
     */
    public AssetTree(AssetTreeAsset[] assets, int limit, int offset, boolean hasMore) {
        this.assets = assets;
        this.limit = limit;
        this.offset = offset;
        this.hasMore = hasMore;
    }

    /**
     * Construct an asset tree using a list of unmapped assets.
     * @param assets
     * @param limit
     * @param offset
     * @param hasMore
     */
    public AssetTree(Asset<?>[] assets, int limit, int offset, boolean hasMore) {
        // Map the assets to asset tree assets
        this.assets = Arrays.stream(assets).map(AssetTreeAsset::fromAsset).toArray(AssetTreeAsset[]::new);
        this.limit = limit;
        this.offset = offset;
        this.hasMore = hasMore;
    }

    public AssetTreeAsset[] getAssets() {
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
            "assets=" + Arrays.toString(assets) +
            ", limit=" + limit +
            ", offset=" + offset +
            ", hasMore=" + hasMore +
            '}';
    }
}
