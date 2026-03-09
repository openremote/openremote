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
import org.openremote.agent.protocol.io.NettyIOClient;
import org.openremote.agent.protocol.modbus.util.ModbusRTUDecoder;
import org.openremote.agent.protocol.modbus.util.ModbusRTUEncoder;
import org.openremote.agent.protocol.modbus.util.ModbusSerialFrame;
import org.openremote.agent.protocol.serial.SerialIOClient;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Modbus Serial (RTU) protocol implementation.
 * Handles Serial-specific transport: half-duplex communication with timing gaps.
 */
public class ModbusSerialProtocol extends AbstractModbusProtocol<ModbusSerialProtocol, ModbusSerialAgent, ModbusSerialFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusSerialProtocol.class);

    // Single pending request (RTU is half-duplex, no transaction IDs)
    private volatile CompletableFuture<ModbusSerialFrame> pendingRequest = null;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 10; // Gap between requests per modbus.org
    private final Object sendLock = new Object();

    public ModbusSerialProtocol(ModbusSerialAgent agent) {
        super(agent);
    }

    // ========== Transport-specific implementations ==========

    @Override
    protected NettyIOClient<ModbusSerialFrame> doCreateIoClient() throws Exception {
        String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
        int baudRate = agent.getBaudRate();
        int dataBits = agent.getDataBits();
        var stopBits = agent.getStopBits();
        var parity = agent.getParity();

        return new SerialIOClient<>(portName, baudRate, dataBits, stopBits, parity);
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider() {
        return () -> new ChannelHandler[] {
            new ModbusRTUEncoder(),
            new ModbusRTUDecoder(),
            new AbstractNettyIOClient.MessageToMessageDecoder<>(ModbusSerialFrame.class, (AbstractNettyIOClient<ModbusSerialFrame, ?>) client)
        };
    }

    @Override
    protected void onMessageReceived(ModbusSerialFrame frame) {
        LOG.finest(() -> "Received frame - UnitID: " + frame.getUnitId() +
                ", FC: 0x" + Integer.toHexString(frame.getFunctionCode() & 0xFF));

        synchronized (requestLock) {
            if (pendingRequest != null) {
                pendingRequest.complete(frame);
            } else {
                LOG.warning("Received Modbus Serial response with no pending request");
            }
        }
    }

    @Override
    protected void doTransportStop() {
        synchronized (requestLock) {
            if (pendingRequest != null) {
                pendingRequest.cancel(true);
                pendingRequest = null;
            }
        }
    }

    @Override
    public ModbusSerialFrame sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        // sendLock ensures only one request-response cycle can be active at a time
        synchronized (sendLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            // Wait for minimum interval between requests (Modbus RTU requires gaps)
            long timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime;
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                try {
                    Thread.sleep(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for request interval", e);
                }
            }

            // Create frame and future for response
            ModbusSerialFrame request = new ModbusSerialFrame(unitId, pdu);
            CompletableFuture<ModbusSerialFrame> responseFuture = new CompletableFuture<>();

            // Register pending request (use requestLock for thread-safe access)
            synchronized (requestLock) {
                pendingRequest = responseFuture;
            }

            try {
                // Send request
                client.sendMessage(request);
                lastRequestTime = System.currentTimeMillis();
                LOG.finest(() -> "Sent Modbus Serial request - UnitID: " + unitId +
                        ", FC: 0x" + Integer.toHexString(pdu[0] & 0xFF));

                // Wait for response (NOT holding requestLock so handler can complete the future)
                ModbusSerialFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                LOG.finest(() -> "Received Modbus Serial response - UnitID: " + response.getUnitId() +
                        ", FC: 0x" + Integer.toHexString(response.getFunctionCode() & 0xFF));
                return response;
            } catch (TimeoutException e) {
                LOG.warning("Modbus Serial request timeout - UnitID: " + unitId);
                throw e;
            } finally {
                // Clear pending request
                synchronized (requestLock) {
                    pendingRequest = null;
                }
            }
        }
    }

    @Override
    public String getProtocolName() {
        return "Modbus Serial";
    }
}