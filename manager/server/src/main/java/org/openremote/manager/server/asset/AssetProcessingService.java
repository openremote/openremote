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
import org.openremote.model.Attribute;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeState;
import org.openremote.model.Consumer;
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
 * </li>
 * </ul>
 */
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetProcessingService.class.getName());

    protected AssetRulesService assetRulesService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;

    final protected List<Consumer<AssetUpdate>> processors = new ArrayList<>();

    @Override
    public void init(Container container) throws Exception {
        assetRulesService = container.getService(AssetRulesService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);

        processors.add(assetRulesService);
        processors.add(assetStorageService);
        processors.add(assetDatapointService);

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
            .filter(body().isInstanceOf(AttributeEvent.class))
            .process(exchange -> {
                AttributeEvent attributeEvent = exchange.getIn().getBody(AttributeEvent.class);

                // From sensor topic means this must update a thing asset
                ServerAsset thing = assetStorageService.find(attributeEvent.getEntityId());
                if (thing.getWellKnownType() != THING) {
                    LOG.fine("Ignoring " + attributeEvent + ", not a thing: " + thing);
                    return;
                }

                // Get the attribute and check it is actually linked to an agent (although the
                // event comes from a Protocol, we can not assume that the attribute is still linked,
                // consider a protocol that receives a batch of messages because a gateway was offline
                // for a day)
                ThingAttributes thingAttributes = new ThingAttributes(thing);
                ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
                    assetStorageService.getAgentLinkResolver(), attributeEvent.getAttributeName()
                );

                processAssetUpdate(thing, thingAttribute, attributeEvent, true);
            });
    }

    public void processAssetUpdate(ServerAsset asset,
                                   Attribute attribute,
                                   AttributeEvent attributeEvent) {
        processAssetUpdate(asset, attribute, attributeEvent, false);
    }

    protected void processAssetUpdate(ServerAsset asset,
                                      Attribute attribute,
                                      AttributeEvent attributeEvent,
                                      boolean ignoreReadOnly) {

        // Check the asset has this attribute (might also not be present if a Thing's attribute is unlinked)
        if (attribute == null) {
            LOG.fine("Ignoring " + attributeEvent + ", attribute not available: " + asset);
            return;
        }

        // Check attribute isn't readonly
        if (!ignoreReadOnly && attribute.isReadOnly()) {
            LOG.fine("Ignoring " + attributeEvent + ", attribute is read-only in: " + asset);
            return;
        }

        // Ensure timestamp of event is not in the future as that would essentially block access to
        // the attribute until after that time (maybe that is desirable behaviour)
        // Allow a leniency of 1s
        if (attributeEvent.getTimestamp() - System.currentTimeMillis() > 1000) {
            // TODO: Decide how to handle update events in the future - ignore or change timestamp
            LOG.fine("Ignoring " + attributeEvent + ", event-time is in the future in:" + asset);
            return;
        }

        // Hold on to existing attribute state so we can use it during processing
        AttributeEvent lastStateEvent = attribute.getStateEvent(asset.getId());

        // Check the last update timestamp of the attribute, ignoring any event that is older than last udpate
        // TODO: This means we drop out-of-sequence events, we might need better at-least-once handling
        if (lastStateEvent.getTimestamp() >= 0 && attributeEvent.getTimestamp() - lastStateEvent.getTimestamp() < 1000) {
            LOG.fine("Ignoring " + attributeEvent + ", event-time is older than attribute's last value in: " + asset);
            return;
        }

        // Set new value and event timestamp on attribute, thus validating any attribute constraints
        try {
            attribute.applyStateEvent(attributeEvent);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.FINE, "Ignoring " + attributeEvent + ", attribute constraint violation in: " + asset, ex);
            return;
        }

        processAssetUpdate(
            new AssetUpdate(asset, attribute, attributeEvent, lastStateEvent)
        );
    }

    protected void processAssetUpdate(AssetUpdate assetUpdate) {

        // Create and process the attributeState change
        for (Consumer<AssetUpdate> processor : processors) {
            try {
                LOG.fine("Processor " + processor + " accepts: " + assetUpdate);
                processor.accept(assetUpdate);
            } catch (Throwable t) {
                assetUpdate.setStatus(AssetUpdate.Status.ERROR);
                assetUpdate.setError(t);
            }

            switch (assetUpdate.getStatus()) {
                case HANDLED:
                    LOG.fine("Processor " + processor + " finally handled: " + assetUpdate);
                    break;
                case ERROR:
                    // TODO Better error handling, not sure we need rewind?
                    throw new RuntimeException("Processor " + processor + " error: " + assetUpdate, assetUpdate.getError());
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
