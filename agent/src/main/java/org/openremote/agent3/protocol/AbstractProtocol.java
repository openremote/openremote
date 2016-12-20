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
import org.openremote.model.asset.ThingAttribute;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeValueChange;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractProtocol implements Protocol {

    private static final Logger LOG = Logger.getLogger(AbstractProtocol.class.getName());

    static Predicate isAttributeValueChangeIn(Collection<AttributeRef> attributeRefs) {
        return exchange -> {
            if (!(exchange.getIn().getBody() instanceof AttributeValueChange))
                return false;
            AttributeValueChange valueChange = exchange.getIn().getBody(AttributeValueChange.class);
            return attributeRefs.contains(valueChange.getAttributeRef());
        };
    }

    final protected Map<AttributeRef, ThingAttribute> linkedAttributes = new HashMap<>();
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;

    @Override
    public void init(Container container) throws Exception {
        LOG.info("Initializing protocol: " + getProtocolName());
    }

    @Override
    public void configure(Container container) throws Exception {
        this.messageBrokerContext = container.getService(MessageBrokerService.class).getContext();
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
                            synchronized (linkedAttributes) {
                                // TODO This could be optimized, we must avoid inspecting all messages (maybe tag with protocol name in header?)
                                if (isAttributeValueChangeIn(linkedAttributes.keySet()).matches(exchange)) {
                                    sendToActuator(exchange.getIn().getBody(AttributeValueChange.class));
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
                if (linkedAttributes.containsKey(attribute.getAttributeRef())) {
                    LOG.fine("Attribute updated on '" + getProtocolName() + "': " + attribute);
                    onAttributeUpdated(attribute);
                    linkedAttributes.put(attribute.getAttributeRef(), attribute);
                } else {
                    LOG.fine("Attribute added on '" + getProtocolName() + "': " + attribute);
                    onAttributeAdded(attribute);
                    linkedAttributes.put(attribute.getAttributeRef(), attribute);
                }
            }
        }
    }

    @Override
    public void unlinkAttributes(String entityId) throws Exception {
        synchronized (linkedAttributes) {
            Iterator<Map.Entry<AttributeRef, ThingAttribute>> entryIterator = linkedAttributes.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<AttributeRef, ThingAttribute> entry = entryIterator.next();
                if (entry.getKey().getEntityId().equals(entityId)) {
                    LOG.fine("Attribute moved on '" + getProtocolName() + "': " + entry.getValue());
                    onAttributeRemoved(entry.getValue());
                    entryIterator.remove();
                }
            }
        }
    }

    protected ThingAttribute getLinkedAttribute(AttributeRef attributeRef) {
        synchronized (linkedAttributes) {
            return linkedAttributes.get(attributeRef);
        }
    }

    protected void onSensorUpdate(AttributeValueChange attributeValueChange) {
        producerTemplate.sendBody(SENSOR_TOPIC, attributeValueChange);
    }

    abstract protected void sendToActuator(AttributeValueChange attributeValueChange);

    abstract protected void onAttributeAdded(ThingAttribute attribute);

    abstract protected void onAttributeUpdated(ThingAttribute attribute);

    abstract protected void onAttributeRemoved(ThingAttribute attribute);

}
