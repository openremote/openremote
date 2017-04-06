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
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;
import org.openremote.model.asset.thing.ThingAttribute;

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

    final protected Map<AttributeRef, ThingAttribute> linkedAttributes = new HashMap<>();
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;

    @Override
    public void init(Container container) throws Exception {
        LOG.info("Initializing protocol: " + getProtocolName());
        this.messageBrokerContext = container.getService(MessageBrokerSetupService.class).getContext();
        this.producerTemplate = messageBrokerContext.createProducerTemplate();
    }

    @Override
    public void start(Container container) throws Exception {
        synchronized (linkedAttributes) {
            LOG.fine("Starting: " + getProtocolName());
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
    public void linkAttributes(Collection<ThingAttribute> attributes) throws Exception {
        synchronized (linkedAttributes) {
            for (ThingAttribute attribute : attributes) {
                if (linkedAttributes.containsKey(attribute.getReference())) {
                    LOG.fine("Attribute updated on '" + getProtocolName() + "': " + attribute);
                    onAttributeUpdated(attribute);
                    linkedAttributes.put(attribute.getReference(), attribute);
                } else {
                    LOG.fine("Attribute added on '" + getProtocolName() + "': " + attribute);
                    onAttributeAdded(attribute);
                    linkedAttributes.put(attribute.getReference(), attribute);
                }
            }
        }
    }

    @Override
    public void unlinkAttributes(Collection<ThingAttribute> attributes) throws Exception {
        synchronized (linkedAttributes) {
            attributes
                    .stream()
                    .filter(thingAttribute -> linkedAttributes.values()
                                    .stream()
                                    .anyMatch(linkedAttribute -> linkedAttribute.getReference().equals(thingAttribute.getReference()))
                    )
                    .forEach(attributeToRemove -> {
                        linkedAttributes.remove(attributeToRemove.getReference());
                        LOG.fine("Attribute removed on '" + getProtocolName() + "': " + attributeToRemove);
                        onAttributeRemoved(attributeToRemove);
                    });
        }
    }

    protected ThingAttribute getLinkedAttribute(AttributeRef attributeRef) {
        synchronized (linkedAttributes) {
            return linkedAttributes.get(attributeRef);
        }
    }

    protected void sendAttributeUpdate(AttributeState state) {
        producerTemplate.sendBodyAndHeader(SENSOR_TOPIC, new AttributeEvent(state, System.currentTimeMillis()), "isSensorUpdate", false);
    }

    protected void onSensorUpdate(AttributeState state, long timestamp) {
        producerTemplate.sendBody(SENSOR_TOPIC, new AttributeEvent(state, timestamp));
    }

    protected void onSensorUpdate(AttributeState state) {
        onSensorUpdate(state, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    abstract protected void sendToActuator(AttributeEvent event);

    abstract protected void onAttributeAdded(ThingAttribute attribute);

    abstract protected void onAttributeUpdated(ThingAttribute attribute);

    abstract protected void onAttributeRemoved(ThingAttribute attribute);

}
