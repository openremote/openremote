/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.protocol;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.event.shared.RealmScopedEvent;

/**
 * Initiates a protocol discovery process (either instance or asset discovery depending on {@link #isAssetDiscovery()});
 * for a given client only one discovery process can be running at a time so any already running discovery process will
 * be stopped.
 */
public class ProtocolDiscoveryStartRequestEvent extends RealmScopedEvent {

    protected String agentDescriptor;
    protected String assetId;
    protected boolean assetDiscovery;

    public ProtocolDiscoveryStartRequestEvent(String realm, String agentDescriptor, String assetId, boolean assetDiscovery) {
        super(realm);
        this.agentDescriptor = agentDescriptor;
        this.assetId = assetId;
        this.assetDiscovery = assetDiscovery;
    }

    public String getAgentDescriptor() {
        return agentDescriptor;
    }

    /**
     * If {@link #isAssetDiscovery} then this should be the ID of the {@link Agent} on which {@link Asset} discovery
     * should be initiated. Otherwise this should be the ID of the parent {@link Asset} under which the discovered
     * {@link Agent} would be created.
     */
    public String getAssetId() {
        return assetId;
    }

    public boolean isAssetDiscovery() {
        return assetDiscovery;
    }

}
