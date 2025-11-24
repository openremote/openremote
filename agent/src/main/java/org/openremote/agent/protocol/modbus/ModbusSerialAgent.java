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
import com.fasterxml.jackson.annotation.JsonValue;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

/**
 * This Modbus serial agent has been tested with USB-UART-RS485 converters with chips CH341 and CP2104. RS232 remains untested.
 */
@Entity
public class ModbusSerialAgent extends ModbusAgent<ModbusSerialAgent, ModbusSerialProtocol> {

    public static final AttributeDescriptor<String> SERIAL_PORT = Agent.SERIAL_PORT.withOptional(false);
    public static final AttributeDescriptor<Integer> BAUD_RATE = Agent.SERIAL_BAUDRATE.withOptional(false);
    public static final AttributeDescriptor<Integer> DATA_BITS = new AttributeDescriptor<>("dataBits", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> STOP_BITS = new AttributeDescriptor<>("stopBits", ValueType.POSITIVE_INTEGER);

    public static final ValueDescriptor<ModbusClientParity> VALUE_MODBUS_PARITY = new ValueDescriptor<>("ModbusParity", ModbusClientParity.class);
    public static final AttributeDescriptor<ModbusClientParity> PARITY = new AttributeDescriptor<>("parity", VALUE_MODBUS_PARITY);

    public enum ModbusClientParity {
        NO,
        ODD,
        EVEN,
        MARK,
        SPACE;

        public static ModbusClientParity fromValue(int value) {
            if (value >= 0 && value < values().length) {
                return values()[value];
            }
            return EVEN; // Default for Modbus RTU
        }
    }

    public static final AgentDescriptor<ModbusSerialAgent, ModbusSerialProtocol, ModbusAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            ModbusSerialAgent.class, ModbusSerialProtocol.class, ModbusAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ModbusSerialAgent() {
    }

    public ModbusSerialAgent(String name) {
        super(name);
    }

    public Optional<String> getSerialPort() {
        return getAttributes().getValue(SERIAL_PORT);
    }

    public Integer getBaudRate() {
        return getAttributes().getValue(BAUD_RATE).orElse(null);
    }

    public Integer getDataBits() {
        return getAttributes().getValue(DATA_BITS).orElse(null);
    }

    public Integer getStopBits() {
        return getAttributes().getValue(STOP_BITS).orElse(null);
    }

    public ModbusClientParity getParity() {
        return getAttribute(PARITY).map(attr -> attr.getValue().orElse(ModbusClientParity.EVEN)).orElse(ModbusClientParity.EVEN);
    }

    public int getParityValue() {
        return getParity().ordinal();
    }

    public Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return getAttributes().getValue(ModbusAgent.DEVICE_CONFIG);
    }

    @Override
    public ModbusSerialProtocol getProtocolInstance() {
        return new ModbusSerialProtocol(this);
    }
}
