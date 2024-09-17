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
package org.openremote.manager.simulator;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.simulator.SimulatorProtocol;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.agent.AgentService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.simulator.RequestSimulatorState;
import org.openremote.model.simulator.SimulatorAttributeInfo;
import org.openremote.model.simulator.SimulatorState;

import java.util.List;
import java.util.logging.Logger;

import static org.openremote.manager.event.ClientEventService.CLIENT_INBOUND_QUEUE;
import static org.openremote.manager.event.ClientEventService.getSessionKey;

// RT: Removed this from META-INF as RequestSimulatorState not used anywhere
/**
 * Connects the client/UI to the {@link SimulatorProtocol}.
 */
public class SimulatorService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SimulatorService.class.getName());

    protected AgentService agentService;
    protected ManagerIdentityService managerIdentityService;
    protected AssetStorageService assetStorageService;
    protected ClientEventService clientEventService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        agentService = container.getService(AgentService.class);
        managerIdentityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        clientEventService = container.getService(ClientEventService.class);

        clientEventService.addSubscriptionAuthorizer((realm, auth, subscription) -> {
            if (!subscription.isEventType(SimulatorState.class))
                return false;

            if (auth == null) {
                return false;
            }

            // Superuser can get all
            if (auth.isSuperUser())
                return true;

            // TODO Should realm admins be able to work with simulators in their realm?

            return false;
        });

        clientEventService.addEventAuthorizer((realm, auth, event) -> {
            if (event instanceof RequestSimulatorState) {

                // Only super users can use this
                return auth.isSuperUser();

                // TODO Should realm admins be able to work with simulators in their realm?
            }
            return false;
        });

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {
        from(CLIENT_INBOUND_QUEUE)
            .routeId("ClientInbound-ReadSimulatorState")
            .filter(body().isInstanceOf(RequestSimulatorState.class))
            .process(exchange -> {
                RequestSimulatorState event = exchange.getIn().getBody(RequestSimulatorState.class);
                LOG.finest("Handling from client: " + event);

                String sessionKey = getSessionKey(exchange);

                // TODO Should realm admins be able to work with simulators in their realm?
                publishSimulatorState(sessionKey, event.getAgentId());
            });
    }

    protected void publishSimulatorState(String sessionKey, String agentId) {
        LOG.finest("Attempting to publish simulator state: Agent ID=" + agentId);

        Protocol<?> protocol = agentService.getProtocolInstance(agentId);

        if (!(protocol instanceof SimulatorProtocol simulatorProtocol)) {
            LOG.warning("Failed to publish simulator state, agent is not a simulator agent: Agent ID=" + agentId);
            return;
        }

        SimulatorState simulatorState = getSimulatorState(simulatorProtocol);

        if (sessionKey != null) {
            clientEventService.sendToSession(sessionKey, simulatorState);
        } else {
            clientEventService.publishEvent(simulatorState);
        }
    }

    /**
     * Get info about all attributes linked to this instance (for frontend usage)
     */
    protected SimulatorState getSimulatorState(SimulatorProtocol protocolInstance) {
        LOG.info("Getting simulator info for protocol instance: " + protocolInstance);

        // We need asset names instead of identifiers for user-friendly display
        List<String> linkedAssetIds = protocolInstance.getLinkedAttributes().keySet().stream().map(AttributeRef::getId).distinct().toList();
        List<String> assetNames = assetStorageService.findNames(linkedAssetIds.toArray(new String[0]));

        if (assetNames.size() != linkedAssetIds.size()) {
            LOG.warning("Retrieved asset names don't match requested asset IDs");
            return null;
        }

        SimulatorAttributeInfo[] attributeInfos = protocolInstance.getLinkedAttributes().entrySet().stream().map(refAttributeEntry -> {
            String assetName = assetNames.get(linkedAssetIds.indexOf(refAttributeEntry.getKey().getId()));
            return new SimulatorAttributeInfo(assetName, refAttributeEntry.getKey().getId(), refAttributeEntry.getValue(), protocolInstance.getReplayMap().containsKey(refAttributeEntry.getKey()));
        }).toArray(SimulatorAttributeInfo[]::new);

        return new SimulatorState(protocolInstance.getAgent().getId(), attributeInfos);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
