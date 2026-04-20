/*
 * Copyright 2019, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * Allows {@link Asset}s to be represented in a hierarchical structure
 */
public class AssetTreeNode {

    public Asset<?> asset;
    public AssetTreeNode[] children;

    public AssetTreeNode(Asset<?> asset) {
        this.asset = asset;
    }

    @JsonCreator
    public AssetTreeNode(@JsonProperty("asset") Asset<?> asset,
                         @JsonProperty("children") AssetTreeNode... children) {
        this.asset = asset;
        this.children = children;
    }

    public Asset<?> getAsset() {
        return asset;
    }

    public AssetTreeNode[] getChildAssets() {
        return children;
    }

    public AssetTreeNode addChild(AssetTreeNode asset) {
        if (this == asset) {
            throw new IllegalArgumentException("Cannot add parent asset as a child");
        }

        if (children != null) {
            children = Arrays.copyOf(children, children.length+1);
            children[children.length-1] = asset;
        } else {
            children = new AssetTreeNode[] {asset};
        }

        return this;
    }
}
