/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.modbus;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;

@Entity
public class ModbusTcpAgent extends ModbusAgent<ModbusTcpAgent, ModbusTcpProtocol>{

    public static final AgentDescriptor<ModbusTcpAgent, ModbusTcpProtocol, ModbusAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            ModbusTcpAgent.class, ModbusTcpProtocol.class, ModbusAgentLink.class
    );

    @NotNull
    public static final AttributeDescriptor<String> HOST = Agent.HOST.withOptional(false);
    @NotNull
    public static final AttributeDescriptor<Integer> PORT = Agent.PORT.withOptional(false);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ModbusTcpAgent() {
    }

    public ModbusTcpAgent(String name) {
        super(name);
    }

    @Override
    public ModbusTcpProtocol getProtocolInstance() {
        return new ModbusTcpProtocol(this);
    }
}
