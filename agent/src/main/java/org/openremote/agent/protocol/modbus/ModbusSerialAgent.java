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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig;
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit;
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

    public static final ValueDescriptor<StopBits> VALUE_STOPBITS = new ValueDescriptor<>("StopBits", StopBits.class);
    public static final AttributeDescriptor<StopBits> STOP_BITS = new AttributeDescriptor<>("stopBits", VALUE_STOPBITS);

    public static final ValueDescriptor<Paritybit> VALUE_PARITY = new ValueDescriptor<>("Paritybit", Paritybit.class);
    public static final AttributeDescriptor<Paritybit> PARITY = new AttributeDescriptor<>("parity", VALUE_PARITY);

    public enum StopBits {
        @JsonProperty("1")
        ONE(JSerialCommChannelConfig.Stopbits.STOPBITS_1),
        @JsonProperty("1,5")
        ONE_HALF(JSerialCommChannelConfig.Stopbits.STOPBITS_1_5),
        @JsonProperty("2")
        TWO(JSerialCommChannelConfig.Stopbits.STOPBITS_2);

        private final JSerialCommChannelConfig.Stopbits value;

        StopBits(JSerialCommChannelConfig.Stopbits value) {
            this.value = value;
        }

        public JSerialCommChannelConfig.Stopbits toJSerialComm() {
            return value;
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

    public int getBaudRate() {
        return getAttributes().getValue(BAUD_RATE).orElse(9600);
    }

    public int getDataBits() {
        return getAttributes().getValue(DATA_BITS).orElse(8);
    }

    public StopBits getStopBits() {
        return getAttributes().getValue(STOP_BITS).orElse(StopBits.ONE);
    }

    public Paritybit getParity() {
        return getAttributes().getValue(PARITY).orElse(Paritybit.EVEN);
    }

    public Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return getAttributes().getValue(ModbusAgent.DEVICE_CONFIG);
    }

    @Override
    public ModbusSerialProtocol getProtocolInstance() {
        return new ModbusSerialProtocol(this);
    }
}
