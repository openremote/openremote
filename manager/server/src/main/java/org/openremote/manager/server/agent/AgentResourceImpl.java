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
package org.openremote.manager.server.agent;

import org.openremote.manager.server.asset.AssetResourceImpl;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.web.ManagerWebService;
import org.openremote.manager.shared.agent.AgentResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class AgentResourceImpl extends ManagerWebService implements AgentResource {

    private static final Logger LOG = Logger.getLogger(AssetResourceImpl.class.getName());
    protected AgentService agentService;

    public AgentResourceImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public ProtocolDescriptor[] getSupportedProtocols(RequestParams requestParams, String agentId) {
        Asset[] agentFinal = new Asset[1];

        return findAgent(agentId)
            .flatMap(agent -> {
                agentFinal[0] = agent;
                return agentService.getAgentConnector(agent);
            })
            .map(agentConnector -> {
                LOG.finer("Asking connector '" + agentConnector.getClass().getSimpleName() + "' for protocol descriptors");
                return agentConnector.getProtocolDescriptors(agentFinal[0]);
            })
            .orElseThrow(() -> {
                LOG.warning("Agent connector not found for agent ID: " + agentId);
                return new IllegalStateException("Agent connector not found or returned invalid response");
            });
    }

    @Override
    public Map<String, ProtocolDescriptor[]> getAllSupportedProtocols(RequestParams requestParams) {
        Map<String, ProtocolDescriptor[]> agentDescriptorMap = new HashMap<>(agentService.getAgents().size());
        agentService.getAgents().forEach((id, agent) -> {

            agentDescriptorMap.put(
                id,
                agentService.getAgentConnector(agent)
                    .map(agentConnector -> {
                        LOG.finer("Asking connector '" + agentConnector.getClass().getSimpleName() + "' for protocol descriptors");
                        return agentConnector.getProtocolDescriptors(agent);
                    })
                    .orElseThrow(() -> {
                        LOG.warning("Agent connector not found for agent ID: " + id);
                        return new IllegalStateException("Agent connector not found or returned invalid response");
                    })
            );
        });

        return agentDescriptorMap;
    }

    @Override
    public AssetAttribute[] getDiscoveredProtocolConfigurations(RequestParams requestParams, String agentId, String protocolName) {
        return new AssetAttribute[0];
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(RequestParams requestParams, String agentId, AssetAttribute protocolConfiguration) {
        return findAgent(agentId)
            .flatMap(agent -> agentService.getAgentConnector(agent))
            .map(agentConnector -> {
                LOG.finer("Asking connector '" + agentConnector.getClass().getSimpleName() + "' to validate protocol configuration: " + protocolConfiguration);
                return agentConnector.validateProtocolConfiguration(protocolConfiguration);
            })
            .orElseThrow(() -> {
                LOG.warning("Agent connector not found for agent ID: " + agentId);
                return new IllegalStateException("Agent connector not found or returned invalid response");
            });
    }

    protected Optional<Asset> findAgent(String agentId) {
        // Find the agent
        LOG.finer("Find agent: " + agentId);
        Asset agent = agentService.getAgents().get(agentId);
        if (agent == null || agent.getWellKnownType() != AssetType.AGENT) {
            LOG.warning("Failed to find agent with ID: " + agentId);
            throw new IllegalArgumentException("Agent not found");
        }
        return Optional.of(agent);
    }
}
