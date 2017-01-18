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
import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.model.AttributeValueChange;
import org.openremote.model.asset.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.Protocol.SENSOR_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForAssetType;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.AssetType.THING;

public class AgentService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected Container container;
    protected MessageBrokerService messageBrokerService;
    protected MessageBrokerContext messageBrokerContext;
    protected AssetService assetService;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        messageBrokerService = container.getService(MessageBrokerService.class);
        messageBrokerContext = messageBrokerService.getContext();
        assetService = container.getService(AssetService.class);

        messageBrokerService.getContext().addRoutes(this);
    }

    @Override
    public void configure(Container container) throws Exception {
    }

    @Override
    public void start(Container container) throws Exception {
        Asset[] agents = assetService.findByTypeInAllRealms(AGENT.getValue());
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

        // Update thing asset when attribute change value messages are published on the sensor topic
        from(SENSOR_TOPIC)
            .filter(body().isInstanceOf(AttributeValueChange.class))
            .process(exchange -> {
                AttributeValueChange attributeValueChange =
                    exchange.getIn().getBody(AttributeValueChange.class);
                // Note that this is a _direct_ update of the attribute value in the database, it will
                // not trigger a persistence event - we don't want to redeploy a thing just because an
                // attribute value changed!
                boolean success = assetService.updateThingAttributeValue(attributeValueChange);
                // TODO If success then... notify asset listener clients? If not, then handle error?

            });
    }

    protected void deployAgent(Asset agent, PersistenceEvent.Cause cause) {
        LOG.fine("Deploy agent: " + agent);
        switch (cause) {
            case UPDATE:
                // TODO: Find everything with this agent ID in the asset tree not just children
                Asset[] things = assetService.findChildrenByType(agent.getId(), THING);
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
            assetService.getAgentLinkResolver()
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
                if (protocol.getProtocolName().equals(protocolName)) {
                    try {
                        protocol.linkAttributes(attributes.get(protocolName));
                    } catch (Exception ex) {
                        // TODO: Better error handling?
                        LOG.log(Level.INFO, "Ignoring error on attribute unlink for: " + thing, ex);
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
                    LOG.log(Level.INFO, "Ignoring error on attribute unlink for: " + thing, ex);
                }
            }
        }
    }
}