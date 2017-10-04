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
package org.openremote.model.asset.agent;

import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;

/**
 * Published by the server when a a protocol configuration of an
 * agent changes its {@link ConnectionStatus}.
 */
public class AgentStatusEvent extends SharedEvent {

    protected String realmId;
    protected String assetId;
    protected boolean newAssetChildren;

    protected AgentStatusEvent() {
    }

    public AgentStatusEvent(long timestamp, String realmId, String assetId) {
        super(timestamp);
        this.realmId = realmId;
        this.assetId = assetId;
    }

    public AgentStatusEvent(long timestamp, String realmId, String parentId, boolean newAssetChildren) {
        this(timestamp, realmId, parentId);
        this.newAssetChildren = newAssetChildren;
    }

    public AgentStatusEvent(long timestamp, String realmId, boolean newAssetChildren) {
        this(timestamp, realmId, null);
        this.newAssetChildren = newAssetChildren;
    }

    /**
     * @return The identifier of the realm/tenant for tenant and asset addition, removal, name change
     * and relocation of in the tree events. For creation of child asset events, this is the identifier
     * of the realm of a newly created root asset.
     */
    public String getRealmId() {
        return realmId;
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
        return getRealmId() != null && getAssetId() == null;
    }

    /**
     * @return <code>true</code> if child assets were added to parent {@link #assetId} or {@link #realmId}.
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
            "realmId='" + realmId + '\'' +
            ", assetId='" + assetId + '\'' +
            ", newAssetChildren=" + newAssetChildren +
            '}';
    }
}
