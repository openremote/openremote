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

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.serial.SerialIOClient;
import org.openremote.model.syslog.SyslogCategory;

import java.util.logging.Logger;

import static org.openremote.agent.protocol.serial.JSerialCommChannelOption.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Modbus RTU serial client that wraps SerialIOClient with Modbus RTU frame encoding/decoding
 */
public class ModbusSerialIOClient extends SerialIOClient<ModbusSerialFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusSerialIOClient.class);

    private final int dataBits;
    private final int stopBits;
    private final int parity;

    public ModbusSerialIOClient(String port, int baudRate, int dataBits, int stopBits, int parity) {
        super(port, baudRate);
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;

        // Set up Modbus RTU encoder and decoder
        setEncoderDecoderProvider(
            () -> new ChannelHandler[] {
                new ModbusRTUEncoder(),
                new ModbusRTUDecoder(),
                new AbstractNettyIOClient.MessageToMessageDecoder<>(ModbusSerialFrame.class, this)
            }
        );
    }

    @Override
    protected void configureChannel() {
        super.configureChannel();

        // Configure serial port parameters
        bootstrap.option(DATA_BITS, dataBits);
        bootstrap.option(STOP_BITS, convertStopBits(stopBits));
        bootstrap.option(PARITY_BIT, convertParity(parity));
    }

    private org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits convertStopBits(int stopBits) {
        switch (stopBits) {
            case 1:
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits.STOPBITS_1;
            case 2:
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits.STOPBITS_2;
            default:
                LOG.warning("Invalid stop bits value: " + stopBits + ", defaulting to 1");
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits.STOPBITS_1;
        }
    }

    private org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit convertParity(int parity) {
        // Convert parity integer to enum (matches jSerialComm/ModbusSerialAgent values)
        switch (parity) {
            case 0: // NO_PARITY
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.NONE;
            case 1: // ODD_PARITY
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.ODD;
            case 2: // EVEN_PARITY
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.EVEN;
            case 3: // MARK_PARITY
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.MARK;
            case 4: // SPACE_PARITY
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.SPACE;
            default:
                LOG.warning("Invalid parity value: " + parity + ", defaulting to EVEN");
                return org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.EVEN;
        }
    }

    @Override
    public String getClientUri() {
        return "modbus-rtu://" + port;
    }
}
