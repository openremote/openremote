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

import org.openremote.agent.protocol.io.IoClient;
import org.openremote.controller.exception.ConfigurationException;
import org.openremote.controller.protocol.zwave.ZWaveCommandBuilder;
import org.openremote.controller.utils.Logger;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.protocol.zwave.LoggerUtil;
import org.openremote.protocol.zwave.port.SessionLayer;
import org.openremote.protocol.zwave.port.TransportLayer;
import org.openremote.protocol.zwave.port.TransportLayerListener;

/**
 * Serial port implementation based on the Netty framework.
 *
 * @see TransportLayer
 * @see TransportLayerListener
 * @see SessionLayer
 *
 * @author <a href="mailto:rainer@openremote.org">Rainer Hitz</a>
 */
public class NettyTransportLayer implements TransportLayer {

    // Constants ----------------------------------------------------------------------------------

    public static final String SERIAL_PORT_LOG_HEADER      = "Serial_Port                 : ";


    // Class Members ------------------------------------------------------------------------------

    /**
     * Z-Wave logger. Uses a common category for all Z-Wave related logging.
     */
    private final static Logger log = Logger.getLogger(ZWaveCommandBuilder.ZWAVE_LOG_CATEGORY);


    // Private Instance Fields --------------------------------------------------------------------

    private final IoClient<SerialDataPacket> ioClient;

    private volatile TransportLayerListener listener;


    // Constructors -------------------------------------------------------------------------------

    public NettyTransportLayer(IoClient<SerialDataPacket> ioClient) {
        if (ioClient == null) {
            throw new IllegalArgumentException("Missing I/O client.");
        }
        this.ioClient = ioClient;
        this.ioClient.addMessageConsumer(this::onPacketReceived);
        this.ioClient.addConnectionStatusConsumer(this::onConnectionStatusUpdate);
    }


    // Implements TransportLayer ------------------------------------------------------------------

    @Override
    public void open() throws ConfigurationException {
        try {
            ioClient.connect();
        } catch (Exception e) {
            String serialPort = "";
            if (ioClient instanceof ZWSerialClient) {
                serialPort = ((ZWSerialClient) ioClient).getSerialPort();
            }
            ConfigurationException configException = new ConfigurationException("Failed to open serial port " + serialPort, e);
            log.warn(configException.getMessage(), configException);
            throw configException;
        }
    }

    @Override
    public void close() {
        ioClient.disconnect();
    }

    @Override
    public void setListener(TransportLayerListener listener) {
        this.listener = listener;
    }

    @Override
    public void write(byte[] data) {
        if (ioClient.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            logDataBytes(data, true);
            if (data.length > 0) {
                SerialDataPacket dataPacket = new SerialDataPacket(data);
                ioClient.sendMessage(dataPacket);
            }
        }
    }


    // Protected Instance Methods -----------------------------------------------------------------

    protected void onPacketReceived(SerialDataPacket packet) {
        if (listener != null) {
            byte[] data = packet.getData();
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
                case ERROR_CONFIGURATION:
                    listener.onConnectionStatusChanged(org.openremote.protocol.zwave.port.ConnectionStatus.ERROR_CONFIGURATION);
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
