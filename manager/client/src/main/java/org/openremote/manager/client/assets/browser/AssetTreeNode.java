/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.client.assets.browser;

import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.asset.AssetInfo;
import org.openremote.model.asset.AssetType;

/**
 * Represent an asset, tenant, the invisible root, or a temporary node in the tree.
 * <p>
 * The invisible root of the tree (should be only one of those) is special, it has no
 * ID, no name, no realm, no type.
 */
public class AssetTreeNode {

    // This type is used when the node represents a tenant
    public static final String TENANT_TYPE = AssetTreeNode.class.getSimpleName() + ".TENANT";

    // This type is used when we have to stick a temporary node into the tree for whatever reason,
    // e.g. a node that is not an asset nor tenant, but for example a loading message or some
    // other UI signal for the user
    public static final String TEMPORARY_TYPE = AssetTreeNode.class.getSimpleName() + ".TEMPORARY";

    protected String id;

    protected String name;

    protected String type;

    /**
     * Invisible root node
     */
    public AssetTreeNode() {
    }

    public AssetTreeNode(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Asset node
     */
    public AssetTreeNode(AssetInfo assetInfo) {
        this(assetInfo.getId(), assetInfo.getName(), assetInfo.getType());
    }

    /**
     * Tenant node
     */
    public AssetTreeNode(Tenant tenant) {
        this(tenant.getId(), tenant.getDisplayName(), TENANT_TYPE);
    }

    /**
     * Temporary node
     */
    public AssetTreeNode(String name) {
        this(null, name, TEMPORARY_TYPE);
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

    public boolean isRoot() {
        return getId() == null;
    }

    public boolean isTemporary() {
        return TEMPORARY_TYPE.equals(getType());
    }

    public boolean isTenant() {
        return TENANT_TYPE.equals(getType());
    }

    public AssetType getWellKnownType() {
        return getType() != null ? AssetType.getByValue(getType()) : null;
    }

    public boolean isWellKnownType(AssetType assetType) {
        return assetType.equals(getWellKnownType());
    }

    public boolean isLeaf() {
        return isWellKnownType(AssetType.THING) || isTemporary();
    }

    public String getIcon() {
        if (isTenant())
            return "group";
        if (getWellKnownType() == null)
            return "cube";
        switch (getWellKnownType()) {
            case BUILDING:
                return "building";
            case RESIDENCE:
                return "cubes";
            case FLOOR:
                return "server";
            case AGENT:
                return "gears";
            case THING:
                return "gear";
            default:
                return "cube";
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            '}';
    }
}
