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

import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;

/**
 * Published by the server when either tenants or assets are modified, and when that
 * modification may impact any client-side asset tree structure.
 * <p>
 * Tree modifications are:
 * <ul>
 * <li>Tenant addition, removal, or name change</li>
 * <li>Asset addition, removal, or name change</li>
 * <li>Moving of assets between parent assets or tenants</li>
 * </ul>
 * <p>
 * If a tenant is modified, the {@link #assetId} property will be <code>null</code>.
 */
public class AssetTreeModifiedEvent extends SharedEvent {

    public static class TenantFilter extends EventFilter<AssetTreeModifiedEvent> {

        public static final String FILTER_TYPE = "asset-tree-modified-tenant";

        protected String realmId;

        protected TenantFilter() {
        }

        public TenantFilter(String realmId) {
            this.realmId = realmId;
        }

        public String getRealmId() {
            return realmId;
        }

        @Override
        public String getFilterType() {
            return FILTER_TYPE;
        }

        @Override
        public AssetTreeModifiedEvent apply(AssetTreeModifiedEvent event) {
            if (getRealmId().equals(event.getRealmId())) {
                return event;
            }
            return null;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "realmId='" + realmId + '\'' +
                '}';
        }
    }

    protected String realmId;
    protected String assetId;

    protected AssetTreeModifiedEvent() {
    }

    public AssetTreeModifiedEvent(String realmId, String assetId) {
        this.realmId = realmId;
        this.assetId = assetId;
    }

    public AssetTreeModifiedEvent(String assetId) {
        this.assetId = assetId;
    }

    public String getRealmId() {
        return realmId;
    }

    public String getAssetId() {
        return assetId;
    }

    public boolean isTenantModified() {
        return getRealmId() != null && getAssetId() == null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realmId='" + realmId + '\'' +
            ", assetId='" + assetId + '\'' +
            '}';
    }
}
