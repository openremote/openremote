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
package org.openremote.agent3.protocol;

import org.openremote.container.ContainerService;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItem;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A Protocol implementation must have a unique name in a VM.
 * <p>
 * All protocol names must be in the {@link Constants#PROTOCOL_NAMESPACE}.
 * <p>
 * Protocols are singletons and 'instances' are defined by an attribute on an
 * {@link Asset} that has a type of {@link AssetType#AGENT}; the attribute must
 * conform to {@link ProtocolConfiguration}.
 * <p>
 * When a {@link ProtocolConfiguration} {@link AssetAttribute} is loaded/created for a protocol
 * then the {@link #linkProtocolConfiguration(AssetAttribute, Consumer)} method will be called.
 * <p>
 * When a protocolConfiguration is linked the protocol is responsible for calling the consumer
 * whenever the status of the protocolConfiguration changes (e.g. if the protocolConfiguration
 * is not valid then the protocol should call the consumer with a value of {@link DeploymentStatus#ERROR}
 * and it should provide sensible logging to allow fault finding.
 * <p>
 * The protocol should check the {@link AssetAttribute#isEnabled()} status of the protocolConfiguration
 * to determine whether or not it should be running or stopped.
 * <p>
 * {@link AssetAttribute}s of other {@link Asset}s can be linked to a protocolConfiguration by creating
 * an {@link AssetMeta#AGENT_LINK} {@link MetaItem} on the attribute, other MetaItems
 * may then be required that are specific to the linked protocolConfiguration so that it can determine
 * what data to read/write from/to the attribute. Attributes linked to a protocolConfiguration will get passed
 * to the protocol via a call to {@link #linkAttributes(Collection, AssetAttribute)}.
 * <p>
 * NOTE: That {@link #linkProtocolConfiguration(AssetAttribute, Consumer)} will always be called before
 * {@link #linkAttributes(Collection, AssetAttribute)} and {@link #unlinkAttributes(Collection, AssetAttribute)}
 * will always be called before {@link #unlinkProtocolConfiguration(AssetAttribute)}
 * <p>
 * Write operations on attributes linked to a protocolConfiguration can be consumed by the protocol
 * on the {@link #ACTUATOR_TOPIC} where the message body will be an {@link AttributeEvent}.
 * If a protocol wants to update a linked attributes' value then it can produce {@link AttributeEvent} messages
 * on the {@link #SENSOR_QUEUE}.
 * <p>
 * The linked protocol handles south-bound read and write of the attribute value: If the user writes
 * a new value into a linked attribute, the protocol translates this value change into a
 * device (or service) action. If the actual state of the device (or service) changes, the linked
 * protocol writes the new state into the attribute value and the asset system user is notified of the change.
 * <p>
 * Data type conversion is also delegated to the Protocol implementation: If an attribute has a particular
 * AttributeType and therefore a certain JsonValue, the Protocol must receive and send value change messages
 * with values of that type.
 * <p>
 * The following summarises the method calls protocols should expect:
 * <p>
 * Protocol Configuration is created/loaded:
 * <ol>
 *     <li>{@link #linkProtocolConfiguration(AssetAttribute, Consumer)}</li>
 *     <li>{@link #linkAttributes(Collection, AssetAttribute)}</li>
 * </ol>
 * <p>
 * Protocol Configuration is modified:
 * <ol>
 *     <li>{@link #unlinkAttributes(Collection, AssetAttribute)}</li>
 *     <li>{@link #unlinkProtocolConfiguration(AssetAttribute)}</li>
 *     <li>{@link #linkProtocolConfiguration(AssetAttribute, Consumer)}</li>
 *     <li>{@link #linkAttributes(Collection, AssetAttribute)}</li>
 * </ol>
 * <p>
 * Protocol Configuration is removed:
 * <ol>
 *     <li>{@link #unlinkAttributes(Collection, AssetAttribute)}</li>
 *     <li>{@link #unlinkProtocolConfiguration(AssetAttribute)}</li>
 * </ol>
 * <p>
 * Linked Attribute is created/loaded:
 * <ol>
 *     <li>{@link #linkAttributes(Collection, AssetAttribute)}</li>
 * </ol>
 * <p>
 * Linked Attribute is modified:
 * <ol>
 *     <li>{@link #unlinkAttributes(Collection, AssetAttribute)}</li>
 *     <li>{@link #linkAttributes(Collection, AssetAttribute)}</li>
 * </ol>
 * <p>
 * Linked Attribute is removed:
 * <ol>
 *     <li>{@link #unlinkAttributes(Collection, AssetAttribute)}</li>
 * </ol>
 */
public interface Protocol extends ContainerService {

    /**
     * Indicates the deployment status of a protocol configuration (i.e. protocol instance).
     */
    enum DeploymentStatus {

        /**
         * Protocol configuration has not yet been linked (i.e. {@link #linkProtocolConfiguration(AssetAttribute, Consumer)}
         * hasn't yet been called for the configuration.
         */
        UNLINKED,

        /**
         * Protocol configuration is being linked to the protocol.
         */
        LINKING,

        /**
         * Protocol configuration is being unlinked from the protocol.
         */
        UNLINKING,

        /**
         * Protocol configuration has been linked, is valid and it is enabled.
         */
        LINKED_ENABLED,

        /**
         * Protocol configuration has been linked, it is valid but it is not enabled (i.e. it has a
         * {@link org.openremote.model.asset.AssetMeta#ENABLED} {@link MetaItem}
         * with a value of false).
         */
        LINKED_DISABLED,

        /**
         * Protocol configuration is not valid or some internal protocol error occurred that prevents
         * this configuration from running (the protocol should provide sensible logging to allow fault
         * finding).
         */
        ERROR,

        /**
         * Protocol is performing an operation that means the status cannot be exactly determined at this
         * time (e.g. the protocol is trying to re-establish a connection to a remote server).
         */
        UPDATING
    }

    // TODO: Some of these options should be configurable depending on expected load etc.

    // Message topic for communicating from asset/thing to protocol layer (asset attribute changed, trigger actuator)
    String ACTUATOR_TOPIC = "seda://ActuatorTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    // Message queue for communicating from protocol to asset/thing layer (sensor changed, trigger asset attribute update)
    String SENSOR_QUEUE = "seda://SensorQueue?waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=false&size=1000";

    String getProtocolName();

    /**
     * Links attributes to their protocolConfiguration; the protocolConfiguration would have
     * been linked before this call. If an attribute is not valid for this protocol or the protocol
     * Configuration then it is up to the protocol whether to put the entire protocolConfiguration
     * into an {@link DeploymentStatus#ERROR} state or to continue running (appropriate logging should
     * always be used).
     * <p>
     * Attributes are linked to a protocol configuration via an {@link AssetMeta#AGENT_LINK} meta item.
     */
    void linkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration);

    /**
     * Un-links attributes from their protocolConfiguration; called whenever an attribute is modified or removed
     * or when the linked protocolConfiguration is modified or removed.
     */
    void unlinkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception;

    /**
     * Links the protocol configuration to the protocol; the protocol is responsible for calling the deploymentStatusConsumer
     * to indicate the state of the protocol configuration.
     */
    void linkProtocolConfiguration(AssetAttribute protocolConfiguration, Consumer<DeploymentStatus> deploymentStatusConsumer);

    /**
     * Un-links the protocol configuration from the protocol; called whenever a protocolConfiguration is modified
     * or removed.
     */
    void unlinkProtocolConfiguration(AssetAttribute protocolConfiguration);
}
