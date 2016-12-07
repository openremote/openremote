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
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.shared.asset.AssetType;

import java.util.logging.Logger;

public class AgentService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected MessageBrokerService messageBrokerService;
    protected MessageBrokerContext messageBrokerContext;
    protected AssetService assetService;
    protected ConnectorService connectorService;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        messageBrokerContext = messageBrokerService.getContext();
        assetService = container.getService(AssetService.class);
        connectorService = container.getService(ConnectorService.class);
    }

    @Override
    public void configure(Container container) throws Exception {
    }

    @Override
    public void start(Container container) throws Exception {
        /*
        ServerAsset[] agents = assetService.findByTypeInAllRealms(AssetType.AGENT.getValue());
        LOG.fine("Configure agents in all realms:" + agents.length);
        reconfigureAgents(null, agents, null);
        */
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public void configure() throws Exception {
        /*
        // If any agent was modified in the database, reconfigure this service
        from(PERSISTENCE_EVENT_TOPIC)
            .filter(isPersistenceEventForEntityType(Asset.class))
            .filter(isPersistenceEventForAssetType(AssetType.AGENT))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Asset agent = (Asset) persistenceEvent.getEntity();
                ServerAsset[] agents = assetService.findByType(agent.getRealm(), AssetType.AGENT);
                LOG.fine("Reconfigure agents of realm '" + agent.getRealm() + "': " + agents.length);
                reconfigureAgents(agent.getRealm(), agents, persistenceEvent.getCause() == UPDATE ? agent : null);
            });
            */
    }

    /*
    protected void reconfigureAgents(String realm, ServerAsset[] agents, Asset updatedAgent) {
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

            // Find all running agents of the given realm, stop them if they are no longer present (in database)
            List<String> agentsToStop = new ArrayList<>();
            for (Map.Entry<String, AgentRoutes> entry : agentInstanceMap.entrySet()) {
                if (realm == null || entry.getValue().getAgentAsset().getRealm().equals(realm)) {
                    agentsToStop.add(entry.getKey());
                }
            }
            Iterator<String> it = agentsToStop.iterator();
            while (it.hasNext()) {
                String agentId = it.next();
                for (ServerAsset agent : agents) {
                    if (agent.getId().equals(agentId)) {
                        it.remove();
                        break;
                    }
                }
            }
            for (String agentId : agentsToStop) {
                LOG.fine("Agent is not present anymore and still running: " + agentId);
                stopAgent(agentId);
            }
        }
    }

    protected void startAgent(Asset agentAsset) {
        LOG.fine("Starting agent: " + agentAsset);

        Agent agent = new Agent(new Attributes(agentAsset.getAttributes()));
        ConnectorComponent connectorComponent = connectorService.getConnectorComponentByType(agent.getConnectorType());
        // TODO: Check if we have the component, otherwise mark the agent configuration invalid
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

                try {
                    AgentService.this.stopDeviceRoutes(this);
                    AgentService.this.startDeviceRoutes(agentAsset, this);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Error restarting device routes of agent: " + agentAsset.getId(), ex);
                    // TODO: We should mark the agent or fire event, or...
                }
            }

            @Override
            protected void handleResourceValueUpdate(String deviceKey, String deviceResourceKey, Object value) {
                // TODO Value conversion?
                DeviceResourceValueEvent event = new DeviceResourceValueEvent(
                    agentAsset.getId(), deviceKey, deviceResourceKey, value != null ? value.toString() : null
                );
                agentServiceEventRoutes.getDeviceResourceSubscriptions().dispatch(event);
            }
        };

        try {
            messageBrokerContext.addRoutes(agentRoutes.buildAgentRoutes());
            startDeviceRoutes(agentAsset, agentRoutes);
            agentInstanceMap.put(agentAsset.getId(), agentRoutes);
        } catch (FailedToCreateRouteException ex) {
            LOG.log(Level.SEVERE, "Error starting agent: " + agentAsset, ex);
            // TODO: We should mark the agent or fire event, or...
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error starting agent: " + agentAsset, ex);
            // TODO: We should mark the agent or fire event, or...
        }
    }

    protected void stopAgent(String agentId) {
        LOG.fine("Stopping agent: " + agentId);
        if (agentInstanceMap.containsKey(agentId)) {
            AgentRoutes agentRoutes = agentInstanceMap.remove(agentId);
            try {
                stopDeviceRoutes(agentRoutes);
                agentRoutes.stopAgentRoutes(messageBrokerContext);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error stopping agent: " + agentId, ex);
                // TODO: We should mark the agent or fire event, or...
            }
        }
    }
    */
}