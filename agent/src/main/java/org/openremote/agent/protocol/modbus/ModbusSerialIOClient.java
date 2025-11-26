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
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit;
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits;
import org.openremote.agent.protocol.serial.SerialIOClient;
import static org.openremote.agent.protocol.serial.JSerialCommChannelOption.*;

public class ModbusSerialIOClient extends SerialIOClient<ModbusSerialFrame> {

    private final int dataBits;
    private final Stopbits stopBits;
    private final Paritybit parity;

    public ModbusSerialIOClient(String port, int baudRate, int dataBits, Stopbits stopBits, Paritybit parity) {
        super(port, baudRate);
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;

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
        bootstrap.option(DATA_BITS, dataBits);
        bootstrap.option(STOP_BITS, stopBits);
        bootstrap.option(PARITY_BIT, parity);
    }

    @Override
    public String getClientUri() {
        return "modbus-rtu://" + port;
    }
}
