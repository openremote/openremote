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

import java.util.Collection;

/**
 * Connectors to external systems are implemented as Apache Camel components.
 */
public interface ConnectorComponent {

    String HEADER_DEVICE_KEY = ConnectorComponent.class.getCanonicalName() + ".HEADER_DEVICE_KEY";
    String HEADER_DEVICE_RESOURCE_KEY = ConnectorComponent.class.getCanonicalName() + ".HEADER_DEVICE_RESOURCE_KEY";

    enum Capability {
        /**
         * A producer that reacts to empty "trigger discovery" messages and notifies
         * any {@link #inventory} consumers when discovery of devices or device resources
         * resulted in any inventory changes.
         */
        discovery,

        /**
         * A producer that reacts to inventory update messages ("assign device", etc.), or
         * a consumer that starts {@link InventoryModifiedEvent} exchanges when device or device
         * resource inventory of this connector has been modified.
         */
        inventory,

        /**
         * A producer that can read a device resource value. Device and resource key will
         * be set as headers ({@link #HEADER_DEVICE_KEY}, {@link #HEADER_DEVICE_RESOURCE_KEY}),
         * the value must be returned as the body of the IN message.
         */
        read,

        /**
         * A producer that can write a device resource value. Device and resource key will
         * be set as headers ({@link #HEADER_DEVICE_KEY}, {@link #HEADER_DEVICE_RESOURCE_KEY}),
         * the value will be set as the body of the IN message.
         */
        write,

        /**
         * A consumer that starts exchanges when a device resource value is modified. The device
         * and resource key must be available as headers ({@link #HEADER_DEVICE_KEY},
         * {@link #HEADER_DEVICE_RESOURCE_KEY}), the resource value as body of the IN message.
         */
        listen
    }

    /**
     * Get the unique type descriptor for this connector component
     */
    String getType();

    /**
     * Get the friendly display name for this connector component
     */
    String getDisplayName();
    
    /**
     * Get the settings for configuring an {@link Agent} of this component.
     */
    Attributes getConnectorSettings();

    /**
     * Consumers supported by this connector.
     */
    Collection<Capability> getConsumerCapabilities();

    /**
     * Producers supported by this connector.
     */
    Collection<Capability> getProducerCapabilities();

    /**
     * Build a Camel consumer endpoint URI for the given capability and agent configuration. An
     * endpoint URI must be returned if the capability was announced in {@link #getConsumerCapabilities()}.
     * The <code>deviceKey</code> parameter is only available for consumer capability {@link Capability#listen}.
     */
    String buildConsumerEndpoint(Capability capability, String agentAssetId, Agent agent, String deviceKey);

    /**
     * Build a Camel producer endpoint URI for the given capability and agent configuration. An
     * endpoint URI must be returned if the capability was announced in {@link #getProducerCapabilities()}.
     */
    String buildProducerEndpoint(Capability capability, String agentAssetId, Agent agent);
}
