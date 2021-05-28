/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.agent.protocol.zwave;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.serial.SerialIOClient;
import org.openremote.controller.protocol.zwave.ZWaveCommandBuilder;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.protocol.zwave.LoggerUtil;
import org.openremote.protocol.zwave.port.TransportLayer;
import org.openremote.protocol.zwave.port.TransportLayerListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around {@link SerialIOClient} to allow compatibility with Z Wave library
 */
public class ZWaveSerialIOClient extends SerialIOClient<byte[]> implements TransportLayer {

    // Constants ----------------------------------------------------------------------------------

    public static final String SERIAL_PORT_LOG_HEADER      = "Serial_Port                 : ";


    // Class Members ------------------------------------------------------------------------------

    /**
     * Z-Wave logger. Uses a common category for all Z-Wave related logging.
     */

    private final static Logger log = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ZWaveCommandBuilder.ZWAVE_LOG_CATEGORY);

    // Private Instance Fields --------------------------------------------------------------------

    private volatile TransportLayerListener listener;

    // Constructors -------------------------------------------------------------------------------

    public ZWaveSerialIOClient(String port) {
        super(port, 115200);

        setEncoderDecoderProvider(
            () -> new ChannelHandler[] {
                new ZWavePacketEncoder(),
                new ZWavePacketDecoder(),
                new AbstractNettyIOClient.MessageToMessageDecoder<>(byte[].class, this)
            }
        );

        addMessageConsumer(this::onPacketReceived);
        addConnectionStatusConsumer(this::onConnectionStatusUpdate);
    }

    // Implements TransportLayer ------------------------------------------------------------------

    @Override
    public void open() {
        try {
            connect();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to open serial port: " + getClientUri(), e);
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    @Override
    public void setListener(TransportLayerListener listener) {
        this.listener = listener;
    }

    @Override
    public void write(byte[] data) {
        sendMessage(data);
    }

    // Protected Instance Methods -----------------------------------------------------------------

    protected void onPacketReceived(byte[] data) {
        if (listener != null) {
            logDataBytes(data, false);
            listener.dataReceived(data);
        }
    }

    protected void onConnectionStatusUpdate(ConnectionStatus connectionStatus) {
        if (listener != null) {
            switch(connectionStatus) {
                case CONNECTING:
                    listener.onConnectionStatusChanged(org.openremote.protocol.zwave.port.ConnectionStatus.CONNECTING);
                    break;
                case CONNECTED:
                    listener.onConnectionStatusChanged(org.openremote.protocol.zwave.port.ConnectionStatus.CONNECTED);
                    break;
                case DISCONNECTING:
                    listener.onConnectionStatusChanged(org.openremote.protocol.zwave.port.ConnectionStatus.DISCONNECTING);
                    break;
                case DISCONNECTED:
                    listener.onConnectionStatusChanged(org.openremote.protocol.zwave.port.ConnectionStatus.DISCONNECTED);
                    break;
                case ERROR:
                    listener.onConnectionStatusChanged(org.openremote.protocol.zwave.port.ConnectionStatus.ERROR);
                    break;
            }
        }
    }

    private void logDataBytes(byte[] data, boolean isTransmit) {
        if (data == null || data.length == 0) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for(int i = 0; i < data.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(String.format("0x%02X", data[i] & 0xFF));
        }

        builder.append("]");

        LoggerUtil.debug(
            SERIAL_PORT_LOG_HEADER +
            (isTransmit ? "Data bytes transmitted (TX) : {0}" : "Data bytes received (RX) : {0}"),
            builder.toString()
        );
    }
}
