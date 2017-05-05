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

import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.timer.TimerService;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractProtocol implements Protocol {

    private static final Logger LOG = Logger.getLogger(AbstractProtocol.class.getName());

    static Predicate isAttributeStateForAny(Collection<AttributeRef> attributeRefs) {
        return exchange -> {
            if (!(exchange.getIn().getBody() instanceof AttributeEvent))
                return false;
            AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
            return attributeRefs.contains(event.getAttributeRef());
        };
    }

    final protected Map<AttributeRef, AssetAttribute> linkedAttributes = new HashMap<>();
    final protected Map<AttributeRef, Pair<AssetAttribute, Consumer<DeploymentStatus>>> linkedProtocolConfigurations = new HashMap<>();
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
                            String protocolName = exchange.getIn().getHeader("Protocol", String.class);
                            if (getProtocolName().equals(protocolName)) {
                                // TODO Use read/write lock for link/unlink attributes synchronization and additional optional exclusive lock for single-threaded implementors
                                synchronized (linkedAttributes) {
                                    AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                                    AssetAttribute attribute = linkedAttributes.get(event.getAttributeRef());
                                    if (attribute == null) {
                                        LOG.warning("Attribute doesn't exist on this protocol: " + event.getAttributeRef());
                                    } else {
                                        processLinkedAttributeWrite(event, attribute);
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
                new Pair<>(protocolConfiguration, deploymentStatusConsumer)
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
                linkedAttributes.put(attribute.getReferenceOrThrow(), attribute);
                doLinkAttribute(attribute, protocolConfiguration);
            });
        }
    }

    @Override
    public void unlinkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception {
        synchronized (linkedAttributes) {
            attributes.forEach(attribute -> {
                LOG.fine("Unlinking attribute on '" + getProtocolName() + "': " + attribute);
                doUnlinkAttribute(attribute, protocolConfiguration);
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
     * Get the deployment status consumer for a given protocol configuration
     */
    protected Consumer<DeploymentStatus> getDeploymentStatusConsumer(AssetAttribute protocolConfiguration) {
        return getDeploymentStatusConsumer(protocolConfiguration.getReferenceOrThrow());
    }

    /**
     * Get the deployment status consumer for a given protocol configurations attribute ref
     */
    protected Consumer<DeploymentStatus> getDeploymentStatusConsumer(AttributeRef protocolRef) {
        synchronized (linkedProtocolConfigurations) {
            return linkedProtocolConfigurations.containsKey(protocolRef)
                ? linkedProtocolConfigurations.get(protocolRef).value
                : null;
        }
    }

    /**
     * Send an arbitrary {@link AttributeState} through the processing chain
     * using the current system time as the timestamp.
     */
    protected void sendAttributeEvent(AttributeState state) {
        AttributeEvent event = new AttributeEvent(state, timerService.getCurrentTimeMillis());
        LOG.info("Sending attribute event from protocol '" + getProtocolName() + "': " + event);
        assetService.sendAttributeEvent(event);
    }

    /**
     * Send an arbitrary {@link AttributeEvent} through the processing chain.
     */
    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        LOG.info("Sending attribute event: " + attributeEvent);
        assetService.sendAttributeEvent(attributeEvent);
    }

    /**
     * Update the value of a linked attribute.
     */
    protected void updateLinkedAttribute(AttributeState state, long timestamp) {
        AttributeEvent event = new AttributeEvent(state, timestamp);
        LOG.fine("Sending on sensor queue: " + event);
        producerTemplate.sendBody(SENSOR_QUEUE, event);
    }

    /**
     * Update the value of a linked attribute.
     */
    protected void updateLinkedAttribute(AttributeState state) {
        updateLinkedAttribute(state, timerService.getCurrentTimeMillis());
    }

    /**
     * Update the deployment status of a protocol configuration by its attribute ref
     */
    protected void updateDeploymentStatus(AttributeRef protocolRef, DeploymentStatus deploymentStatus) {
        synchronized (linkedProtocolConfigurations) {
            linkedProtocolConfigurations.computeIfPresent(protocolRef,
                (ref, pair) -> {
                    pair.value.accept(deploymentStatus);
                    return pair;
                }
            );
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
