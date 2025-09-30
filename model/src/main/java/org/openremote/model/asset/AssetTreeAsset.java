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

import java.util.Date;

/**
 * Minimal representation of asset data for usage in contexts where faster
 * loading is necessary.
 * Contains the asset ID, name, type, hasChildren flag, and creation date.
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

    /**
     * Convert an asset to an optimized asset tree asset.
     * 
     * @param asset       The asset to convert.
     * @param hasChildren Whether the asset has children. See
     *                    AssetStorageService.hasChildren() for more details.
     * @return The optimized asset tree asset.
     */
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
