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

import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.util.IdentifierUtil;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.shared.Function;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.InventoryModifiedEvent;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.connector.ConnectorComponent;
import org.openremote.manager.shared.util.Util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.*;

public class AgentService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected MessageBrokerService messageBrokerService;
    ProducerTemplate producerTemplate;

    protected AssetService assetService;
    protected ConnectorService connectorService;

    final protected Map<String, AgentRoutes> agentInstanceMap = new LinkedHashMap<>();

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        producerTemplate = messageBrokerService.getContext().createProducerTemplate();
        assetService = container.getService(AssetService.class);
        connectorService = container.getService(ConnectorService.class);

        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                // If any agent was modified in the database, reconfigure this service
                from(PERSISTENCE_EVENT_TOPIC)
                    .filter(body().isInstanceOf(Asset.class))
                    .filter(exchange -> {
                        Asset asset = exchange.getIn().getBody(Asset.class);
                        return AssetType.AGENT.equals(asset.getWellKnownType());
                    })
                    .process(exchange -> {
                        PersistenceEvent persistenceEvent = exchange.getIn().getHeader(PERSISTENCE_EVENT_HEADER, PersistenceEvent.class);
                        Asset agent = exchange.getIn().getBody(Asset.class);

                        ServerAsset[] agents = assetService.findByType(agent.getRealm(), AssetType.AGENT);
                        LOG.fine("Reconfigure agents of realm '" + agent.getRealm() + "': " + agents.length);
                        reconfigureAgents(agents, persistenceEvent == UPDATE ? agent : null);
                    });

                // Dynamically route discovery trigger messages to agents
                from(Agent.TOPIC_TRIGGER_DISCOVERY)
                    .routingSlip(method(AgentService.this, "triggerDiscoverySlip"))
                    .ignoreInvalidEndpoints();
            }
        });
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new AgentResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
        ServerAsset[] agents = assetService.findByTypeInAllRealms(AssetType.AGENT);
        LOG.fine("Configure agents in all realms:" + agents.length);
        reconfigureAgents(agents, null);
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public void clearInventory(String agentId) {
        assetService.deleteChildren(agentId);
    }

    public void triggerDiscovery(String realm, String agentId) {
        if (agentId != null) {
            producerTemplate.sendBody(Agent.TOPIC_TRIGGER_DISCOVERY, agentId);
        } else {
            if (realm == null || realm.length() == 0) {
                throw new IllegalArgumentException(
                    "Realm must be provided to trigger discovery of all agents (in that realm)"
                );
            }
            producerTemplate.sendBodyAndHeader(
                Agent.TOPIC_TRIGGER_DISCOVERY,
                null,
                Agent.TOPIC_TRIGGER_DISCOVERY_HEADER_REALM, realm
            );
        }
    }

    protected void reconfigureAgents(ServerAsset[] agents, Asset updatedAgent) {
        synchronized (agentInstanceMap) {
            for (ServerAsset agentAsset : agents) {
                Agent agent = new Agent(new Attributes(agentAsset.getAttributes()));
                if (agent.isEnabled()) {

                    if (!agentInstanceMap.containsKey(agentAsset.getId())) {
                        LOG.fine("Agent is enabled and was not running: " + agentAsset.getId());
                        startAgent(agentAsset);
                    } else if (updatedAgent != null && agentAsset.getId().equals(updatedAgent.getId())) {
                        LOG.fine("Agent is enabled and already running, restarting due to update: " + agentAsset.getId());
                        stopAgent(agentAsset.getId());
                        startAgent(agentAsset);
                    }

                    // Agent is not enabled anymore but still running
                } else if (agentInstanceMap.containsKey(agentAsset.getId())) {
                    LOG.fine("Agent is disabled and still running: " + agentAsset.getId());
                    stopAgent(agentAsset.getId());
                }
            }

            for (String agentId : agentInstanceMap.keySet()) {
                boolean found = false;
                for (ServerAsset agent : agents) {
                    if (agent.getId().equals(agentId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOG.fine("Agent is not present anymore and still running: " + agentId);
                    stopAgent(agentId);
                }
            }
        }
    }

    protected void startAgent(Asset agentAsset) {
        LOG.fine("Starting agent: " + agentAsset);

        Agent agent = new Agent(new Attributes(agentAsset.getAttributes()));
        ConnectorComponent connectorComponent = connectorService.getConnectorComponentByType(agent.getConnectorType());
        AgentRoutes agentRoutes = new AgentRoutes(agentAsset, agent, connectorComponent) {
            @Override
            protected void handleInventoryModified(InventoryModifiedEvent event) {
                Asset deviceAsset = event.getDeviceAsset();

                // We must combine the device and the agent identifiers to uniquely identify a
                // discovered device - the same device id might be given to several agents (and
                // these agents can be in different realms)
                String combinedAssetId = IdentifierUtil.getEncodedHash(
                    agentAsset.getId().getBytes(Charset.forName("utf-8")),
                    deviceAsset.getId().getBytes(Charset.forName("utf-8"))
                );

                switch (event.getCause()) {
                    case PUT:
                        LOG.fine("Put child asset of agent '" + agentAsset.getId() + "': " + deviceAsset);

                        ServerAsset deviceServerAsset = assetService.get(combinedAssetId);
                        if (deviceServerAsset == null) {
                            deviceServerAsset = ServerAsset.map(
                                deviceAsset,
                                new ServerAsset(),
                                agentAsset.getRealm(),
                                agentAsset.getId(),
                                agentAsset.getCoordinates()
                            );
                            deviceServerAsset.setId(combinedAssetId);
                            LOG.fine("Child asset of agent is new, merging: " + deviceServerAsset);
                            deviceServerAsset = assetService.merge(deviceServerAsset);
                        } else {
                            deviceServerAsset = ServerAsset.map(
                                deviceAsset,
                                deviceServerAsset,
                                agentAsset.getRealm(),
                                agentAsset.getId(),
                                agentAsset.getCoordinates()
                            );
                            LOG.fine("Child asset of agent already exists, merging: " + deviceServerAsset);
                            deviceServerAsset = assetService.merge(deviceServerAsset);
                        }
                        break;
                    case DELETE:
                        LOG.fine("Delete child asset of agent '" + agentAsset.getId() + "': " + deviceAsset);
                        assetService.delete(combinedAssetId);
                        break;
                }
            }
        };

        agentInstanceMap.put(agentAsset.getId(), agentRoutes);

        try {

            MessageBrokerContext context = messageBrokerService.getContext();
            context.addRoutes(agentRoutes);

        } catch (FailedToCreateRouteException ex) {
            LOG.log(Level.SEVERE, "Error starting agent" + agentAsset, ex);
            // TODO: We should mark the agent or fire event, or...
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error starting agent" + agentAsset, ex);
            // TODO: We should mark the agent or fire event, or...
        }
    }

    protected void stopAgent(String agentId) {
        LOG.fine("Stopping agent: " + agentId);
        if (agentInstanceMap.containsKey(agentId)) {
            AgentRoutes agentRoutes = agentInstanceMap.remove(agentId);
            MessageBrokerContext context = messageBrokerService.getContext();
            try {
                agentRoutes.stop(context);
            } catch (FailedToCreateRouteException ex) {
                LOG.log(Level.SEVERE, "Error stopping agent" + agentId, ex);
                // TODO: We should mark the agent or fire event, or...
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error stopping agent" + agentId, ex);
                // TODO: We should mark the agent or fire event, or...
            }
        }
    }

    public String triggerDiscoverySlip(String agentAssetId, Exchange exchange) {
        // Find given or all agents of given realm, add their trigger discovery endpoints to the routing slip
        List<String> destinations = new ArrayList<>();
        if (agentAssetId != null && agentAssetId.length() > 0) {
            Asset agentAsset = assetService.get(agentAssetId);
            if (agentAsset != null) {
                LOG.fine("Routing trigger discovery message for agent: " + agentAssetId);
                destinations.add(
                    AgentRoutes.TRIGGER_DISCOVERY_ROUTE(agentAssetId)
                );
            }
        } else {
            String realm = exchange.getIn().getHeader(
                Agent.TOPIC_TRIGGER_DISCOVERY_HEADER_REALM, String.class
            );
            if (realm != null) {
                LOG.fine("Routing trigger discovery message for all agents of realm: " + realm);
                Asset[] allAgents = assetService.findByType(realm, AssetType.AGENT);
                for (Asset agent : allAgents) {
                    destinations.add(
                        AgentRoutes.TRIGGER_DISCOVERY_ROUTE(agent.getId())
                    );
                }
            }
        }

        LOG.fine("Triggering discovery on agents: " + destinations);
        return Util.toCommaSeparated(
            destinations.toArray(new String[destinations.size()])
        );
    }
}