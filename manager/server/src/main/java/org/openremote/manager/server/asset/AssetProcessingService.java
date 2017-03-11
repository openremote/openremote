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

import elemental.json.Json;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.manager.server.agent.ThingAttributes;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.manager.server.rules.AssetRulesService;
import org.openremote.manager.shared.event.asset.AttributeStateUpdateEvent;
import org.openremote.model.*;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetStateChange;
import org.openremote.model.asset.ThingAttribute;

import java.util.ArrayList;
import java.util.List;
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

                // TODO: Agent should be able to create AttributeStateUpdateEvent with desired timestamp
                AttributeStateUpdateEvent attributeStateUpdateEvent = new AttributeStateUpdateEvent(attributeState);

                // From sensor topic means this is a thing asset so we can get the thing
                ServerAsset thing = assetStorageService.find(attributeState.getAttributeRef().getEntityId());

                // If it's not a thing, that's wrong
                if (thing.getWellKnownType() != THING) {
                    LOG.fine("Ignoring update of " + attributeState + ", not a thing: " + thing);
                    return;
                }

                // Get the attribute and check it is actually linked to an agent
                // TODO: Do we really need to perform this verification it's the protocol that brought us here anyway?
                ThingAttributes thingAttributes = new ThingAttributes(thing);
                ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
                    assetStorageService.getAgentLinkResolver(), attributeState.getAttributeRef().getAttributeName()
                );
                if (thingAttribute == null) {
                    LOG.fine("Ignoring update of " + attributeState + ", linked attribute not found in: " + thing);
                    return;
                }

                processAttributeStateUpdate(attributeStateUpdateEvent, true);
            });
    }

    // TODO: This should be used by client, rules and protocols as the single point of asset attribute state update
    // TODO: How to handle read only attribute updates (if from the protocol then it should be allowed)
    void processAttributeStateUpdate(AttributeStateUpdateEvent attributeStateUpdateEvent, boolean ignoreReadOnlyFlag) {
        AttributeState attributeState = attributeStateUpdateEvent.getAttributeState();
        ServerAsset asset = assetStorageService.find(attributeState.getAttributeRef().getEntityId());
        Attributes attributes = new Attributes(asset.getAttributes());
        String attributeName = attributeState.getAttributeRef().getAttributeName();
        Attribute attribute = attributes.get(attributeName);

        // Check the asset has this attribute
        if (attribute == null) {
            LOG.fine("Ignoring update of " + attributeState + " as asset doesn't have an attribute by this name");
            return;
        }

        // Check attribute isn't readonly
        if (!ignoreReadOnlyFlag && attribute.firstMetaItem(AssetMeta.READ_ONLY) != null) {
            LOG.fine("Ignoring update of " + attributeState + " as asset attribute is readonly");
            return;
        }

        // Hold on to original attributeState so we can use it during processing
        AttributeState originalState = new AttributeState(
                new AttributeRef(asset.getId(), attribute.getName()), attribute.getValue()
        );

        // Set new value on attribute, thus validating the value type against the attribute type
        try {
            attribute.setValue(attributeState.getValue());
        } catch (IllegalArgumentException ex) {
            LOG.fine("Ignoring update of " + attributeState + ", not of expected value type '" + attribute.getType() + "' in: " + asset);
            return;
        }

        // Ensure timestamp of event is not in the future as that would essentially block access to
        // the attribute until after that time (maybe that is desirable behaviour)
        // Allow a leniency of 1s
        if (attributeStateUpdateEvent.getTimestamp() - System.currentTimeMillis() > 1000) {
            // TODO: Decide how to handle update events in the future - ignore or change timestamp
            LOG.fine("Ignoring update of " + attributeState + ", update event timestamp is in the future");
            return;
        }

        // Check the last update timestamp meta on the attribute and store update timestamp
        Meta meta = attribute.getMeta();
        MetaItem lastUpdateMeta = meta.first(AssetMeta.VALUE_TIMESTAMP);
        if (lastUpdateMeta != null) {
            // Can't use int as timestamp will overflow
            Double lastUpdate = lastUpdateMeta.getValueAsDecimal();
            if (lastUpdate != null) {
                // Check last update is before this update
                if (attributeStateUpdateEvent.getTimestamp() - lastUpdate < 1000) {
                    LOG.fine("Ignoring update of " + attributeState + ", update event timestamp is before the attributes last update timestamp");
                    return;
                }
            }
            lastUpdateMeta.setValue(Json.create(attributeStateUpdateEvent.getTimestamp()));
        }
        attribute.setMeta(meta);

        // Create and process the attributeState change
        AssetStateChange<ServerAsset> stateChange = new AssetStateChange<>(asset, attribute, originalState);

        for (Consumer<AssetStateChange<ServerAsset>> consumer : attributeStateChangeConsumers) {
            try {
                LOG.fine("Consumer '" + consumer + "' should process: " + stateChange);
                consumer.accept(stateChange);
                LOG.fine("Consumer '" + consumer + "' completed processing: " + stateChange);
            } catch (Throwable t) {
                // All exceptions during processing should end up here
                // TODO Better error handling, not sure we need rewind?
                throw new RuntimeException("Consumer '" + consumer + "' processing error: " + stateChange, t);
            }

            if (stateChange.getProcessingStatus() == AttributeStateChange.Status.HANDLED) {
                LOG.fine("Consumer '" + consumer + "' finally handled: " + stateChange);
                break;
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
