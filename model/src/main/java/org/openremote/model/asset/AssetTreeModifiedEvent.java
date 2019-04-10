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
package org.openremote.model.asset;

import org.openremote.model.event.shared.TenantScopedEvent;

/**
 * Published by the server when either tenants or assets are modified, and when that
 * modification may impact any client-side asset tree structure.
 * <p>
 * Tree modifications are:
 * <ul>
 * <li>Tenant addition, removal, or name change</li>
 * <li>Asset addition, removal, or name change</li>
 * <li>Moving of assets between parent assets or tenants</li>
 * <li>Creation of a child asset</li>
 * </ul>
 * <p>
 */
public class AssetTreeModifiedEvent extends TenantScopedEvent {

    protected String assetId;
    protected boolean newAssetChildren;

    protected AssetTreeModifiedEvent() {
    }

    public AssetTreeModifiedEvent(long timestamp, String realm, String assetId) {
        super(timestamp, realm);
        this.realm = realm;
        this.assetId = assetId;
    }

    public AssetTreeModifiedEvent(long timestamp, String realm, String parentId, boolean newAssetChildren) {
        this(timestamp, realm, parentId);
        this.newAssetChildren = newAssetChildren;
    }

    public AssetTreeModifiedEvent(long timestamp, String realm, boolean newAssetChildren) {
        this(timestamp, realm, null);
        this.newAssetChildren = newAssetChildren;
    }

    /**
     * @return The identifier of the realm/tenant for tenant and asset addition, removal, name change
     * and relocation of in the tree events. For creation of child asset events, this is the identifier
     * of the realm of a newly created root asset.
     */
    @Override
    public String getRealm() {
        return super.getRealm();
    }

    /**
     * @return The identifier of the asset for asset addition, removal, name change, and
     * relocation in the tree events. For creation of child asset events, this is the
     * identifier of the parent asset. Empty if a tenant was modified and/or no asset was affected.
     */
    public String getAssetId() {
        return assetId;
    }

    public boolean isTenantModified() {
        return getRealm() != null && getAssetId() == null;
    }

    /**
     * @return <code>true</code> if child assets were added to parent {@link #assetId} or {@link #realm}.
     */
    public boolean isNewAssetChildren() {
        return newAssetChildren;
    }

    public void setNewAssetChildren(boolean newAssetChildren) {
        this.newAssetChildren = newAssetChildren;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realm='" + realm + '\'' +
            ", assetId='" + assetId + '\'' +
            ", newAssetChildren=" + newAssetChildren +
            '}';
    }
}
