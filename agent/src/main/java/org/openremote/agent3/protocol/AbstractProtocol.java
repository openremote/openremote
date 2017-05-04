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
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.asset.AssetAttribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;
    protected TimerService timerService;
    protected ProtocolExecutorService executorService;

    @Override
    public void init(Container container) throws Exception {
        LOG.info("Initializing protocol: " + getProtocolName());
        timerService = container.getService(TimerService.class);
        executorService = container.getService(ProtocolExecutorService.class);
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
                            // TODO Use read/write lock for link/unlink attributes synchronization and additional optional exclusive lock for single-threaded implementors
                            synchronized (linkedAttributes) {
                                // TODO This could be optimized, we must avoid inspecting all messages (maybe tag with protocol name in header?)
                                if (isAttributeStateForAny(linkedAttributes.keySet()).matches(exchange)) {
                                    sendToActuator(exchange.getIn().getBody(AttributeEvent.class));
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
    public void linkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception {
        synchronized (linkedAttributes) {
            attributes.forEach(attribute -> {
                if (linkedAttributes.containsKey(attribute.getReferenceOrThrow())) {
                    LOG.fine("Attribute updated on '" + getProtocolName() + "': " + attribute);
                    onAttributeUpdated(attribute, protocolConfiguration);
                    linkedAttributes.put(attribute.getReferenceOrThrow(), attribute);
                } else {
                    LOG.fine("Attribute added on '" + getProtocolName() + "': " + attribute);
                    onAttributeAdded(attribute, protocolConfiguration);
                    linkedAttributes.put(attribute.getReferenceOrThrow(), attribute);
                }
            });
        }
    }

    @Override
    public void unlinkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception {
        synchronized (linkedAttributes) {
            attributes
                .stream()
                .filter(attribute -> linkedAttributes.values()
                    .stream()
                    .anyMatch(linkedAttribute ->
                        linkedAttribute
                            .getReferenceOrThrow()
                            .equals(attribute.getReferenceOrThrow()))
                )
                .forEach(attributeToRemove -> {
                    AttributeRef agentLinkedAttributeRef = attributeToRemove.getReferenceOrThrow();

                    linkedAttributes.remove(agentLinkedAttributeRef);
                    LOG.fine("Attribute removed on '" + getProtocolName() + "': " + attributeToRemove);
                    onAttributeRemoved(attributeToRemove, protocolConfiguration);
                });
        }
    }

    protected AssetAttribute getLinkedAttribute(AttributeRef attributeRef) {
        synchronized (linkedAttributes) {
            return linkedAttributes.get(attributeRef);
        }
    }

    protected void sendAttributeEvent(AttributeState state) {
        AttributeEvent event = new AttributeEvent(state, timerService.getCurrentTimeMillis());
        LOG.info("Sending on sensor queue: " + event);
        producerTemplate.sendBodyAndHeader(SENSOR_QUEUE, event, "isSensorUpdate", false);
    }

    protected void onSensorUpdate(AttributeState state, long timestamp) {
        AttributeEvent event = new AttributeEvent(state, timestamp);
        LOG.fine("Sending on sensor queue: " + event);
        producerTemplate.sendBody(SENSOR_QUEUE, event);
    }

    protected void onSensorUpdate(AttributeState state) {
        onSensorUpdate(state, timerService.getCurrentTimeMillis());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    abstract protected void sendToActuator(AttributeEvent event);

    abstract protected void onAttributeAdded(AssetAttribute attribute, AssetAttribute protocolConfiguration);

    abstract protected void onAttributeUpdated(AssetAttribute attribute, AssetAttribute protocolConfiguration);

    abstract protected void onAttributeRemoved(AssetAttribute attribute, AssetAttribute protocolConfiguration);
}
