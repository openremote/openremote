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
package org.openremote.agent.protocol;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;

/**
 * To be used by protocols that support linked {@link AssetAttribute} discovery.
 * <p>
 * The discovery process should return {@link AssetAttribute}s that contain the necessary {@link org.openremote.model.attribute.MetaItem}s
 * to establish a link to the specified {@link org.openremote.model.asset.agent.ProtocolConfiguration}.
 * <p>
 * The returned {@link AssetAttribute}s must be logically grouped into {@link Asset}s and then ordered into the desired
 * hierarchy using {@link AssetTreeNode}s.
 */
public interface ProtocolLinkedAttributeDiscovery {

    /**
     * Discover all linked {@link AssetAttribute}s for the specified {@link org.openremote.model.asset.agent.ProtocolConfiguration}.
     */
    AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration);
}
