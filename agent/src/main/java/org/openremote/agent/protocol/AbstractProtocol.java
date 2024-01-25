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
import org.apache.camel.impl.DefaultCamelContext;
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
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.util.Pair;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.openremote.model.protocol.ProtocolUtil.hasDynamicWriteValue;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractProtocol<T extends Agent<T, ?, U>, U extends AgentLink<?>> implements Protocol<T> {

    private static final System.Logger LOG = System.getLogger(AbstractProtocol.class.getSimpleName() + "." + PROTOCOL.name());
    protected final Map<AttributeRef, Attribute<?>> linkedAttributes = new ConcurrentHashMap<>();
    protected final Set<AttributeRef> dynamicAttributes = Collections.synchronizedSet(new HashSet<>());
    protected DefaultCamelContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected ProtocolAssetService assetService;
    protected ProtocolPredictedDatapointService predictedDatapointService;
    protected ProtocolDatapointService datapointService;
    protected T agent;
    protected final Object processorLock = new Object();

    public AbstractProtocol(T agent) {
        this.agent = agent;
    }

    @Override
    public void setAssetService(ProtocolAssetService assetService) {
        this.assetService = assetService;
    }

    @Override
    public void start(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        executorService = container.getExecutorService();
        predictedDatapointService = container.getService(ProtocolPredictedDatapointService.class);
        datapointService = container.getService(ProtocolDatapointService.class);
        messageBrokerContext = container.getService(MessageBrokerService.class).getContext();
        this.producerTemplate = container.getService(MessageBrokerService.class).getProducerTemplate();
        doStart(container);
    }

    @Override
    final public void stop(Container container) {
        linkedAttributes.clear();
        try {
            messageBrokerContext.stopRoute("Actuator-" + getProtocolName(), 1, TimeUnit.MILLISECONDS);
            messageBrokerContext.removeRoute("Actuator-" + getProtocolName());

            doStop(container);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void setConnectionStatus(ConnectionStatus connectionStatus) {
        sendAttributeEvent(new AttributeEvent(getAgent().getId(), Agent.STATUS, connectionStatus));
    }

    @Override
    final public void linkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

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
    }

    @Override
    final public void unlinkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        if (linkedAttributes.remove(attributeRef) != null) {
            dynamicAttributes.remove(attributeRef);
            doUnlinkAttribute(assetId, attribute, agent.getAgentLink(attribute));
        }
    }

    public T getAgent() {
        return this.agent;
    }

    @Override
    public Map<AttributeRef, Attribute<?>> getLinkedAttributes() {
        return linkedAttributes;
    }

    @Override
    public void processLinkedAttributeWrite(AttributeEvent event) {
        synchronized (processorLock) {
            LOG.log(System.Logger.Level.TRACE, () -> "Processing linked attribute write on protocol '" + this + "': " + event);
            AgentLink<?> agentLink = agent.getAgentLink(event);

            Pair<Boolean, Object> ignoreAndConverted = ProtocolUtil.doOutboundValueProcessing(
                event.getId(),
                event,
                agentLink,
                event.getValue().orElse(null),
                dynamicAttributes.contains(event.getRef()));

            if (ignoreAndConverted.key) {
                LOG.log(System.Logger.Level.DEBUG, "Value conversion returned ignore so attribute will not write to protocol: " + event.getRef());
                return;
            }

            doLinkedAttributeWrite(agent.getAgentLink(event), event, ignoreAndConverted.value);

            if (agent.isUpdateOnWrite().orElse(false) || agentLink.getUpdateOnWrite().orElse(false)) {
                updateLinkedAttribute(new AttributeState(event.getRef(), ignoreAndConverted.value));
            }
        }
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
        // Don't allow updating linked attributes with this mechanism as it could cause an infinite loop
        if (linkedAttributes.containsKey(event.getRef())) {
            LOG.log(System.Logger.Level.WARNING, () -> "Cannot update an attribute linked to the same protocol; use updateLinkedAttribute for that: " + event);
            return;
        }
        assetService.sendAttributeEvent(event);
    }


    @Override
    final public void updateLinkedAttribute(final AttributeState state, long timestamp) {
        Attribute<?> attribute = linkedAttributes.get(state.getRef());

        if (attribute == null) {
            LOG.log(System.Logger.Level.WARNING, () -> "Update linked attribute called for un-linked attribute: " + state);
            return;
        }

        Pair<Boolean, Object> ignoreAndConverted = ProtocolUtil.doInboundValueProcessing(state.getRef().getId(), attribute, agent.getAgentLink(attribute), state.getValue().orElse(null));

        if (ignoreAndConverted.key) {
            LOG.log(System.Logger.Level.DEBUG, "Value conversion returned ignore so attribute will not be updated: " + state.getRef());
            return;
        }

        AttributeEvent attributeEvent = new AttributeEvent(new AttributeState(state.getRef(), ignoreAndConverted.value), timestamp);
        LOG.log(System.Logger.Level.TRACE, () -> "Sending linked attribute update: " + attributeEvent);
        assetService.sendAttributeEvent(attributeEvent);
    }

    @Override
    final public void updateLinkedAttribute(final AttributeState state) {
        updateLinkedAttribute(state, timerService.getCurrentTimeMillis());
    }

    @Override
    public boolean onAgentAttributeChanged(AttributeEvent event) {
        // If event is for an agent attribute then we can try and handle it here in a generic way
        Agent<?,?,?> agent = getAgent();
        return agent.isConfigurationAttribute(event.getName());
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
    abstract protected void doLinkedAttributeWrite(U agentLink, AttributeEvent event, Object processedValue);
}
