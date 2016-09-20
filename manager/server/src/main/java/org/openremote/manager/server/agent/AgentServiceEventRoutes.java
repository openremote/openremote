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
import org.apache.camel.model.ChoiceDefinition;
import org.openremote.container.web.socket.UserPredicates;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.agent.*;
import org.openremote.manager.shared.asset.Asset;

import java.util.logging.Logger;

import static org.openremote.container.web.socket.UserPredicates.isUserInRole;
import static org.openremote.manager.server.event.EventPredicates.isEventType;
import static org.openremote.manager.shared.connector.ConnectorComponent.HEADER_DEVICE_KEY;
import static org.openremote.manager.shared.connector.ConnectorComponent.HEADER_DEVICE_RESOURCE_KEY;

/**
 * The messaging routes of all running agent instances, central dispatch.
 */
public abstract class AgentServiceEventRoutes extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(AgentServiceEventRoutes.class.getName());

    final protected DeviceResourceSubscriptions deviceResourceSubscriptions;

    public AgentServiceEventRoutes(DeviceResourceSubscriptions deviceResourceSubscriptions) {
        this.deviceResourceSubscriptions = deviceResourceSubscriptions;
    }

    @Override
    public void configure() throws Exception {
        ChoiceDefinition incomingEventRoute =
            from(EventService.INCOMING_EVENT_QUEUE)
                .routeId("Agent Service Event Route")
                .choice();
        buildInventoryEventRoute(incomingEventRoute);
        buildDeviceEventRoute(incomingEventRoute);
    }

    public DeviceResourceSubscriptions getDeviceResourceSubscriptions() {
        return deviceResourceSubscriptions;
    }

    protected void buildInventoryEventRoute(ChoiceDefinition routeDefinition) {
        routeDefinition
            .when(isEventType(RefreshInventoryEvent.class))
            .choice()
            .when(isUserInRole("write:assets"))
            .process(exchange -> {
                String agentAssetId = exchange.getIn().getBody(RefreshInventoryEvent.class).getAgentId();
                Asset agentAsset = getAsset(agentAssetId);
                if (agentAsset == null)
                    return;
                LOG.fine("Clearing inventory of agent: " + agentAssetId);
                deleteAssetChildren(agentAssetId);

                exchange.getIn().getHeaders().clear();
                exchange.getIn().setBody(null);

                exchange.getIn().setHeader(
                    "routingSlip",
                    AgentRoutes.PRODUCER_DISCOVERY_ROUTE(agentAssetId)
                );
            })
            .routingSlip(header("routingSlip")).ignoreInvalidEndpoints()
            .end()
            .otherwise()
            .to("log:org.openremote.event.forbidden?level=INFO&showAll=true&multiline=true")
            .endChoice();
    }

    protected void buildDeviceEventRoute(ChoiceDefinition routeDefinition) {
        routeDefinition
            .when(isEventType(SubscribeDeviceResourceUpdates.class))
            .choice()
            .when(UserPredicates.isUserInRole("read:assets"))
            .process(exchange -> {
                String sessionKey = EventService.getSessionKey(exchange);
                SubscribeDeviceResourceUpdates event = exchange.getIn().getBody(SubscribeDeviceResourceUpdates.class);
                Asset agentAsset = getAsset(event.getAgentId());
                if (agentAsset == null) {
                    LOG.fine("Agent with identifier '" + event.getAgentId() + "' not found, ignoring subscription on session: " + sessionKey);
                    return;
                }
                deviceResourceSubscriptions.addSubscription(sessionKey, event);
            })
            .otherwise()
            .to("log:org.openremote.event.forbidden?level=INFO&showAll=true&multiline=true")
            .endChoice()

            .when(isEventType(UnsubscribeDeviceResourceUpdates.class))
            .choice()
            .when(UserPredicates.isUserInRole("read:assets"))
            .process(exchange -> {
                String sessionKey = EventService.getSessionKey(exchange);
                UnsubscribeDeviceResourceUpdates event = exchange.getIn().getBody(UnsubscribeDeviceResourceUpdates.class);
                deviceResourceSubscriptions.removeSubscription(sessionKey, event);
            })
            .otherwise()
            .to("log:org.openremote.event.forbidden?level=INFO&showAll=true&multiline=true")
            .endChoice()

            .when(isEventType(DeviceResourceWrite.class))
            .choice()
            .when(UserPredicates.isUserInRole("write:assets"))
            .process(exchange -> {
                DeviceResourceWrite event = exchange.getIn().getBody(DeviceResourceWrite.class);
                LOG.fine("Processing: " + event);
                exchange.getIn().setHeader(
                    "routingSlip",
                    AgentRoutes.PRODUCER_WRITE_ROUTE(event.getAgentId())
                );
                exchange.getIn().setHeader(HEADER_DEVICE_KEY, event.getDeviceKey());
                exchange.getIn().setHeader(HEADER_DEVICE_RESOURCE_KEY, event.getDeviceResourceKey());
                exchange.getIn().setBody(event.getValue());
            })
            .routingSlip(header("routingSlip")).ignoreInvalidEndpoints()
            .end()
            .otherwise()
            .to("log:org.openremote.event.forbidden?level=INFO&showAll=true&multiline=true")
            .endChoice()

            .when(isEventType(DeviceResourceRead.class))
            .choice()
            .when(UserPredicates.isUserInRole("read:assets"))
            .process(exchange -> {
                DeviceResourceRead event = exchange.getIn().getBody(DeviceResourceRead.class);
                LOG.fine("Processing: " + event);
                exchange.getIn().setHeader(
                    "routingSlip",
                    AgentRoutes.PRODUCER_READ_ROUTE(event.getAgentId())
                );
                exchange.getIn().setHeader("agentId", event.getAgentId());
                exchange.getIn().setHeader(HEADER_DEVICE_KEY, event.getDeviceKey());
                exchange.getIn().setHeader(HEADER_DEVICE_RESOURCE_KEY, event.getDeviceResourceKey());
            })
            .routingSlip(header("routingSlip"))
            .ignoreInvalidEndpoints()
            .process(exchange -> {
                // TODO value conversion?
                if (exchange.getIn().getBody() != null) {
                    DeviceResourceValueEvent event = new DeviceResourceValueEvent(
                        exchange.getIn().getHeader("agentId", String.class),
                        exchange.getIn().getHeader(HEADER_DEVICE_KEY, String.class),
                        exchange.getIn().getHeader(HEADER_DEVICE_RESOURCE_KEY, String.class),
                        exchange.getIn().getBody(String.class)
                    );
                    exchange.getIn().setBody(event);
                } else {
                    LOG.fine("Read did not result in a value, ignoring");
                }
            })
            .to(EventService.OUTGOING_EVENT_QUEUE)
            .endChoice()
            .otherwise()
            .to("log:org.openremote.event.forbidden?level=INFO&showAll=true&multiline=true")
            .endChoice();
    }

    abstract protected Asset getAsset(String assetId);

    abstract protected void deleteAssetChildren(String parentAssetId);
}
