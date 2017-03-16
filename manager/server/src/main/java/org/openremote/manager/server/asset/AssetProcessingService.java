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
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.server.agent.ThingAttributes;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.manager.server.rules.AssetRulesService;
import org.openremote.model.*;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.ThingAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.Protocol.ACTUATOR_TOPIC;
import static org.openremote.agent3.protocol.Protocol.SENSOR_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.model.asset.AssetType.THING;

/**
 * Process asset data changes, routing asset-related messages.
 * <p>
 * The regular processor chain is:
 * <ul>
 * <li>{@link AssetRulesService}</li>
 * <li>{@link AssetStorageService}</li>
 * <li>{@link AssetDatapointService}</li>
 * </ul>
 * <p>
 * Listens for {@link AttributeEvent} messages on {@link Protocol#SENSOR_TOPIC} and processes them.
 * <p>
 * Listens for {@link PersistenceEvent} on {@link PersistenceEvent#PERSISTENCE_TOPIC} of {@link Asset}
 * entity and detects changed attributes. Any created or updated {@link ThingAttribute} value will be
 * send to its {@link Protocol#ACTUATOR_TOPIC}. Any other created or updated attribute will be processed.
 * TODO This is not implemented
 * <p>
 * Accepts client update calls on {@link #processClientUpdate(AttributeEvent)}. An update event for
 * a {@link ThingAttribute} will be send to its {@link Protocol#ACTUATOR_TOPIC}. An update event for
 * other attributes will be processed.
 * <p>
 * TODO What's the call path for thing attribute value updates from clients (northbound, not sensors)?
 * <p>
 * Proposal:
 * <p>
 * 1. Receive AttributeEvent on client API
 * <p>
 * 2. Handle security and verify the message is for a linked agent/thing/attribute in the asset database
 * <p>
 * 3. Send the AttributeEvent to the Protocol through ACTUATOR_TOPIC and trigger device/service call
 * <p>
 * 4a. We assume that most protocol implementations are not using fire-and-forget but request/reply
 * communication. Thus, an AttributeEvent has no further consequences besides triggering an
 * actuator. We expect that a sensor "response" will let us know "soon" if the call was successful.
 * Only a sensor value change will result in an update of the asset database state. The window of
 * inconsistency we can accept depends on how "soon" a protocol typically responds.
 * <p>
 * 4b. Alternatively, if the updated attribute is configured with the "forceUpdate" meta item, we write
 * the new attribute state directly into the asset database after triggering the actuator. This flag
 * is useful if the protocol does not reflect actuator changes "immediately". For example, if we
 * send a value to a light dimmer actuator, does the light dimmer also have a sensor that responds
 * quickly with the new value? If the device/service does not reply to value changes, we can force
 * an update of the "current state" in our database and simply assume that the actuator call was
 * successful.
 * <p>
 * 4c. Should "forceUpdate" be the default behavior?
 */
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetProcessingService.class.getName());

    protected AssetRulesService rulesService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected MessageBrokerService messageBrokerService;

    final protected List<Consumer<AssetUpdate>> processors = new ArrayList<>();

    @Override
    public void init(Container container) throws Exception {
        rulesService = container.getService(AssetRulesService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);

        processors.add(rulesService);
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
                processSensorUpdate(exchange.getIn().getBody(AttributeEvent.class));
            });

        // If any asset was modified in the database, detect changed attributes
        from(PERSISTENCE_TOPIC)
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Asset asset = (Asset) persistenceEvent.getEntity();
                // TODO: Detect which attribute was created/updated in the database and handle it
            });
    }

    public void processClientUpdate(AttributeEvent attributeEvent) {

        ServerAsset asset = assetStorageService.find(attributeEvent.getEntityId());

		if (asset == null) {
			LOG.warning("Processing client update failed asset not found: " + attributeEvent);
			return;
		}

        switch (asset.getWellKnownType()) {
            case THING:
                // Send it to the protocol actuator after checking if it's a thing asset and linked attribute
                LOG.fine("Processing client " + attributeEvent + " for thing: " + asset);

                ThingAttributes thingAttributes = new ThingAttributes(asset);
                ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
                    assetStorageService.getAgentLinkResolver(), attributeEvent.getAttributeName()
                );

                if (thingAttribute == null) {
                    throw new RuntimeException("Ignoring " + attributeEvent + ", thing attribute not available or linked to agent: " + asset);
                }

                // TODO See class Javadoc, should we have a forceUpdate flag?
                // processUpdate(asset, thingAttribute, attributeEvent, false);

                // Push it to protocol actuator
                messageBrokerService.getProducerTemplate().sendBody(ACTUATOR_TOPIC, attributeEvent);

                break;

            case AGENT:
                // TODO We don't want to allow agent attribute updates?! Not sure why but it seems like a good idea...
                throw new RuntimeException("Agent attributes can not be updated individually, update the whole asset instead");

            default:
                // Perform regular asset attribute update
                LOG.fine("Processing client " + attributeEvent + " for: " + asset);
                Attributes attributes = new Attributes(asset.getAttributes());
                Attribute attribute = attributes.get(attributeEvent.getAttributeName());
                processUpdate(asset, attribute, attributeEvent, false); // Don't ignore readOnly flag
                break;
        }
    }

    protected void processSensorUpdate(AttributeEvent attributeEvent) {
        // Must reference a thing asset
        ServerAsset thing = assetStorageService.find(attributeEvent.getEntityId());
        if (thing.getWellKnownType() != THING) {
            LOG.fine("Ignoring " + attributeEvent + ", not a thing: " + thing);
            return;
        }

        LOG.fine("Processing sensor " + attributeEvent + " for thing: " + thing);

        // Get the attribute and check it is actually linked to an agent (although the
        // event comes from a Protocol, we can not assume that the attribute is still linked,
        // consider a protocol that receives a batch of messages because a gateway was offline
        // for a day)
        ThingAttributes thingAttributes = new ThingAttributes(thing);
        ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
            assetStorageService.getAgentLinkResolver(), attributeEvent.getAttributeName()
        );

        processUpdate(thing, thingAttribute, attributeEvent, true);
    }

    protected void processUpdate(ServerAsset asset,
                                 Attribute attribute,
                                 AttributeEvent attributeEvent,
                                 boolean ignoreReadOnly) {

        // Check the asset has this attribute (might also not be present if a Thing's attribute is unlinked)
        if (attribute == null) {
            LOG.warning("Ignoring " + attributeEvent + ", attribute not available: " + asset);
            return;
        }

        // Check attribute isn't readonly
        if (!ignoreReadOnly && attribute.isReadOnly()) {
            LOG.warning("Ignoring " + attributeEvent + ", attribute is read-only in: " + asset);
            return;
        }

        // Ensure timestamp of event is not in the future as that would essentially block access to
        // the attribute until after that time (maybe that is desirable behaviour)
        // Allow a leniency of 1s
        if (attributeEvent.getTimestamp() - System.currentTimeMillis() > 1000) {
            // TODO: Decide how to handle update events in the future - ignore or change timestamp
            LOG.warning("Ignoring " + attributeEvent + ", event-time is in the future in:" + asset);
            return;
        }

        // Hold on to existing attribute state so we can use it during processing
        AttributeEvent lastStateEvent = attribute.getStateEvent(asset.getId());

        // Check the last update timestamp of the attribute, ignoring any event that is older than last update
        // TODO: This means we drop out-of-sequence events, we might need better at-least-once handling
        if (lastStateEvent.getTimestamp() >= 0 && attributeEvent.getTimestamp() < lastStateEvent.getTimestamp()) {
            LOG.warning("Ignoring " + attributeEvent + ", event-time is older than attribute's last state " + lastStateEvent + " in: " + asset);
            return;
        }

        // Set new value and event timestamp on attribute, thus validating any attribute constraints
        try {
            attribute.applyStateEvent(attributeEvent);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "Ignoring " + attributeEvent + ", attribute constraint violation in: " + asset, ex);
            return;
        }

        processUpdate(new AssetUpdate(asset, attribute, attributeEvent, lastStateEvent));
    }

    protected void processUpdate(AssetUpdate assetUpdate) {
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
