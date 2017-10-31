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
package org.openremote.agent.protocol;

import gnu.io.NRSerialPort;
import gnu.io.SerialPort;
import io.netty.channel.ConnectTimeoutException;

import java.net.SocketAddress;

@SuppressWarnings("deprecation")
public class NrJavaSerialChannel extends io.netty.channel.rxtx.RxtxChannel {

    private boolean open = true;
    protected SerialPort serialPort;
    protected NrJavaSerialAddress deviceAddress;

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        NrJavaSerialAddress remote = (NrJavaSerialAddress) remoteAddress;
        final NRSerialPort serial = new NRSerialPort(remote.value(), remote.getBaudRate());
        serial.connect();
        serialPort = serial.getSerialPortInstance();
        if (serialPort == null) {
            // No exception is thrown on connect failure so throw one
            throw new ConnectTimeoutException("Failed to establish connection to COM port: " + remote.value());
        }
        serialPort.enableReceiveTimeout(config().getOption(io.netty.channel.rxtx.RxtxChannelOption.READ_TIMEOUT));
        deviceAddress = remote;
    }

    @Override
    protected void doInit() throws Exception {
        serialPort.setSerialPortParams(
            config().getOption(io.netty.channel.rxtx.RxtxChannelOption.BAUD_RATE),
            config().getOption(io.netty.channel.rxtx.RxtxChannelOption.DATA_BITS).value(),
            config().getOption(io.netty.channel.rxtx.RxtxChannelOption.STOP_BITS).value(),
            config().getOption(io.netty.channel.rxtx.RxtxChannelOption.PARITY_BIT).value()
        );
        serialPort.setDTR(config().getOption(io.netty.channel.rxtx.RxtxChannelOption.DTR));
        serialPort.setRTS(config().getOption(io.netty.channel.rxtx.RxtxChannelOption.RTS));

        activate(serialPort.getInputStream(), serialPort.getOutputStream());
    }

    @Override
    protected void doClose() throws Exception {
        open = false;
        try {
            super.doClose();
        } finally {
            if (serialPort != null) {
                serialPort.removeEventListener();
                serialPort.close();
                serialPort = null;
            }
        }
    }
}
