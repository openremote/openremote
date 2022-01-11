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
import org.openremote.container.concurrent.GlobalLock;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.value.MetaItemType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.model.protocol.ProtocolUtil.hasDynamicWriteValue;
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
public abstract class AbstractProtocol<T extends Agent<T, ?, U>, U extends AgentLink<?>> implements Protocol<T> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractProtocol.class);
    protected final Map<AttributeRef, Attribute<?>> linkedAttributes = new HashMap<>();
    protected final Set<AttributeRef> dynamicAttributes = new HashSet<>();
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected ProtocolAssetService assetService;
    protected ProtocolPredictedAssetService predictedAssetService;
    protected T agent;

    public AbstractProtocol(T agent) {
        this.agent = agent;
    }

    @Override
    public void start(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        executorService = container.getExecutorService();
        assetService = container.getService(ProtocolAssetService.class);
        predictedAssetService = container.getService(ProtocolPredictedAssetService.class);
        messageBrokerContext = container.getService(MessageBrokerService.class).getContext();

        withLock(getProtocolName() + "::start", () -> {
            try {
                messageBrokerContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from(ACTUATOR_TOPIC)
                            .routeId("Actuator-" + getProtocolName() + getAgent().getId())
                            .process(exchange -> {
                                Protocol<?> protocolInstance = exchange.getIn().getHeader(ACTUATOR_TOPIC_TARGET_PROTOCOL, Protocol.class);
                                if (protocolInstance != AbstractProtocol.this) {
                                    return;
                                }

                                AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                                Attribute<?> linkedAttribute = getLinkedAttributes().get(event.getAttributeRef());

                                if (linkedAttribute == null) {
                                    LOG.info("Attempt to write to attribute that is not actually linked to this protocol '" + AbstractProtocol.this + "': " + linkedAttribute);
                                    return;
                                }

                                processLinkedAttributeWrite(event);
                            });
                    }
                });

                doStart(container);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
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

    protected void setConnectionStatus(ConnectionStatus connectionStatus) {
        sendAttributeEvent(new AttributeEvent(getAgent().getId(), Agent.STATUS, connectionStatus));
    }

    @Override
    final public void linkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        withLock(getProtocolName() + "::linkAttribute", () -> {

            AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

            if (linkedAttributes.containsKey(attributeRef)) {
                LOG.warning("Attribute is already linked to this protocol so ignoring: " + attributeRef);
                return;
            }

            // Need to add to map before actual linking as protocols may want to update the value as part of
            // linking process and without entry in the map any update would be blocked
            linkedAttributes.put(attributeRef, attribute);

            // Check for dynamic value placeholder

            if (hasDynamicWriteValue(agent.getAgentLink(attribute))) {
                dynamicAttributes.add(attributeRef);
            }

            try {
                doLinkAttribute(assetId, attribute, agent.getAgentLink(attribute));
            } catch (Exception e) {
                linkedAttributes.remove(attributeRef);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    final public void unlinkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        withLock(getProtocolName() + "::unlinkAttributes", () -> {
            AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

            if (linkedAttributes.remove(attributeRef) != null) {
                dynamicAttributes.remove(attributeRef);
                doUnlinkAttribute(assetId, attribute, agent.getAgentLink(attribute));
            }
        });
    }

    public T getAgent() {
        return this.agent;
    }

    @Override
    public Map<AttributeRef, Attribute<?>> getLinkedAttributes() {
        return linkedAttributes;
    }

    final protected void processLinkedAttributeWrite(AttributeEvent event) {
        LOG.finest("Processing linked attribute write on protocol '" + this + "': " + event);
        withLock(getProtocolName() + "::processLinkedAttributeWrite", () -> {

            Attribute<?> attribute = linkedAttributes.get(event.getAttributeRef());

            if (attribute == null) {
                LOG.warning("Attribute not linked to protocol '" + this + "':" + event);
            } else {

                AgentLink<?> agentLink = agent.getAgentLink(attribute);

                Pair<Boolean, Object> ignoreAndConverted = ProtocolUtil.doOutboundValueProcessing(
                    event.getAssetId(),
                    attribute,
                    agentLink,
                    event.getValue().orElse(null),
                    dynamicAttributes.contains(event.getAttributeRef()));

                if (ignoreAndConverted.key) {
                    LOG.fine("Value conversion returned ignore so attribute will not write to protocol: " + event.getAttributeRef());
                    return;
                }

                doLinkedAttributeWrite(attribute, agent.getAgentLink(attribute), event, ignoreAndConverted.value);

                if (agent.isUpdateOnWrite().orElse(false) || agentLink.getUpdateOnWrite().orElse(false)) {
                    updateLinkedAttribute(new AttributeState(event.getAttributeRef(), ignoreAndConverted.value));
                }
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
     * {@link ProtocolUtil#doInboundValueProcessing} before sending on the sensor queue.
     */
    final protected void updateLinkedAttribute(final AttributeState state, long timestamp) {
        Attribute<?> attribute = linkedAttributes.get(state.getRef());

        if (attribute == null) {
            LOG.severe("Update linked attribute called for un-linked attribute: " + state);
            return;
        }

        Pair<Boolean, Object> ignoreAndConverted = ProtocolUtil.doInboundValueProcessing(state.getRef().getId(), attribute, agent.getAgentLink(attribute), state.getValue().orElse(null));

        if (ignoreAndConverted.key) {
            LOG.fine("Value conversion returned ignore so attribute will not be updated: " + state.getRef());
            return;
        }

        AttributeEvent attributeEvent = new AttributeEvent(new AttributeState(state.getRef(), ignoreAndConverted.value), timestamp);
        LOG.finer("Sending linked attribute update on sensor queue: " + attributeEvent);
        producerTemplate.sendBodyAndHeader(SENSOR_QUEUE, attributeEvent, Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, getProtocolName());
    }

    /**
     * Update the value of one of this {@link Protocol}s linked {@link Agent}'s {@link Attribute}s.
     */
    final protected void updateAgentAttribute(final AttributeState state) {
        if (!agent.getAttributes().has(state.getRef().getName()) || !agent.getId().equals(state.getRef().getId())) {
            LOG.warning("Attempt to update non existent agent attribute or agent ID is incorrect: " + state);
            return;
        }
        AttributeEvent attributeEvent = new AttributeEvent(state, timerService.getCurrentTimeMillis());
        LOG.finer("Sending protocol agent attribute update: " + attributeEvent);
        assetService.sendAttributeEvent(attributeEvent);
    }

    /**
     * Update the value of a linked attribute, with the current system time as event time see
     * {@link #updateLinkedAttribute(AttributeState, long)} for more details.
     */
    final protected void updateLinkedAttribute(AttributeState state) {
        updateLinkedAttribute(state, timerService.getCurrentTimeMillis());
    }

    /**
     * Start this protocol instance
     */
    protected abstract void doStart(Container container) throws Exception;

    /**
     * Stop this protocol instance
     */
    protected abstract void doStop(Container container) throws Exception;

    @Override
    public String toString() {
        return getProtocolName() + "[" + getProtocolInstanceUri() + "]";
    }

    /**
     * Link an {@link Attribute} to its linked {@link Agent}.
     */
    abstract protected void doLinkAttribute(String assetId, Attribute<?> attribute, U agentLink) throws RuntimeException;

    /**
     * Unlink an {@link Attribute} from its linked {@link Agent}.
     */
    abstract protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, U agentLink);

    /**
     * An Attribute event (write) has been requested for an attribute linked to this protocol. The
     * processedValue is the resulting value after applying standard outbound value processing
     * (see {@link ProtocolUtil#doOutboundValueProcessing}). Protocol implementations should generally use the
     * processedValue but may also choose to use the original value for some purpose if required.
     */
    abstract protected void doLinkedAttributeWrite(Attribute<?> attribute, U agentLink, AttributeEvent event, Object processedValue);
}
