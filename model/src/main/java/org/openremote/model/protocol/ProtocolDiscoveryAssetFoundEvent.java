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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.event.shared.SharedEvent;

/**
 * Raised when protocol device asset(s) has been discovered
 */
public class ProtocolDiscoveryAssetFoundEvent extends SharedEvent {

    protected String agentDescriptor;
    protected AssetTreeNode[] assets;

    @JsonCreator
    public ProtocolDiscoveryAssetFoundEvent(@JsonProperty("agentDescriptor") String agentDescriptor, @JsonProperty("assets") AssetTreeNode[] assets) {
        this.agentDescriptor = agentDescriptor;
        this.assets = assets;
    }

    public String getAgentDescriptor() {
        return agentDescriptor;
    }

    public AssetTreeNode[] getAssets() {
        return assets;
    }
}
