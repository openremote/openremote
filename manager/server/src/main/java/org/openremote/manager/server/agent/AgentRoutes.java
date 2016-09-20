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
import org.openremote.agent.controller2.Controller2Component;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.InventoryModifiedEvent;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.connector.ConnectorComponent;
import org.openremote.manager.shared.device.DeviceAttributes;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import static org.openremote.manager.server.event.EventPredicates.isEventType;

/**
 * The messaging routes of a running agent instance.
 *
 */
public abstract class AgentRoutes {

    private static final Logger LOG = Logger.getLogger(AgentRoutes.class.getName());

    public static String PRODUCER_INVENTORY_ROUTE(String agentAssetId) {
        return "direct:urn:openremote:agent:inventory:" + agentAssetId;
    }

    public static String PRODUCER_DISCOVERY_ROUTE(String agentAssetId) {
        return "direct:urn:openremote:agent:discovery:" + agentAssetId;
    }

    public static String PRODUCER_READ_ROUTE(String agentAssetId) {
        return "direct:urn:openremote:agent:read:" + agentAssetId;
    }

    public static String PRODUCER_WRITE_ROUTE(String agentAssetId) {
        return "direct:urn:openremote:agent:write:" + agentAssetId;
    }

    final protected Set<String> agentRouteIds = new CopyOnWriteArraySet<>();
    final protected Set<String> deviceRouteIds = new CopyOnWriteArraySet<>();
    final protected Asset agentAsset;
    final protected Agent agent;
    final protected ConnectorComponent connectorComponent;

    public AgentRoutes(Asset agentAsset, Agent agent, ConnectorComponent connectorComponent) {
        this.agentAsset = agentAsset;
        this.agent = agent;
        this.connectorComponent = connectorComponent;
    }

    public RouteBuilder buildAgentRoutes() throws Exception {
        return new RouteBuilder() {

            String routeId;
            String endpointUri;

            @Override
            public void configure() throws Exception {
                for (ConnectorComponent.Capability capability : connectorComponent.getConsumerCapabilities()) {
                    switch (capability) {
                        case inventory:
                            routeId = "Agent Inventory Consumer - " + agentAsset.getId();
                            endpointUri = connectorComponent.buildConsumerEndpoint(capability, agentAsset.getId(), agent, null);
                            if (endpointUri == null)
                                throwCapabilityMismatchException(capability);
                            from(endpointUri)
                                .routeId(routeId)
                                .filter(isEventType(InventoryModifiedEvent.class))
                                .process(exchange -> {
                                    handleInventoryModified(
                                        exchange.getIn().getBody(InventoryModifiedEvent.class)
                                    );
                                });
                            agentRouteIds.add(routeId);
                            break;
                    }
                }

                for (ConnectorComponent.Capability capability : connectorComponent.getProducerCapabilities()) {
                    switch (capability) {
                        case discovery:
                            routeId = "Agent Discovery Producer - " + agentAsset.getId();
                            endpointUri = connectorComponent.buildProducerEndpoint(capability, agentAsset.getId(), agent);
                            if (endpointUri == null)
                                throwCapabilityMismatchException(capability);
                            from(PRODUCER_DISCOVERY_ROUTE(agentAsset.getId()))
                                .routeId(routeId)
                                .to(endpointUri);
                            agentRouteIds.add(routeId);
                            break;
                        case inventory:
                            routeId = "Agent Inventory Producer - " + agentAsset.getId();
                            endpointUri = connectorComponent.buildProducerEndpoint(capability, agentAsset.getId(), agent);
                            if (endpointUri == null)
                                throwCapabilityMismatchException(capability);
                            from(PRODUCER_INVENTORY_ROUTE(agentAsset.getId()))
                                .routeId(routeId)
                                .to(endpointUri);
                            agentRouteIds.add(routeId);
                            break;
                        case read:
                            routeId = "Agent Read Producer - " + agentAsset.getId();
                            endpointUri = connectorComponent.buildProducerEndpoint(capability, agentAsset.getId(), agent);
                            if (endpointUri == null)
                                throwCapabilityMismatchException(capability);
                            from(PRODUCER_READ_ROUTE(agentAsset.getId()))
                                .routeId(routeId)
                                .to(endpointUri);
                            agentRouteIds.add(routeId);
                            break;
                        case write:
                            routeId = "Agent Write Producer - " + agentAsset.getId();
                            endpointUri = connectorComponent.buildProducerEndpoint(capability, agentAsset.getId(), agent);
                            if (endpointUri == null)
                                throwCapabilityMismatchException(capability);
                            from(PRODUCER_WRITE_ROUTE(agentAsset.getId()))
                                .routeId(routeId)
                                .to(endpointUri);
                            agentRouteIds.add(routeId);
                            break;
                    }
                }
            }
        };
    }

    public RouteBuilder buildDeviceRoutes(Collection<Asset> deviceAssets) throws Exception {
        if (!connectorComponent.getConsumerCapabilities().contains(ConnectorComponent.Capability.listen)) {
            throw new IllegalArgumentException(
                "Connector does not support capability " + ConnectorComponent.Capability.listen + ": " + connectorComponent.getType()
            );
        }
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                for (Asset deviceAsset : deviceAssets) {
                    DeviceAttributes deviceAttributes = new DeviceAttributes(deviceAsset.getAttributes());
                    String deviceKey = deviceAttributes.getKey();
                    String routeId = "Agent Listen Consumer - " + agentAsset.getId() + "(" + deviceKey + ")";
                    String endpointUri = connectorComponent.buildConsumerEndpoint(
                        ConnectorComponent.Capability.listen, agentAsset.getId(), agent, deviceKey
                    );
                    from(endpointUri)
                        .routeId(routeId)
                        .process(exchange -> {
                            String receivedDeviceKey = exchange.getIn().getHeader(Controller2Component.HEADER_DEVICE_KEY, String.class);
                            String receivedResourceKey = exchange.getIn().getHeader(Controller2Component.HEADER_DEVICE_RESOURCE_KEY, String.class);
                            Object value = exchange.getIn().getBody();
                            handleResourceValueUpdate(receivedDeviceKey, receivedResourceKey, value);
                        });
                    deviceRouteIds.add(routeId);
                }
            }
        };
    }

    public void stopAgentRoutes(MessageBrokerContext context) throws Exception {
        for (String agentRouteId : agentRouteIds) {
            LOG.fine("Stopping agent route: " + agentRouteId);
            context.stopRoute(agentRouteId);
            context.removeRoute(agentRouteId);
        }
    }

    public void stopDeviceRoutes(MessageBrokerContext context) throws Exception {
        for (String deviceRouteId : deviceRouteIds) {
            LOG.fine("Stopping agent device route: " + deviceRouteId);
            context.stopRoute(deviceRouteId);
            context.removeRoute(deviceRouteId);
        }
    }

    protected void throwCapabilityMismatchException(ConnectorComponent.Capability capability) {
        throw new IllegalStateException(
            "Capability '" + capability +"' announced but component didn't build endpoint: " + connectorComponent.getType()
        );
    }

    protected abstract void handleInventoryModified(InventoryModifiedEvent event);

    protected abstract void handleResourceValueUpdate(String deviceKey, String deviceResourceKey, Object value);
}
