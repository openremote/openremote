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

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class ModbusTcpAgent extends ModbusAgent<ModbusTcpAgent, ModbusTcpProtocol>{

    public static final AgentDescriptor<ModbusTcpAgent, ModbusTcpProtocol, ModbusAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            ModbusTcpAgent.class, ModbusTcpProtocol.class, ModbusAgentLink.class
    );

    @NotNull
    public static final AttributeDescriptor<String> HOST = Agent.HOST.withOptional(false);
    @NotNull
    public static final AttributeDescriptor<Integer> PORT = Agent.PORT.withOptional(false);

    public static final AttributeDescriptor<String> ILLEGAL_REGISTERS = new AttributeDescriptor<>("illegalRegisters", ValueType.TEXT);
    public static final AttributeDescriptor<Integer> MAX_REGISTER_LENGTH = new AttributeDescriptor<>("maxRegisterLength", ValueType.POSITIVE_INTEGER);

    public static final ValueDescriptor<ModbusAgent.EndianOrder> VALUE_ENDIAN_ORDER = new ValueDescriptor<>("EndianOrder", ModbusAgent.EndianOrder.class);
    public static final AttributeDescriptor<ModbusAgent.EndianOrder> BYTE_ORDER = new AttributeDescriptor<>("byteOrder", VALUE_ENDIAN_ORDER);
    public static final AttributeDescriptor<ModbusAgent.EndianOrder> WORD_ORDER = new AttributeDescriptor<>("wordOrder", VALUE_ENDIAN_ORDER);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ModbusTcpAgent() {
    }

    public ModbusTcpAgent(String name) {
        super(name);
    }

    public Optional<String> getIllegalRegisters() {
        return getAttributes().getValue(ILLEGAL_REGISTERS);
    }

    public Integer getMaxRegisterLength() {
        return getAttributes().getValue(MAX_REGISTER_LENGTH).orElse(1); // Batch processing disabled by default.
    }

    public ModbusAgent.EndianOrder getByteOrder() {
        return getAttribute(BYTE_ORDER).map(attr -> attr.getValue().orElse(ModbusAgent.EndianOrder.BIG)).orElse(ModbusAgent.EndianOrder.BIG);
    }

    public ModbusAgent.EndianOrder getWordOrder() {
        return getAttribute(WORD_ORDER).map(attr -> attr.getValue().orElse(ModbusAgent.EndianOrder.BIG)).orElse(ModbusAgent.EndianOrder.BIG);
    }

    @Override
    public ModbusTcpProtocol getProtocolInstance() {
        return new ModbusTcpProtocol(this);
    }
}
