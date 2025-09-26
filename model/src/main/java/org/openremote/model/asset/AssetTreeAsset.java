package org.openremote.model.asset;

import java.util.Date;

/**
 * This class is used to represent an minimal asset for use in the asset tree.
 */
public class AssetTreeAsset {

    String id;
    String name;
    String type;

    boolean hasChildren;
    Date createdOn;

    public AssetTreeAsset(String id, String name, String type, boolean hasChildren, Date createdOn) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.hasChildren = hasChildren;
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

    public boolean hasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public static AssetTreeAsset fromAsset(Asset<?> asset, Boolean hasChildren) {
        return new AssetTreeAsset(asset.getId(), asset.getName(), asset.getType(), hasChildren, asset.getCreatedOn());
    }

    @Override
    public String toString() {
        return "AssetTreeAsset{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", hasChildren=" + hasChildren +
                ", createdOn=" + createdOn +
                '}';
    }

}
