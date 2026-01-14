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

import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModbusSerialProtocol extends AbstractModbusProtocol<ModbusSerialProtocol, ModbusSerialAgent> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusSerialProtocol.class);
    private ModbusSerialIOClient client = null;
    private volatile CompletableFuture<ModbusSerialFrame> pendingRequest = null;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 10; // Gap between requests per modbus.org (conservative number)
    private final Object sendLock = new Object();

    public ModbusSerialProtocol(ModbusSerialAgent agent) {
        super(agent);
    }

    @Override
    protected void doStartProtocol(Container container) throws Exception {
        String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
        int baudRate = agent.getBaudRate();
        int dataBits = agent.getDataBits();
        var stopBits = agent.getStopBits();
        var parity = agent.getParity();

        connectionString = "modbus-rtu://" + portName + "?baud=" + baudRate + "&data=" + dataBits + "&stop=" + stopBits + "&parity=" + parity;

        try {
            client = new ModbusSerialIOClient(portName, baudRate, dataBits, stopBits, parity);
            client.addMessageConsumer(this::handleIncomingFrame);
            client.addConnectionStatusConsumer(this::setConnectionStatus);
            client.connect();
            LOG.info("Modbus Serial client created and connection initiated for " + connectionString);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create Modbus Serial client: " + agent, e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }
    }

    @Override
    protected void doStopProtocol(Container container) throws Exception {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        synchronized (requestLock) {
            if (pendingRequest != null) {
                pendingRequest.cancel(true);
                pendingRequest = null;
            }
        }
    }

    @Override
    protected Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return agent.getDeviceConfig();
    }

    @Override
    protected ModbusResponse sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        return sendRequestAndWaitForResponse(unitId, pdu, timeoutMs);
    }

    @Override
    public String getProtocolName() {
        return "Serial";
    }

    protected ModbusSerialFrame sendRequestAndWaitForResponse(int unitId, byte[] pdu, long timeoutMs) throws Exception {
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
                LOG.finest("Sent Modbus Serial request - UnitID: " + unitId + ", FC: 0x" + Integer.toHexString(pdu[0] & 0xFF));

                // Wait for response (NOT holding requestLock so handler can complete the future)
                ModbusSerialFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                LOG.finest("Received Modbus Serial response - UnitID: " + response.getUnitId() + ", FC: 0x" + Integer.toHexString(response.getFunctionCode() & 0xFF));
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

    protected void handleIncomingFrame(ModbusSerialFrame frame) {
        LOG.finest("Received frame - UnitID: " + frame.getUnitId() + ", FC: 0x" + Integer.toHexString(frame.getFunctionCode() & 0xFF));

        synchronized (requestLock) {
            if (pendingRequest != null) {
                pendingRequest.complete(frame);
            } else {
                LOG.warning("Received Modbus Serial response for unknown or timed out transaction ID: " + frame.getUnitId());
            }
        }
    }
}
