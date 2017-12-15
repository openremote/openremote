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
package org.openremote.agent.protocol;

import org.openremote.agent.protocol.filter.MessageFilter;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.*;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

/**
 * A protocol is a thread-safe singleton {@link ContainerService} that connects devices and
 * services to the context broker. Each protocol implementation must have a unique
 * resource name (RfC 2141 URN) in a VM.
 * <h3>Configuring protocol instances</h3>
 * A protocol implementation must support multiple protocol configurations and therefore
 * support multiple logical instances. A protocol 'instance' can be defined by an
 * attribute on an {@link Asset} that has a type of {@link AssetType#AGENT}; the
 * attribute must conform to {@link ProtocolConfiguration}.
 * <p>
 * When a protocol configuration is loaded/created for a protocol then the
 * {@link #linkProtocolConfiguration} method will be called.
 * The protocol should check the {@link AssetAttribute#isEnabled} status of the protocol
 * configuration to determine whether or not the logical instance should be running or stopped.
 * <p>
 * The protocol is responsible for calling the provided consumer whenever the status of the
 * logical instance changes (e.g. if the configuration is not valid then the protocol should
 * call the consumer with a value of {@link ConnectionStatus#ERROR} and it should provide
 * sensible logging to allow fault finding).
 * <h3>Connecting attributes to actuators and sensors</h3>
 * {@link AssetAttribute}s of {@link Asset}s can be linked to a protocol configuration
 * instance by creating an {@link AssetMeta#AGENT_LINK} {@link MetaItem} on an attribute.
 * Besides the {@link AssetMeta#AGENT_LINK}, other protocol-specific meta items may also be
 * required when an asset attribute is linked to a protocol configuration instance.
 * Attributes linked to a protocol configuration instance will get passed to the protocol via
 * a call to {@link #linkAttributes}.
 * <p>
 * The protocol handles read and write of linked attributes:
 * <p>
 * If the actual state of the device (or service) changes, the linked protocol writes the new
 * state into the attribute value and notifies the context broker of the change. A protocol updates
 * a linked attributes' value by sending  an {@link AttributeEvent} messages on the
 * {@link #SENSOR_QUEUE}, including the source protocol name in header {@link #SENSOR_QUEUE_SOURCE_PROTOCOL}.
 * <p>
 * If the user writes a new value into the linked attribute, the protocol translates this value
 * change into a device (or service) action. Write operations on attributes linked to a protocol
 * configuration can be consumed by the protocol on the {@link #ACTUATOR_TOPIC} where the message
 * body will be an {@link AttributeEvent}. Each message also contains the target protocol name in
 * header {@link #ACTUATOR_TOPIC_TARGET_PROTOCOL}.
 * <p>
 * Data type conversion is also delegated to the protocol implementation: If an attribute has a particular
 * {@link AttributeType} and therefore a base {@link ValueType}, the protocol implementation must
 * receive and send value change messages with values of that type.
 * <p>
 * Generic protocols should implement support for filtering state messages from devices (or services) before the
 * protocol updates the linked attribute, to implement this protocols should use the {@link #META_PROTOCOL_FILTERS}
 * {@link MetaItem}.
 * <p>
 * NOTE: That {@link #linkProtocolConfiguration} will always be called
 * before {@link #linkAttributes} and {@link #unlinkAttributes} will always be called before
 * {@link #unlinkProtocolConfiguration}.
 * <p>
 * The following summarises the method calls protocols should expect:
 * <p>
 * Protocol configuration (logical instance) is created/loaded:
 * <ol>
 * <li>{@link #linkProtocolConfiguration}</li>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Protocol configuration (logical instance) is modified:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * <li>{@link #unlinkProtocolConfiguration)}</li>
 * <li>{@link #linkProtocolConfiguration}</li>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Protocol configuration (logical instance) is removed:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * <li>{@link #unlinkProtocolConfiguration}</li>
 * </ol>
 * <p>
 * Attribute linked to protocol configuration is created/loaded:
 * <ol>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Attribute linked to protocol configuration is modified:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Attribute link to protocol configuration is removed:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * </ol>
 */
public interface Protocol extends ContainerService {

    Logger LOG = Logger.getLogger(Protocol.class.getName());
    String ACTUATOR_TOPIC_TARGET_PROTOCOL = "Protocol";
    String SENSOR_QUEUE_SOURCE_PROTOCOL = "Protocol";

    /**
     * {@link MetaItem} for defining {@link MessageFilter}s to apply to values before they are sent on the
     * {@link #SENSOR_QUEUE} (i.e. before it is used to update a protocol linked attribute); this is particularly
     * useful for generic protocols. The {@link MetaItem} should be an {@link ArrayValue} of {@link ObjectValue}s where
     * each {@link ObjectValue} represents a serialised {@link MessageFilter}. The message should pass through the
     * filters in array order.
     */
    String META_PROTOCOL_FILTERS = PROTOCOL_NAMESPACE + ":filters";

    // TODO: Some of these options should be configurable depending on expected load etc.

    // Message topic for communicating from asset/thing to protocol layer (asset attribute changed, trigger actuator)
    String ACTUATOR_TOPIC = "seda://ActuatorTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    // Message queue for communicating from protocol to asset/thing layer (sensor changed, trigger asset attribute update)
    String SENSOR_QUEUE = "seda://SensorQueue?waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";

    /**
     * Get the name for this protocol
     */
    String getProtocolName();

    /**
     * Get the display friendly name for this protocol
     */
    String getProtocolDisplayName();

    /**
     * Get the version number for this protocol
     */
    String getVersion();

    /**
     * Links attributes to their protocolConfiguration; the protocolConfiguration would have
     * been linked before this call. If an attribute is not valid for this protocol or the protocol
     * Configuration then it is up to the protocol whether to put the entire protocolConfiguration
     * into an {@link ConnectionStatus#ERROR} state or to continue running (appropriate logging should
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
     * Links the protocol configuration to the protocol; the protocol is responsible for calling the statusConsumer
     * to indicate the runtime state of the protocol configuration. If the {@link ConnectionStatus} is set to
     * {@link ConnectionStatus#ERROR} or {@link ConnectionStatus#ERROR_CONFIGURATION} or this method throws an exception
     * then attribute linking will be skipped for this {@link ProtocolConfiguration}.
     */
    void linkProtocolConfiguration(AssetAttribute protocolConfiguration, Consumer<ConnectionStatus> statusConsumer)
        throws Exception;

    /**
     * Un-links the protocol configuration from the protocol; called whenever a protocolConfiguration is modified
     * or removed.
     */
    void unlinkProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Get a {@link ProtocolDescriptor} for this protocol.
     */
    ProtocolDescriptor getProtocolDescriptor();

    /**
     * Create an empty {@link ProtocolConfiguration} attribute that contains the required meta items needed
     * by the protocol. The purpose of this is to populate the UI when adding a new protocol configuration
     * for this protocol.
     */
    AssetAttribute getProtocolConfigurationTemplate();

    /**
     * Validate the supplied {@link ProtocolConfiguration} attribute against this protocol (should indicate that the
     * protocol configuration is well formed but not necessarily that it connects to a working system).
     */
    AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Extract the {@link MessageFilter}s from the specified {@link Attribute}
     */
    static Optional<List<MessageFilter>> getLinkedAttributeMessageFilters(Attribute attribute) {
        if (attribute == null) {
            return Optional.empty();
        }

        Optional<ArrayValue> arrayValueOptional = attribute.getMetaItem(META_PROTOCOL_FILTERS)
            .flatMap(AbstractValueHolder::getValueAsArray);

        if (!arrayValueOptional.isPresent()) {
            return Optional.empty();
        }

        try {
            ArrayValue arrayValue = arrayValueOptional.get();
            List<MessageFilter> messageFilters = new ArrayList<>(arrayValue.length());
            for (int i = 0; i < arrayValue.length(); i++) {
                ObjectValue objValue = arrayValue.getObject(i).orElseThrow(() ->
                    new IllegalArgumentException("Attribute protocol filters meta item is invalid"));
                MessageFilter filter = deserialiseMessageFilter(objValue);
                messageFilters.add(filter);
            }
            return messageFilters.isEmpty() ? Optional.empty() : Optional.of(messageFilters);
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Deserialise a {@link MessageFilter} from an {@link ObjectValue}
     */
    static MessageFilter deserialiseMessageFilter(ObjectValue objectValue) throws IllegalArgumentException {
        String json = objectValue.toJson();
        try {
            return Container.JSON.readValue(json, MessageFilter.class);
        } catch (IOException ioException) {
            LOG.log(Level.WARNING, "Failed to deserialise message filter", ioException);
            throw new IllegalArgumentException(ioException);
        }
    }
}
