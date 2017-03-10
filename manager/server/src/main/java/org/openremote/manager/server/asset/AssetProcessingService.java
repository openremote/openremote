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
package org.openremote.manager.server.asset;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.manager.server.agent.ThingAttributes;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.manager.server.rules.AssetRulesService;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;
import org.openremote.model.AttributeStateChange;
import org.openremote.model.Consumer;
import org.openremote.model.asset.AssetStateChange;
import org.openremote.model.asset.ThingAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.Protocol.SENSOR_TOPIC;
import static org.openremote.model.asset.AssetType.THING;

/**
 * Process asset data changes, routing asset-related messages.
 * <ul>
 * <li>
 * Listens to {@link AttributeState} messages on {@link Protocol#SENSOR_TOPIC} and processes them
 * with consumers: {@link AssetRulesService}, {@link AssetStorageService}, {@link AssetDatapointService}
 * TODO Make consumers Camel endpoints?
 * </li>
 * </ul>
 * TODO Move AssetListenerSubscriptions from AssetStorageService, when we figured out client messaging pub/sub
 */
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetProcessingService.class.getName());

    protected AssetRulesService assetRulesService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    final protected List<Consumer<AssetStateChange<ServerAsset>>> attributeStateChangeConsumers = new ArrayList<>();


    @Override
    public void init(Container container) throws Exception {
        assetRulesService = container.getService(AssetRulesService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);

        attributeStateChangeConsumers.add(assetRulesService);
        attributeStateChangeConsumers.add(assetStorageService);
        attributeStateChangeConsumers.add(assetDatapointService);

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

        // Sensor updates are processed first by rules, then stored in the asset and datapoint databases
        from(SENSOR_TOPIC)
            .filter(body().isInstanceOf(AttributeState.class))
            .process(exchange -> {
                AttributeState attributeState =
                    exchange.getIn().getBody(AttributeState.class);

                // From sensor topic means this is a thing asset so we can get the thing
                ServerAsset thing = assetStorageService.find(attributeState.getAttributeRef().getEntityId());

                // If it's not a thing, that's wrong
                if (thing.getWellKnownType() != THING) {
                    LOG.fine("Ignoring update of " + attributeState + ", not a thing: " + thing);
                    return;
                }

                // Get the attribute
                ThingAttributes thingAttributes = new ThingAttributes(thing);
                ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
                    assetStorageService.getAgentLinkResolver(), attributeState.getAttributeRef().getAttributeName()
                );
                if (thingAttribute == null) {
                    LOG.fine("Ignoring update of " + attributeState + ", linked attribute not found in: " + thing);
                    return;
                }

                // Hold on to original attribute state so we can use it during processing
                AttributeState originalState = new AttributeState(
                    new AttributeRef(thing.getId(), thingAttribute.getName()), thingAttribute.getValue()
                );

                // Set new value on attribute, thus validating the value type against the attribute type
                try {
                    thingAttribute.setValue(attributeState.getValue());
                } catch (IllegalArgumentException ex) {
                    LOG.log(Level.FINE, "Ignoring update of " + attributeState + ", attribute constraint violations in: " + thing, ex);
                    return;
                }

                // Create and process the state change
                AssetStateChange<ServerAsset> stateChange = new AssetStateChange<>(thing, thingAttribute, originalState);

                for (Consumer<AssetStateChange<ServerAsset>> consumer : attributeStateChangeConsumers) {
                    try {
                        LOG.fine("Consumer " + consumer + " should process: " + stateChange);
                        consumer.accept(stateChange);
                        LOG.fine("Consumer " + consumer + " completed processing: " + stateChange);
                    } catch (Throwable t) {
                        // All exceptions during processing should end up here
                        // TODO Better error handling, not sure we need rewind?
                        throw new RuntimeException("Consumer " + consumer + " processing error: " + stateChange, t);
                    }

                    if (stateChange.getProcessingStatus() == AttributeStateChange.Status.HANDLED) {
                        LOG.fine("Consumer " + consumer + " finally handled: " + stateChange);
                        break;
                    }
                }
            });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
