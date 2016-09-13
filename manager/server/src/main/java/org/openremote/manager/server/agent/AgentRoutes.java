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
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.manager.server.event.EventPredicate;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.InventoryModifiedEvent;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.connector.ConnectorComponent;

import java.util.logging.Logger;

/**
 * The messaging routes of a running agent instance.
 */
public abstract class AgentRoutes extends RouteBuilder {

    public static String TRIGGER_DISCOVERY_ROUTE(String agentAssetId) {
        return "direct:urn:openremote:agent:triggerDiscovery:" + agentAssetId;
    }

    private static final Logger LOG = Logger.getLogger(AgentRoutes.class.getName());

    final protected Asset agentAsset;
    final protected Agent agent;
    final protected ConnectorComponent connectorComponent;
    final protected String inventoryUri;
    final protected String triggerDiscoveryUri;

    public AgentRoutes(Asset agentAsset, Agent agent, ConnectorComponent connectorComponent) {
        this.agentAsset = agentAsset;
        this.agent = agent;
        this.connectorComponent = connectorComponent;

        inventoryUri = connectorComponent.getInventoryUri(agentAsset.getId(), agent);
        triggerDiscoveryUri = connectorComponent.getDiscoveryTriggerUri(agentAsset.getId(), agent);
    }

    @Override
    public void configure() throws Exception {
        if (inventoryUri != null) {
            from(inventoryUri)
                .routeId("Agent Inventory - " + agentAsset.getId())
                .filter(new EventPredicate<>(InventoryModifiedEvent.class))
                .process(exchange -> {
                    handleInventoryModified(
                        exchange.getIn().getBody(InventoryModifiedEvent.class)
                    );
                });
        }

        if (triggerDiscoveryUri != null) {
            from(TRIGGER_DISCOVERY_ROUTE(agentAsset.getId()))
                .routeId("Agent Trigger Discovery - " + agentAsset.getId())
                .to(triggerDiscoveryUri);
        }
    }

    public void stop(MessageBrokerContext context) throws Exception {
        if (inventoryUri != null) {
            context.stopRoute("Agent Inventory - " + agentAsset.getId());
        }
        if (triggerDiscoveryUri != null) {
            context.stopRoute("Agent Trigger Discovery - " + agentAsset.getId());
        }
    }

    protected abstract void handleInventoryModified(InventoryModifiedEvent event);
}
