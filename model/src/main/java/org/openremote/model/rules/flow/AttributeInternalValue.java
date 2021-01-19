package org.openremote.model.rules.flow;

public class AttributeInternalValue {
    private String assetId;
    private String attributeName;

    public AttributeInternalValue() {
    }

    public AttributeInternalValue(String assetId, String attributeName) {
        this.assetId = assetId;
        this.attributeName = attributeName;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
}
