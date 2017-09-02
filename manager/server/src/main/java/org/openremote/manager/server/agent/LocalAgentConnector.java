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
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;

import java.util.Collections;

public class LocalAgentConnector implements AgentConnector {

    protected AgentService agentService;

    public LocalAgentConnector(AgentService agentService) {
        this.agentService = agentService;
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
}
