package org.openremote.model.validation;

import org.openremote.model.attribute.Attribute;

/**
 * This is just used as a wrapper for validation purposes
 */
// TODO: Replace this with AssetState
@AssetStateValid
public class AssetStateStore {

    protected String assetType;
    protected Attribute<?> attribute;

    public AssetStateStore(String assetType, Attribute<?> attribute) {
        this.assetType = assetType;
        this.attribute = attribute;
    }

    public String getAssetType() {
        return assetType;
    }

    public Attribute<?> getAttribute() {
        return attribute;
    }
}
