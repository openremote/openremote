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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.concurrent.GlobalLock;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Thread-safe base implementation for protocols.
 * <p>
 * Subclasses should use the {@link GlobalLock#withLock} and {@link GlobalLock#withLockReturning} methods
 * to guard critical sections when modifying shared state:
 * <blockquote><pre>{@code
 * withLock(getProtocolName(), () -> {
 *     // Critical section
 * });
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * return withLockReturning(() -> {
 *     // Critical section
 *     return ...;
 * });
 * }</pre></blockquote>
 * <p>
 * All <code>abstract</code> methods are always called within lock scope. An implementation can rely on this lock
 * and safely modify internal, protocol-specific shared state. However, if a protocol implementation schedules
 * an asynchronous task, this task must obtain the lock to call any protocol operations.
 */
public abstract class AbstractProtocol implements Protocol {

    protected static class LinkedProtocolInfo {

        final AssetAttribute protocolConfiguration;
        final Consumer<ConnectionStatus> connectionStatusConsumer;
        ConnectionStatus currentConnectionStatus;

        protected LinkedProtocolInfo(
            AssetAttribute protocolConfiguration,
            Consumer<ConnectionStatus> connectionStatusConsumer,
            ConnectionStatus currentConnectionStatus
        ) {
            this.protocolConfiguration = protocolConfiguration;
            this.connectionStatusConsumer = connectionStatusConsumer;
            this.currentConnectionStatus = currentConnectionStatus;
        }

        public AssetAttribute getProtocolConfiguration() {
            return protocolConfiguration;
        }

        public Consumer<ConnectionStatus> getConnectionStatusConsumer() {
            return connectionStatusConsumer;
        }

        public ConnectionStatus getCurrentConnectionStatus() {
            return currentConnectionStatus;
        }

        protected void setCurrentConnectionStatus(ConnectionStatus currentConnectionStatus) {
            this.currentConnectionStatus = currentConnectionStatus;
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractProtocol.class);
    public static final int PRIORITY = MessageBrokerService.PRIORITY + 100;
    protected final Map<AttributeRef, AssetAttribute> linkedAttributes = new HashMap<>();
    protected final Set<AttributeRef> dynamicAttributes = new HashSet<>();
    protected final Map<AttributeRef, LinkedProtocolInfo> linkedProtocolConfigurations = new HashMap<>();
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;
    protected TimerService timerService;
    protected ProtocolExecutorService executorService;
    protected ProtocolAssetService assetService;
    protected ProtocolPredictedAssetService predictedAssetService;
    protected ProtocolClientEventService protocolClientEventService;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        LOG.info("Initializing protocol: " + getProtocolName());
        timerService = container.getService(TimerService.class);
        executorService = container.getService(ProtocolExecutorService.class);
        assetService = container.getService(ProtocolAssetService.class);
        predictedAssetService = container.getService(ProtocolPredictedAssetService.class);
        protocolClientEventService = container.getService(ProtocolClientEventService.class);
        messageBrokerContext = container.getService(MessageBrokerService.class).getContext();

        withLock(getProtocolName() + "::start", () -> {
            try {
                messageBrokerContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from(ACTUATOR_TOPIC)
                            .routeId("Actuator-" + getProtocolName())
                            .process(exchange -> {
                                String protocolName = exchange.getIn().getHeader(ACTUATOR_TOPIC_TARGET_PROTOCOL, String.class);
                                if (!getProtocolName().equals(protocolName))
                                    return;
                                processLinkedAttributeWrite(exchange.getIn().getBody(AttributeEvent.class));
                            });
                    }
                });

                doStart(container);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @Override
    final public void start(Container container) throws Exception {
        LOG.fine("Starting protocol: " + getProtocolName());
        this.producerTemplate = container.getService(MessageBrokerService.class).getProducerTemplate();
    }

    @Override
    final public void stop(Container container) {
        withLock(getProtocolName() + "::stop", () -> {
            linkedAttributes.clear();
            try {
                messageBrokerContext.stopRoute("Actuator-" + getProtocolName(), 1, TimeUnit.MILLISECONDS);
                messageBrokerContext.removeRoute("Actuator-" + getProtocolName());

                doStop(container);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @Override
    final public void linkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration, Consumer<ConnectionStatus> statusConsumer) {
        withLock(getProtocolName() + "::linkProtocolConfiguration", () -> {
            LOG.finer("Linking protocol configuration to protocol '" + getProtocolName() + "': " + protocolConfiguration);

            ConnectionStatus currentStatus = protocolConfiguration.isEnabled() ? ConnectionStatus.CONNECTING : ConnectionStatus.DISABLED;

            linkedProtocolConfigurations.put(
                protocolConfiguration.getReferenceOrThrow(),
                new LinkedProtocolInfo(protocolConfiguration, statusConsumer, currentStatus)
            );
            statusConsumer.accept(currentStatus);
            if (currentStatus == ConnectionStatus.CONNECTING) {
                doLinkProtocolConfiguration(agent, protocolConfiguration);
            } else {
                LOG.info("Protocol configuration is disabled so not linking '" + getProtocolName() + "': " + protocolConfiguration);
            }
        });
    }

    @Override
    final public void unlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        withLock(getProtocolName() + "::unlinkProtocolConfiguration", () -> {

            LinkedProtocolInfo protocolInfo = linkedProtocolConfigurations.get(protocolConfiguration.getReferenceOrThrow());
            if (protocolInfo != null && protocolInfo.currentConnectionStatus != ConnectionStatus.DISABLED) {
                LOG.finer("Unlinking protocol configuration from protocol '" + getProtocolName() + "': " + protocolConfiguration);
                doUnlinkProtocolConfiguration(agent, protocolConfiguration);
                updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.DISCONNECTED);
            }
            linkedProtocolConfigurations.remove(protocolConfiguration.getReferenceOrThrow());
        });
    }

    @Override
    final public void linkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) {
        withLock(getProtocolName() + "::linkAttributes", () -> {

            if (!protocolConfiguration.isEnabled()) {
                LOG.info("Protocol configuration is disabled so not linking attributes '" + getProtocolName() + "': " + protocolConfiguration);
                return;
            }

            attributes.forEach(attribute -> {
                LOG.fine("Linking attribute to '" + getProtocolName() + "': " + attribute);
                AttributeRef attributeRef = attribute.getReferenceOrThrow();
                // Need to add to map before actual linking as protocols may want to update the value as part of
                // linking process and without entry in the map any update would be blocked
                linkedAttributes.put(attributeRef, attribute);

                // Check for dynamic value placeholder
                final String writeValue = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_WRITE_VALUE, false, true)
                    .map(Object::toString).orElse(null);

                if (!TextUtil.isNullOrEmpty(writeValue) && writeValue.contains(DYNAMIC_VALUE_PLACEHOLDER)) {
                    dynamicAttributes.add(attributeRef);
                }

                try {
                    doLinkAttribute(attribute, protocolConfiguration);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to link attribute to protocol: " + attribute, e);
                    linkedAttributes.remove(attributeRef);
                }
            });
        });
    }

    @Override
    final public void unlinkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception {
        withLock(getProtocolName() + "::unlinkAttributes", () -> {

            LinkedProtocolInfo protocolInfo = linkedProtocolConfigurations.get(protocolConfiguration.getReferenceOrThrow());
            if (protocolInfo.currentConnectionStatus == ConnectionStatus.DISABLED) {
                LOG.info("Protocol configuration is disabled so not unlinking attributes '" + getProtocolName() + "': " + protocolConfiguration);
                return;
            }

            attributes.forEach(attribute -> {
                LOG.fine("Unlinking attribute on '" + getProtocolName() + "': " + attribute);
                AttributeRef attributeRef = attribute.getReferenceOrThrow();
                linkedAttributes.remove(attributeRef);
                dynamicAttributes.remove(attributeRef);
                doUnlinkAttribute(attribute, protocolConfiguration);
            });
        });
    }

    /**
     * Gets a linked attribute by its attribute ref
     */
    protected AssetAttribute getLinkedAttribute(AttributeRef attributeRef) {
        return withLockReturning(getProtocolName() + "::getLinkedAttribute", () -> linkedAttributes.get(attributeRef));
    }

    /**
     * Get the protocol configuration that this attribute links to.
     */
    protected AssetAttribute getLinkedProtocolConfiguration(AssetAttribute attribute) {
        AttributeRef protocolConfigRef = AgentLink.getAgentLink(attribute).orElseThrow(() -> new IllegalStateException("Attribute is not linked to a protocol"));
        return getLinkedProtocolConfiguration(protocolConfigRef);
    }

    protected AssetAttribute getLinkedProtocolConfiguration(AttributeRef protocolConfigurationRef) {
        return withLockReturning(getProtocolName() + "::getLinkedProtocolConfigurations", () -> {
            LinkedProtocolInfo linkedProtocolInfo = linkedProtocolConfigurations.get(protocolConfigurationRef);
            // Don't bother with null check if someone calls here with an attribute not linked to this protocol
            // then they're doing something wrong so fail hard and fast
            return linkedProtocolInfo.getProtocolConfiguration();
        });
    }

    final protected void processLinkedAttributeWrite(AttributeEvent event) {
        LOG.finest("Processing linked attribute write on " + getProtocolName() + ": " + event);
        withLock(getProtocolName() + "::processLinkedAttributeWrite", () -> {
            AttributeRef attributeRef = event.getAttributeRef();
            AssetAttribute attribute = linkedAttributes.get(attributeRef);
            if (attribute == null) {
                LOG.warning("Attribute doesn't exist on this protocol: " + attributeRef);
            } else {

                Pair<Boolean, Value> ignoreAndConverted = Protocol.doOutboundValueProcessing(
                    attribute,
                    event.getValue().orElse(null),
                    dynamicAttributes.contains(attributeRef));

                if (ignoreAndConverted.key) {
                    LOG.fine("Value conversion returned ignore so attribute will not write to protocol: " + attribute.getReferenceOrThrow());
                    return;
                }

                AssetAttribute protocolConfiguration = getLinkedProtocolConfiguration(attribute);
                processLinkedAttributeWrite(event, ignoreAndConverted.value, protocolConfiguration);
            }
        });
    }

    /**
     * Send an arbitrary {@link AttributeState} through the processing chain using the current system time as the
     * timestamp. Use {@link #updateLinkedAttribute} to publish new sensor values, which performs additional
     * verification and uses a different messaging queue.
     */
    final protected void sendAttributeEvent(AttributeState state) {
        sendAttributeEvent(new AttributeEvent(state, timerService.getCurrentTimeMillis()));
    }

    /**
     * Send an arbitrary {@link AttributeEvent} through the processing chain. Use {@link #updateLinkedAttribute} to
     * publish new sensor values, which performs additional verification and uses a different messaging queue.
     */
    final protected void sendAttributeEvent(AttributeEvent event) {
        withLock(getProtocolName() + "::sendAttributeEvent", () -> {
            // Don't allow updating linked attributes with this mechanism as it could cause an infinite loop
            if (linkedAttributes.containsKey(event.getAttributeRef())) {
                LOG.warning("Cannot update an attribute linked to the same protocol; use updateLinkedAttribute for that: " + event);
                return;
            }
            assetService.sendAttributeEvent(event);
        });
    }

    /**
     * Update the value of a linked attribute. Call this to publish new sensor values. This will call
     * {@link #doInboundValueProcessing} before sending on the sensor queue.
     */
    final protected void updateLinkedAttribute(final AttributeState state, long timestamp) {
        AssetAttribute attribute = linkedAttributes.get(state.getAttributeRef());

        if (attribute == null) {
            LOG.severe("Update linked attribute called for un-linked attribute: " + state);
            return;
        }

        Pair<Boolean, Value> ignoreAndConverted = Protocol.doInboundValueProcessing(attribute, state.getValue().orElse(null), assetService);

        if (ignoreAndConverted.key) {
            LOG.fine("Value conversion returned ignore so attribute will not be updated: " + attribute.getReferenceOrThrow());
            return;
        }

        AttributeEvent attributeEvent = new AttributeEvent(new AttributeState(attribute.getReferenceOrThrow(), ignoreAndConverted.value), timestamp);
        LOG.fine("Sending on sensor queue: " + attributeEvent);
        producerTemplate.sendBodyAndHeader(SENSOR_QUEUE, attributeEvent, Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, getProtocolName());
    }

    /**
     * Update the value of a linked attribute, with the current system time as event time see
     * {@link #updateLinkedAttribute(AttributeState, long)} for more details.
     */
    final protected void updateLinkedAttribute(AttributeState state) {
        updateLinkedAttribute(state, timerService.getCurrentTimeMillis());
    }

    /**
     * Update a linked protocol configuration; allows protocols to reconfigure their own protocol configurations to
     * persist changing data e.g. authorization tokens. First this clones the existing protocolConfiguration and calls
     * the consumer to perform the modification.
     */
    final protected void updateLinkedProtocolConfiguration(AssetAttribute protocolConfiguration, Consumer<AssetAttribute> protocolUpdater) {
        withLock(getProtocolName() + "::updateLinkedProtocolConfiguration", () -> {
            // Clone the protocol configuration rather than modify this one
            AssetAttribute modifiedProtocolConfiguration = protocolConfiguration.deepCopy();
            protocolUpdater.accept(modifiedProtocolConfiguration);
            assetService.updateProtocolConfiguration(modifiedProtocolConfiguration);
        });
    }

    /**
     * Update the runtime status of a protocol configuration by its attribute ref
     */
    final protected void updateStatus(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        withLock(getProtocolName() + "::updateStatus", () -> {
            LinkedProtocolInfo protocolInfo = linkedProtocolConfigurations.get(protocolRef);
            if (protocolInfo != null) {
                LOG.fine("Updating protocol status to '" + connectionStatus + "': " + protocolRef);
                protocolInfo.getConnectionStatusConsumer().accept(connectionStatus);
                protocolInfo.setCurrentConnectionStatus(connectionStatus);
            }
        });
    }

    /**
     * Gets the current runtime status of a protocol configuration.
     */
    final protected ConnectionStatus getStatus(AssetAttribute protocolConfiguration) {
        return withLockReturning(getProtocolName() + "::getStatus", () -> {
            LinkedProtocolInfo linkedProtocolInfo = linkedProtocolConfigurations.get(protocolConfiguration.getReferenceOrThrow());
            return linkedProtocolInfo.getCurrentConnectionStatus();
        });
    }

    @Override
    final public ProtocolDescriptor getProtocolDescriptor() {
        return new ProtocolDescriptor(
            getProtocolName(),
            getProtocolDisplayName(),
            getVersion(),
            this instanceof ProtocolConfigurationDiscovery,
            this instanceof ProtocolConfigurationImport,
            this instanceof ProtocolLinkedAttributeDiscovery,
            this instanceof ProtocolLinkedAttributeImport,
            getProtocolConfigurationTemplate(),
            getProtocolConfigurationMetaItemDescriptors(),
            buildLinkedAttributeMetaItemDescriptors()
        );
    }

    protected List<MetaItemDescriptor> buildLinkedAttributeMetaItemDescriptors() {
        List<MetaItemDescriptor> descriptors = getLinkedAttributeMetaItemDescriptors();
        descriptors = descriptors != null ? new ArrayList<>(descriptors) : new ArrayList<>();

        // Add standard meta item descriptors that all protocols support
        if (descriptors.stream().noneMatch(d -> d.getUrn().equalsIgnoreCase(META_ATTRIBUTE_VALUE_FILTERS.getUrn()))) {
            descriptors.add(META_ATTRIBUTE_VALUE_FILTERS);
        }
        if (descriptors.stream().noneMatch(d -> d.getUrn().equalsIgnoreCase(META_ATTRIBUTE_VALUE_CONVERTER.getUrn()))) {
            descriptors.add(META_ATTRIBUTE_VALUE_CONVERTER);
        }
        if (descriptors.stream().noneMatch(d -> d.getUrn().equalsIgnoreCase(META_ATTRIBUTE_WRITE_VALUE.getUrn()))) {
            descriptors.add(META_ATTRIBUTE_WRITE_VALUE);
        }
        if (descriptors.stream().noneMatch(d -> d.getUrn().equalsIgnoreCase(META_ATTRIBUTE_WRITE_VALUE_CONVERTER.getUrn()))) {
            descriptors.add(META_ATTRIBUTE_WRITE_VALUE_CONVERTER);
        }

        return descriptors;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("protocolConfig"), getProtocolName());
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = new AttributeValidationResult(protocolConfiguration.getName().orElse(""));

        if (!ProtocolConfiguration.isProtocolConfiguration(protocolConfiguration)) {
            result.addMetaFailure(new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_MISSING, MetaItemType.PROTOCOL_CONFIGURATION.name()));
        }
        if (!ProtocolConfiguration.isValidProtocolName(protocolConfiguration.getValueAsString().orElse(null))) {
            result.addAttributeFailure(new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID));
        }
        return result;
    }

    /**
     * Start any background tasks and get necessary resources.
     */
    protected void doStart(Container container) throws Exception {
    }

    /**
     * Stop background tasks and close all resources.
     */
    protected void doStop(Container container) throws Exception {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    /**
     * Get list of {@link MetaItemDescriptor}s that describe the {@link MetaItem}s a {@link ProtocolConfiguration} for this
     * protocol supports
     */
    protected abstract List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors();

    /**
     * Get list of {@link MetaItemDescriptor}s that describe the {@link MetaItem}s an {@link Attribute} linked to this
     * protocol supports
     */
    abstract protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors();

    /**
     * Link the protocol configuration.
     */
    abstract protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration);

    /**
     * Unlink the protocol configuration.
     */
    abstract protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration);

    /**
     * Link an attribute to its linked protocol configuration.
     */
    abstract protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws Exception;

    /**
     * Unlink an attribute from its linked protocol configuration.
     */
    abstract protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration);

    /**
     * Attribute event (write) has been requested for an attribute linked to the specified protocol configuration. The
     * processedValue is the resulting {@link Value} after applying any {@link #META_ATTRIBUTE_WRITE_VALUE} and/or
     * {@link #META_ATTRIBUTE_WRITE_VALUE_CONVERTER} {@link MetaItem}s that are defined on the {@link Attribute}; if
     * neither are defined then the processedValue will be the same as {@link AttributeEvent#getValue}. Protocol
     * implementations should generally use the processedValue but may also choose to use the original value for some
     * purpose if required (e.g. {@link org.openremote.agent.protocol.http.HttpClientProtocol#META_QUERY_PARAMETERS}).
     */
    abstract protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration);
}
