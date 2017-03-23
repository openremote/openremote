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
package org.openremote.manager.server.agent;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent3.protocol.AbstractProtocol;
import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.AssetUpdate;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.model.asset.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.Protocol.ACTUATOR_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForAssetType;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.AssetType.THING;

/**
 * Finds all {@link AssetType#AGENT} and {@link AssetType#THING} assets and starts the protocols for them.
 */
public class AgentService extends RouteBuilder implements ContainerService, Consumer<AssetUpdate> {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected Container container;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected MessageBrokerService messageBrokerService;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {
        Asset[] agents = assetStorageService.findByType(AGENT.getValue());
        LOG.fine("Deploy all agents in all realms: " + agents.length);
        for (Asset agent : agents) {
            deployAgent(agent, PersistenceEvent.Cause.UPDATE);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public void configure() throws Exception {

        // If any agent or thing was modified in the database, deploy the changes
        from(PERSISTENCE_TOPIC)
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Asset asset = (Asset) persistenceEvent.getEntity();
                if (isPersistenceEventForAssetType(AGENT).matches(exchange)) {
                    deployAgent(asset, persistenceEvent.getCause());
                } else if (isPersistenceEventForAssetType(THING).matches(exchange)) {
                    deployThing(asset, persistenceEvent.getCause());
                }
            });
    }

    protected void deployAgent(Asset agent, PersistenceEvent.Cause cause) {
        LOG.fine("Deploy agent: " + agent);
        switch (cause) {
            case UPDATE:
                // TODO: Find everything with this agent ID in the asset tree not just children
                Asset[] things = assetStorageService.findChildrenByType(agent.getId(), THING);
                for (Asset thing : things) {
                    deployThing(thing, PersistenceEvent.Cause.UPDATE);
                }
                break;
            case DELETE:
                // TODO The 'things' children must be deleted first, although we don't have foreign key constraint enforcing this!
                throw new UnsupportedOperationException("TODO not implemented");
        }
    }

    protected void deployThing(Asset thing, PersistenceEvent.Cause cause) {
        LOG.fine("Deploy thing: " + thing);
        ThingAttributes thingAttributes = new ThingAttributes(thing);

        // Attributes grouped by protocol name
        Map<String, List<ThingAttribute>> linkedAttributes = thingAttributes.getLinkedAttributes(
            // Linked attributes have a reference to an agent, and a protocol configuration attribute of that agent
            assetStorageService.getAgentLinkResolver()
        );

        LOG.fine("Thing has attribute links to " + linkedAttributes.size() + " protocol(s): " + thing);

        Collection<Protocol> protocols = container.getServices(Protocol.class);

        switch (cause) {
            case INSERT:
                linkAttributes(thing, protocols, linkedAttributes);
                break;
            case UPDATE:
                // TODO Not very efficient
                unlinkAttributes(thing, protocols, linkedAttributes);
                linkAttributes(thing, protocols, linkedAttributes);
                break;
            case DELETE:
                unlinkAttributes(thing, protocols, linkedAttributes);
                break;
        }
    }

    protected void linkAttributes(Asset thing,
                                  Collection<Protocol> protocols,
                                  Map<String, List<ThingAttribute>> attributes) {
        for (String protocolName : attributes.keySet()) {
            for (Protocol protocol : protocols) {
                if (protocol.getProtocolName() == null || protocol.getProtocolName().length() == 0)
                    throw new IllegalStateException("Protocol can't have empty name: " + protocol.getClass());
                if (protocol.getProtocolName().equals(protocolName)) {
                    try {
                        protocol.linkAttributes(attributes.get(protocolName));
                    } catch (Exception ex) {
                        // TODO: Better error handling?
                        LOG.log(Level.WARNING, "Ignoring error on attribute link for: " + thing, ex);
                    }
                }
            }
        }
    }

    protected void unlinkAttributes(Asset thing,
                                    Collection<Protocol> protocols,
                                    Map<String, List<ThingAttribute>> attributes) {
        for (Protocol protocol : protocols) {
            if (attributes.containsKey(protocol.getProtocolName())) {
                try {
                    protocol.unlinkAttributes(thing.getId());
                } catch (Exception ex) {
                    // TODO: Better error handling?
                    LOG.log(Level.WARNING, "Ignoring error on attribute unlink for: " + thing, ex);
                }
            }
        }
    }

    /**
     * If this update is not for an asset of type THING or it has been initiated by
     * a protocol then we ignore it.
     *
     * Otherwise we push the update to the protocol to handle and prevent any further
     * processing of this event by the processing chain, the protocol should raise
     * sensor updates as required (i.e. the protocol is responsible for synchronising state)
     *
     * @param assetUpdate
     */
    @Override
    public void accept(AssetUpdate assetUpdate) {
        // Check that asset is a THING
        if (assetUpdate.getAsset().getWellKnownType() != THING) {
            LOG.fine("Ignoring asset update as asset is not a THING:" + assetUpdate);
            return;
        }

        // If update was initiated by a protocol ignore it
        if (AbstractProtocol.class.isAssignableFrom(assetUpdate.getSender())) {
            LOG.fine("Ignoring asset update as it came from a protocol:" + assetUpdate);
            return;
        }

        boolean hasAgentLink = ThingAttribute.getAgentLink(assetUpdate.getAttribute()) != null;

        if (hasAgentLink) {
            // Check attribute is linked to an actual agent
            ThingAttributes thingAttributes = new ThingAttributes(assetUpdate.getAsset());
            ThingAttribute thingAttribute = thingAttributes.getLinkedAttribute(
                    assetStorageService.getAgentLinkResolver(), assetUpdate.getAttribute().getName()
            );

            if (thingAttribute == null) {
                LOG.warning("Cannot process asset update as agent link is invalid:" + assetUpdate);
                assetUpdate.setStatus(AssetUpdate.Status.ERROR);
                return;
            }
        } else {
            // This is just a non protocol attribute so allow the processing to continue
            LOG.fine("Ignoring asset update as it is not for an attribute linked to an agent:" + assetUpdate);
            return;
        }

        // Its' a send to actuator - push the update to the protocol
        LOG.fine("Processing asset update: " + assetUpdate);
        messageBrokerService.getProducerTemplate().sendBody(ACTUATOR_TOPIC, assetUpdate.getNewState());
        assetUpdate.setStatus(AssetUpdate.Status.HANDLED);
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}