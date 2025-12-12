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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusTcpProtocol.class);
    private ModbusTcpIOClient client = null;
    private final Map<Integer, CompletableFuture<ModbusTcpFrame>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<Integer, Long> timedOutRequests = new ConcurrentHashMap<>(); // Track timed-out TxIDs with timestamp

    public ModbusTcpProtocol(ModbusTcpAgent agent) {super(agent);}

    @Override
    protected void doStartProtocol(Container container) throws Exception {
        String host = agent.getHost().orElseThrow(() -> new IllegalStateException("Host not configured"));
        int port = agent.getPort().orElseThrow(() -> new IllegalStateException("Port not configured"));
        connectionString = "modbus-tcp://" + host + ":" + port;

        try {
            client = new ModbusTcpIOClient(host, port);
            client.addMessageConsumer(this::handleIncomingFrame);
            client.addConnectionStatusConsumer(this::setConnectionStatus);
            client.connect();
            LOG.info("Modbus TCP connection initiated for " + connectionString);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create Modbus TCP client: " + agent, e);
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
        pendingRequests.clear();
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
        return "TCP";
    }

    private ModbusTcpFrame sendRequestAndWaitForResponse(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        int transactionId;
        CompletableFuture<ModbusTcpFrame> responseFuture;

        // Synchronize sending the request
        synchronized (requestLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            transactionId = client.getNextTransactionId();
            ModbusTcpFrame request = new ModbusTcpFrame(transactionId, unitId, pdu);
            responseFuture = new CompletableFuture<>();
            pendingRequests.put(transactionId, responseFuture);

            client.sendMessage(request);
            LOG.finest(() -> "Sent Modbus TCP request - TxID: " + transactionId + ", UnitID: " + unitId + ", FC: 0x" + Integer.toHexString(pdu[0] & 0xFF));
        }

        // Wait for response outside synchronized block to avoid blocking other threads
        try {
            ModbusTcpFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            LOG.finest(() -> "Received Modbus TCP response - TxID: " + response.getTransactionId() + ", UnitID: " + response.getUnitId() + ", FC: 0x" + Integer.toHexString(response.getFunctionCode() & 0xFF));
            return response;
        } catch (TimeoutException e) {
            LOG.warning("Modbus TCP request timeout - TxID: " + transactionId + ", UnitID: " + unitId);
            timedOutRequests.put(transactionId, System.currentTimeMillis());
            throw e;
        } finally {
            pendingRequests.remove(transactionId);
        }
    }

    private void handleIncomingFrame(ModbusTcpFrame frame) {
        LOG.finest(() -> "Received frame - TxID: " + frame.getTransactionId() + ", UnitID: " + frame.getUnitId() + ", FC: 0x" + Integer.toHexString(frame.getFunctionCode() & 0xFF));

        int txId = frame.getTransactionId();

        // Find pending request for this transaction ID
        CompletableFuture<ModbusTcpFrame> future = pendingRequests.get(txId);
        if (future != null) {
            future.complete(frame);
        } else {
            // Check if this was a timed-out request
            Long timeoutTime = timedOutRequests.remove(txId);
            if (timeoutTime != null) {
                long latency = System.currentTimeMillis() - timeoutTime;
                LOG.warning("Received late response for timed-out transaction ID " + txId + " (arrived " + latency + "ms after timeout)");
            } else {
                LOG.warning("Received response for unknown transaction ID: " + txId);
            }
        }

        // Clean up old timed-out requests (older than 30 seconds)
        long now = System.currentTimeMillis();
        timedOutRequests.entrySet().removeIf(entry -> (now - entry.getValue()) > 30000);
    }

}
