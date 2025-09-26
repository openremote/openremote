package org.openremote.model.asset;

import java.util.Date;

/**
 * This class is used to represent an minimal asset for use in the asset tree.
 */
public class AssetTreeAsset {

    String id;
    String name;
    String type;
    boolean isParent;
    Date createdOn;

    public AssetTreeAsset(String id, String name, String type, boolean isParent, Date createdOn) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isParent = isParent;
        this.createdOn = createdOn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isParent() {
        return isParent;
    }

    public void setParent(boolean isParent) {
        this.isParent = isParent;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }


    public static AssetTreeAsset fromAsset(Asset<?> asset) {
        return new AssetTreeAsset(asset.getId(), asset.getName(), asset.getType(), asset.getParentId() == null, asset.getCreatedOn());
    }


    @Override
    public String toString() {
        return "AssetTreeAsset{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", isParent=" + isParent +
            ", createdOn=" + createdOn +
            '}';
    }
    
}
