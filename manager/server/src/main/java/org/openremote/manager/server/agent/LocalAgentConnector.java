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

import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.ProtocolLinkedAttributeDiscovery;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.*;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.file.FileInfo;
import org.openremote.model.util.Pair;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LocalAgentConnector implements AgentConnector {

    private static final Logger LOG = Logger.getLogger(LocalAgentConnector.class.getName());
    protected AgentService agentService;

    public LocalAgentConnector(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public List<AgentStatusEvent> getConnectionStatus(Asset agent) {
        Optional<Asset> foundAgent = agentService.getAgents().entrySet().stream()
            .filter(entry -> entry.getKey().equals(agent.getId()))
            .map(Map.Entry::getValue)
            .findFirst();
        return foundAgent.map(asset -> Agent.getProtocolConfigurations(asset).stream()
            .map(AssetAttribute::getReferenceOrThrow)
            .map(protocolConfigurationRef -> new Pair<>(protocolConfigurationRef, agentService.getProtocolConnectionStatus(protocolConfigurationRef)))
            .map(pair -> new AgentStatusEvent(
                agentService.timerService.getCurrentTimeMillis(),
                asset.getRealmId(),
                pair.key,
                pair.value)
            )
            .collect(Collectors.toList())).orElseGet(ArrayList::new);
    }

    @Override
    public ProtocolDescriptor[] getProtocolDescriptors(Asset agent) {
        return agentService.protocols
            .values()
            .stream()
            .map(Protocol::getProtocolDescriptor).toArray(ProtocolDescriptor[]::new);
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        return agentService.protocols
            .values()
            .stream()
            .filter(protocol -> protocol.getProtocolName().equals(protocolConfiguration.getValueAsString().orElse(null)))
            .findFirst()
            .map(protocol -> protocol.validateProtocolConfiguration(protocolConfiguration))
            .orElse(new AttributeValidationResult(
                protocolConfiguration.getName().orElse(null),
                Collections.singletonList(
                    new ValidationFailure(
                        ProtocolConfiguration.ValidationFailureReason.VALUE_NOT_A_VALID_PROTOCOL_URN
                    )),
                null));
    }

    @Override
    public Asset[] getDiscoveredLinkedAttributes(AttributeRef protocolConfigurationRef) throws IllegalArgumentException, UnsupportedOperationException {
        Optional<Pair<Protocol, AssetAttribute>> protocolAndConfigOptional = getProtocolAndConfig(protocolConfigurationRef);

        if (!protocolAndConfigOptional.isPresent()) {
            throw new IllegalArgumentException("Protocol not found for: " + protocolConfigurationRef);
        }

        // Check protocol is of correct type
        if (!(protocolAndConfigOptional.get().key instanceof ProtocolLinkedAttributeDiscovery)) {
            LOG.info("Protocol not of type '" + ProtocolLinkedAttributeDiscovery.class.getSimpleName() + "'");
            throw new UnsupportedOperationException("Protocol doesn't support linked attribute discovery:" + protocolAndConfigOptional.get().key.getProtocolDisplayName());
        }

        return protocolAndConfigOptional
            .map(protocolAndConfig -> {
                ProtocolLinkedAttributeDiscovery discoveryProtocol = (ProtocolLinkedAttributeDiscovery)protocolAndConfig.key;
                return discoveryProtocol.discoverLinkedAssetAttributes(protocolAndConfig.value);
            })
            .orElse(new Asset[0]);
    }

    @Override
    public Asset[] getDiscoveredLinkedAttributes(AttributeRef protocolConfigurationRef, FileInfo fileInfo) throws IllegalArgumentException, UnsupportedOperationException, IllegalStateException {
        Optional<Pair<Protocol, AssetAttribute>> protocolAndConfigOptional = getProtocolAndConfig(protocolConfigurationRef);

        if (!protocolAndConfigOptional.isPresent()) {
            throw new IllegalArgumentException("Protocol not found for: " + protocolConfigurationRef);
        }

        Pair<Protocol, AssetAttribute> protocolAndConfig = protocolAndConfigOptional.get();

        // Check protocol is of correct type
        if (!(protocolAndConfigOptional.get().key instanceof ProtocolLinkedAttributeImport)) {
            LOG.info("Protocol not of type '" + ProtocolLinkedAttributeImport.class.getSimpleName() + "'");
            throw new UnsupportedOperationException("Protocol doesn't support linked attribute import:" + protocolAndConfigOptional.get().key.getProtocolDisplayName());
        }

        ProtocolLinkedAttributeImport discoveryProtocol = (ProtocolLinkedAttributeImport)protocolAndConfig.key;
        return discoveryProtocol.discoverLinkedAssetAttributes(protocolAndConfig.value, fileInfo);
    }

    protected Optional<Pair<Protocol,AssetAttribute>> getProtocolAndConfig(AttributeRef protocolConfigurationRef) {
        return agentService.getProtocolConfiguration(protocolConfigurationRef)
            .map(protocolConfig ->
                // Find protocol
                agentService.protocols
                    .values()
                    .stream()
                    .filter(protocol -> protocol.getProtocolName().equals(protocolConfig.getValueAsString().orElse(null)))
                    .findFirst()
                    .map(protocol -> new Pair<>(protocol, protocolConfig))
                    .orElseGet(() -> {
                        LOG.info("Failed to find protocol configuration and protocol for: " + protocolConfigurationRef);
                        return null;
                    }));
    }
}
