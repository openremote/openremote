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

import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.util.IdentifierUtil;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.DeviceResourceValueEvent;
import org.openremote.manager.shared.agent.InventoryModifiedEvent;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetInfo;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.connector.ConnectorComponent;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.Cause.UPDATE;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForAssetType;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;

public class AgentService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    final protected Map<String, AgentRoutes> agentInstanceMap = new LinkedHashMap<>();

    protected MessageBrokerService messageBrokerService;
    protected MessageBrokerContext messageBrokerContext;
    protected AssetService assetService;
    protected ConnectorService connectorService;
    protected AgentServiceEventRoutes agentServiceEventRoutes;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        messageBrokerContext = messageBrokerService.getContext();
        assetService = container.getService(AssetService.class);
        connectorService = container.getService(ConnectorService.class);

        agentServiceEventRoutes = new AgentServiceEventRoutes(
            new DeviceResourceSubscriptions(container.getService(EventService.class))
        ) {
            @Override
            protected Asset getAsset(String assetId) {
                return assetService.get(assetId);
            }

            @Override
            protected void deleteAssetChildren(String parentAssetId) {
                assetService.deleteChildren(parentAssetId);
            }
        };

        messageBrokerService.getContext().addRoutes(this);
        messageBrokerService.getContext().addRoutes(agentServiceEventRoutes);
    }

    @Override
    public void configure(Container container) throws Exception {
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

    @Override
    public void configure() throws Exception {
        // If any agent was modified in the database, reconfigure this service
        from(PERSISTENCE_EVENT_TOPIC)
            .filter(isPersistenceEventForEntityType(Asset.class))
            .filter(isPersistenceEventForAssetType(AssetType.AGENT))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Asset agent = (Asset) persistenceEvent.getEntity();
                ServerAsset[] agents = assetService.findByType(agent.getRealm(), AssetType.AGENT);
                LOG.fine("Reconfigure agents of realm '" + agent.getRealm() + "': " + agents.length);
                reconfigureAgents(agents, persistenceEvent.getCause() == UPDATE ? agent : null);
            });
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

    protected void startDeviceRoutes(Asset agentAsset, AgentRoutes agentRoutes) throws Exception {
        // We start listener routes for all device children of the agent asset
        // TODO: We should selectively enable this on a per-device basis
        List<Asset> deviceAssets = new ArrayList<>();
        AssetInfo[] agentChildren = assetService.getChildren(agentAsset.getId());
        for (AssetInfo agentChild : agentChildren) {
            if (agentChild.getWellKnownType() == AssetType.DEVICE) {
                deviceAssets.add(assetService.get(agentChild.getId()));
            }
        }
        messageBrokerContext.addRoutes(agentRoutes.buildDeviceRoutes(deviceAssets));
    }

    protected void stopDeviceRoutes(AgentRoutes agentRoutes) throws Exception {
        agentRoutes.stopDeviceRoutes(messageBrokerContext);
    }
}