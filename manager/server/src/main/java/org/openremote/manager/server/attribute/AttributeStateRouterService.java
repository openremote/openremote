/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.server.attribute;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.manager.server.agent.ThingAttributes;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.asset.datapoint.DatapointService;
import org.openremote.manager.server.rules.RulesService;
import org.openremote.model.AttributeState;
import org.openremote.model.asset.ThingAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.Protocol.SENSOR_TOPIC;

/**
 * This class is responsible for configuring the routing of attribute state messages.
 *
 * Long term maybe AttributeStateChangeConsumers should be camel components and then use camel routes for processing chain
 */
public class AttributeStateRouterService extends RouteBuilder implements ContainerService {
    private static final Logger LOG = Logger.getLogger(AttributeStateRouterService.class.getName());
    protected RulesService rulesService;
    protected AssetService assetService;
    protected DatapointService datapointService;
    protected List<AttributeStateChangeConsumer> consumers = new ArrayList<>();

    @Override
    public void init(Container container) throws Exception {
        rulesService = container.getService(RulesService.class);
        assetService = container.getService(AssetService.class);
        datapointService = container.getService(DatapointService.class);
        consumers.add(rulesService);
        consumers.add(assetService);
        consumers.add(datapointService);

        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

        // Protocols send Attribute State messages on the Sensor Topic so convert it to an AttributeStateChange
        // object and pass it through to the required consumers sequentially with each returning a state consumer result
        from(SENSOR_TOPIC)
                .filter(body().isInstanceOf(AttributeState.class))
                .process(exchange -> {
                    AttributeState attributeState =
                            exchange.getIn().getBody(AttributeState.class);

                    // From sensor topic means this is a thing asset so we can get the thing
                    ServerAsset thing = assetService.find(attributeState.getAttributeRef().getEntityId());

                    // Get the attribute
                    ThingAttributes thingAttributes = new ThingAttributes(thing);
                    ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
                            assetService.getAgentLinkResolver(), attributeState.getAttributeRef().getAttributeName()
                    );

                    if (thingAttribute == null) {
                        LOG.fine("Ignoring attribute state change '" + attributeState + "' for unknown/unlinked attribute: " + thing);
                        return;
                    }

                    // Validate the value type against the attribute type
                    if (thingAttribute.getType().getJsonType() != attributeState.getValue().getType()) {
                        LOG.fine("Ignoring attribute state change '" + attributeState + "', wrong value type '" + attributeState.getValue().getType() + "': " + thing);
                        return;
                    }

                    AttributeStateChange stateChange = new AttributeStateChange(
                            thing,
                            thingAttribute,
                            thingAttribute.getValue_TODO_BUG_IN_JAVASCRIPT(),
                            attributeState.getValue());

                    for (int i=0; i<consumers.size(); i++) {
                        AttributeStateConsumerResult result = consumers.get(i).consumeAttributeStateChange(stateChange);
                        if(result == null) {
                            // Assume ok if no result returned
                            result = AttributeStateConsumerResult.OK;
                        }
                        switch (result) {
                            case OK:
                                // Check handled status of change
                                if (stateChange.isHandled()) {
                                    // Don't pass this change to any more consumers
                                    LOG.finest("Attribute state change consumer returned abort result so not passing state change onto any more consumers");
                                    return;
                                }
                                break;
                            default:
                                // TODO: maybe rewind the state change and run through processed consumers in reverse order?
                                LOG.info("Attribute state change consumer returned ERROR result so abandoning further processing of state change");
                                throw new RuntimeException("Attribute state change consumer return ERROR result");
                        }
                    }
                });
    }
}
