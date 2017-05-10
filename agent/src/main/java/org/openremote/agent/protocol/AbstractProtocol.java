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
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.timer.TimerService;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.HasUniqueResourceName;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.*;
import org.openremote.model.util.Triplet;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractProtocol implements Protocol {

    private static final Logger LOG = Logger.getLogger(AbstractProtocol.class.getName());
    protected final Map<AttributeRef, AssetAttribute> linkedAttributes = new HashMap<>();
    protected final Map<AttributeRef, Triplet<AssetAttribute, Consumer<DeploymentStatus>, DeploymentStatus>> linkedProtocolConfigurations = new HashMap<>();
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;
    protected TimerService timerService;
    protected ProtocolExecutorService executorService;
    protected ProtocolAssetService assetService;

    @Override
    public void init(Container container) throws Exception {
        LOG.info("Initializing protocol: " + getProtocolName());
        timerService = container.getService(TimerService.class);
        executorService = container.getService(ProtocolExecutorService.class);
        assetService = container.getService(ProtocolAssetService.class);
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.fine("Starting protocol: " + getProtocolName());
        this.messageBrokerContext = container.getService(MessageBrokerSetupService.class).getContext();
        this.producerTemplate = container.getService(MessageBrokerService.class).getProducerTemplate();

        synchronized (linkedAttributes) {
            messageBrokerContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ACTUATOR_TOPIC)
                        .routeId("Actuator-" + getProtocolName())
                        .process(exchange -> {
                            String protocolName = exchange.getIn().getHeader(Protocol.ACTUATOR_TOPIC_TARGET_PROTOCOL, String.class);
                            if (getProtocolName().equals(protocolName)) {
                                // TODO Use read/write lock for link/unlink attributes synchronization and additional optional exclusive lock for single-threaded implementors
                                synchronized (linkedAttributes) {
                                    AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                                    AssetAttribute attribute = linkedAttributes.get(event.getAttributeRef());
                                    if (attribute == null) {
                                        LOG.warning("Attribute doesn't exist on this protocol: " + event.getAttributeRef());
                                    } else {
                                        AssetAttribute protocolConfiguration = getLinkedProtocolConfiguration(attribute);

                                        // If attribute is linked to protocol's enabled status handle here
                                        boolean isLinkedToEnabledStatus = attribute
                                            .getMetaItem(AssetMeta.PROTOCOL_PROPERTY)
                                            .flatMap(AbstractValueHolder::getValueAsString)
                                            .map(property -> property.equals("ENABLED"))
                                            .orElse(false);

                                        if (isLinkedToEnabledStatus) {
                                            LOG.fine("Attribute is linked to protocol's enabled property so handling write here");

                                            // check event value is a boolean
                                            boolean enabled = Values.getBoolean(event.getValue()
                                                .orElse(null))
                                                .orElseThrow(() -> new IllegalStateException("Writing to protocol configuration ENABLED property requires a boolean value"));

                                            if (enabled == protocolConfiguration.isEnabled()) {
                                                LOG.finer("Protocol configuration enabled status is already: " + enabled);
                                            } else {
                                                updateLinkedProtocolConfiguration(protocolConfiguration, AssetMeta.ENABLED, new MetaItem(AssetMeta.ENABLED, Values.create(enabled)));
                                            }
                                        } else {
                                            processLinkedAttributeWrite(event, protocolConfiguration);
                                        }
                                    }
                                }
                            }
                        });
                }
            });
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        synchronized (linkedAttributes) {
            linkedAttributes.clear();
            messageBrokerContext.stopRoute("Actuator-" + getProtocolName());
            messageBrokerContext.removeRoute("Actuator-" + getProtocolName());
        }
    }

    @Override
    public void linkProtocolConfiguration(AssetAttribute protocolConfiguration, Consumer<DeploymentStatus> deploymentStatusConsumer) {
        synchronized (linkedProtocolConfigurations) {
            LOG.finer("Linking protocol configuration to protocol '" + getProtocolName() + "': " + protocolConfiguration);
            linkedProtocolConfigurations.put(
                protocolConfiguration.getReferenceOrThrow(),
                new Triplet<>(protocolConfiguration, deploymentStatusConsumer, DeploymentStatus.LINKING)
            );
            doLinkProtocolConfiguration(protocolConfiguration);
        }
    }

    @Override
    public void unlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        synchronized (linkedProtocolConfigurations) {
            LOG.finer("Unlinking protocol configuration from protocol '" + getProtocolName() + "': " + protocolConfiguration);
            doUnlinkProtocolConfiguration(protocolConfiguration);
            linkedProtocolConfigurations.remove(protocolConfiguration.getReferenceOrThrow());
        }
    }

    @Override
    public void linkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) {
        synchronized (linkedAttributes) {
            attributes.forEach(attribute -> {
                LOG.fine("Linking attribute to '" + getProtocolName() + "': " + attribute);

                // If attribute is linked to protocol configuration enabled status handle here
                boolean isLinkedToEnabledStatus = attribute
                    .getMetaItem(AssetMeta.PROTOCOL_PROPERTY)
                    .flatMap(AbstractValueHolder::getValueAsString)
                    .map(property -> property.equals("ENABLED"))
                    .orElse(false);

                AttributeRef attributeRef = attribute.getReferenceOrThrow();
                linkedAttributes.put(attributeRef, attribute);

                if (isLinkedToEnabledStatus) {
                    LOG.fine("Attribute is linked to protocol's enabled property so handling here");
                    updateLinkedAttribute(new AttributeState(attributeRef, Values.create(protocolConfiguration.isEnabled())));
                } else {
                    doLinkAttribute(attribute, protocolConfiguration);
                }
            });
        }
    }

    @Override
    public void unlinkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception {
        synchronized (linkedAttributes) {
            attributes.forEach(attribute -> {
                LOG.fine("Unlinking attribute on '" + getProtocolName() + "': " + attribute);

                // If attribute is linked to protocol's enabled status handle here
                boolean isLinkedToEnabledStatus = attribute
                    .getMetaItem(AssetMeta.PROTOCOL_PROPERTY)
                    .flatMap(AbstractValueHolder::getValueAsString)
                    .map(property -> property.equals("ENABLED"))
                    .orElse(false);

                if (!isLinkedToEnabledStatus) {
                    // Only pass to the protocol if it is not linked to enabled status
                    doUnlinkAttribute(attribute, protocolConfiguration);
                }
                linkedAttributes.remove(attribute.getReferenceOrThrow());
            });
        }
    }

    /**
     * Gets a linked attribute by its attribute ref
     */
    protected AssetAttribute getLinkedAttribute(AttributeRef attributeRef) {
        synchronized (linkedAttributes) {
            return linkedAttributes.get(attributeRef);
        }
    }

    /**
     * Get the protocol configuration that this attribute links to.
     */
    protected AssetAttribute getLinkedProtocolConfiguration(AssetAttribute attribute) {
        AttributeRef protocolConfigRef = AgentLink.getAgentLink(attribute).orElseThrow(() -> new IllegalStateException("Attribute is not linked to a protocol"));
        return getLinkedProtocolConfiguration(protocolConfigRef);
    }

    protected AssetAttribute getLinkedProtocolConfiguration(AttributeRef protocolConfigurationRef) {
        synchronized (linkedProtocolConfigurations) {
            Triplet<AssetAttribute, Consumer<DeploymentStatus>, DeploymentStatus> assetAttributeConsumerInfo = linkedProtocolConfigurations.get(protocolConfigurationRef);
            // Don't bother with null check if someone calls here with an attribute not linked to this protocol
            // then they're doing something wrong so fail hard and fast
            return assetAttributeConsumerInfo.getValue1();
        }
    }

    /**
     * Send an arbitrary {@link AttributeState} through the processing chain
     * using the current system time as the timestamp.
     */
    protected void sendAttributeEvent(AttributeState state) {
        sendAttributeEvent(new AttributeEvent(state, timerService.getCurrentTimeMillis()));
    }

    /**
     * Send an arbitrary {@link AttributeEvent} through the processing chain.
     */
    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        LOG.info("Sending attribute event from protocol '" + getProtocolName() + "': " + attributeEvent);
        // TODO: Decide whether protocols should be allowed to send arbitrary attribute events to read only attributes
        // Use same mechanism as clients - cannot write to readonly attributes
        //assetProcessingService.sendAttributeEvent();
        // Use special route for protocols to allow write on readonly attributes
        producerTemplate.sendBodyAndHeader(SENSOR_QUEUE, attributeEvent, "isSensorUpdate", false);
    }

    /**
     * Update the value of a linked attribute.
     */
    protected void updateLinkedAttribute(AttributeState state, long timestamp) {
        AttributeEvent attributeEvent = new AttributeEvent(state, timestamp);
        LOG.fine("Sending on sensor queue: " + attributeEvent);
        producerTemplate.sendBodyAndHeader(SENSOR_QUEUE, attributeEvent, Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, getProtocolName());
    }

    /**
     * Update the value of a linked attribute.
     */
    protected void updateLinkedAttribute(AttributeState state) {
        updateLinkedAttribute(state, timerService.getCurrentTimeMillis());
    }

    /**
     * Update a linked protocol configuration; allows protocols to reconfigure their
     * own protocol configurations to persist changing data e.g. authorization tokens.
     */
    protected void updateLinkedProtocolConfiguration(AssetAttribute protocolConfiguration, HasUniqueResourceName metaName, MetaItem... newMetaItem) {
        updateLinkedProtocolConfiguration(protocolConfiguration, metaName.getUrn(), newMetaItem);
    }

    protected void updateLinkedProtocolConfiguration(AssetAttribute protocolConfiguration, String metaName, MetaItem... newMetaItem) {
        // Clone the protocol configuration rather than modify this one
        AssetAttribute modifiedProtocolConfiguration = protocolConfiguration.deepCopy();
        MetaItem.replaceMetaByName(modifiedProtocolConfiguration.getMeta(), metaName, Arrays.asList(newMetaItem));
        assetService.updateProtocolConfiguration(modifiedProtocolConfiguration);
    }

    /**
     * Update the deployment status of a protocol configuration by its attribute ref
     */
    protected void updateDeploymentStatus(AttributeRef protocolRef, DeploymentStatus deploymentStatus) {
        synchronized (linkedProtocolConfigurations) {
            linkedProtocolConfigurations.computeIfPresent(protocolRef,
                (ref, pair) -> {
                    pair.getValue2().accept(deploymentStatus);
                    pair.value3 = deploymentStatus;
                    return pair;
                }
            );
        }
    }

    /**
     * Gets the current deployment status of a protocol configuration.
     */
    protected DeploymentStatus getDeploymentStatus(AssetAttribute protocolConfiguration) {
        synchronized (linkedProtocolConfigurations) {
            Triplet<AssetAttribute, Consumer<DeploymentStatus>, DeploymentStatus> assetAttributeConsumerInfo = linkedProtocolConfigurations.get(protocolConfiguration.getReferenceOrThrow());
            return assetAttributeConsumerInfo.getValue3();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    /**
     * Link the protocol configuration.
     */
    abstract protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Unlink the protocol configuration.
     */
    abstract protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Link an attribute to its linked protocol configuration.
     */
    abstract protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration);

    /**
     * Unlink an attribute from its linked protocol configuration.
     */
    abstract protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration);

    /**
     * Attribute event (write) has been requested for an attribute linked to the specified
     * protocol configuration.
     */
    abstract protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration);
}
