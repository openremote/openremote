/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.connector;

import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.InventoryModifiedEvent;
import org.openremote.manager.shared.attribute.Attributes;

/**
 * Connectors to external systems are implemented as Apache Camel components.
 */
public interface ConnectorComponent {

    /**
     * Get the unique type descriptor for this connector component
     */
    String getType();

    /**
     * Get the friendly display name for this connector component
     */
    String getDisplayName();
    
    /**
     * Get the settings mask for creating/updating a child asset of the supplied
     * parent asset. This is used by clients for user entry of child assets.
     *
     * If parentAsset is null then child is being provisioned at the root
     * (in connector terms this usually means creating agent asset).
     */
    Attributes getConnectorSettings();

    /**
     * Return the inventory endpoint URI where {@link InventoryModifiedEvent}s
     * can be received. If null is returned, no inventory actions will be monitored.
     */
    String getInventoryUri(String agentAssetId, Agent agent);

    /**
     * Return the inventory endpoint URI where a user can send empty messages, which
     * should trigger internal discovery of child assets and possible asynchronous
     * reaction with {@link InventoryModifiedEvent}s.
     * If null is returned, triggering discovery will not be supported.
     */
    String getDiscoveryTriggerUri(String agentAssetId, Agent agent);

}
