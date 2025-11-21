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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;
import static org.openremote.agent.protocol.modbus.ModbusAgentLink.ModbusDataType.*;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusTcpProtocol.class);

    private ModbusTcpIOClient client = null;
    private String connectionString;
    private final Object requestLock = new Object();
    private final Map<Integer, CompletableFuture<ModbusTcpFrame>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<Integer, Long> timedOutRequests = new ConcurrentHashMap<>(); // Track timed-out TxIDs with timestamp

    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    @Override
    protected Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return agent.getDeviceConfig();
    }

    @Override
    protected void doStartProtocol(Container container) throws Exception {
        String host = agent.getHost().orElseThrow(() -> new IllegalStateException("Host not configured"));
        int port = agent.getPort().orElseThrow(() -> new IllegalStateException("Port not configured"));
        connectionString = "modbus-tcp://" + host + ":" + port;

        try {
            LOG.info("Creating Modbus TCP client for " + connectionString);

            // Create client
            client = new ModbusTcpIOClient(host, port);

            // Set up message consumer to handle incoming frames
            client.addMessageConsumer(this::handleIncomingFrame);

            // Set up connection status consumer to track connection state
            client.addConnectionStatusConsumer(this::setConnectionStatus);

            // Connect (AbstractNettyIOClient handles retries automatically)
            client.connect();

            LOG.info("Modbus TCP client created and connection initiated for " + connectionString);
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
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        int unitId = agentLink.getUnitId();
        int writeAddress = getOrThrowAgentLinkProperty(Optional.ofNullable(agentLink.getWriteAddress()), "write address");
        int registersCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(1);
        String messageId = "tcp_write_" + unitId + "_" + event.getRef().getId() + "_" + event.getRef().getName() + "_" + writeAddress;

        // Convert 1-based user address to 0-based protocol address
        int protocolAddress = writeAddress - 1;

        try {
            byte[] pdu;

            switch (agentLink.getWriteMemoryArea()) {
                case COIL -> {
                    // Write single coil
                    boolean value = processedValue instanceof Boolean ? (Boolean) processedValue : false;
                    pdu = buildWriteSingleCoilPDU(protocolAddress, value);
                    LOG.fine("Modbus TCP Write Coil - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                }
                case HOLDING -> {
                    if (registersCount == 1) {
                        // Write single register
                        int value = processedValue instanceof Number ? ((Number) processedValue).intValue() : 0;
                        pdu = buildWriteSingleRegisterPDU(protocolAddress, value);
                        LOG.fine("Modbus TCP Write Single Register - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                    } else {
                        // Write multiple registers using shared conversion method
                        byte[] registerData = convertValueToRegisterBytes(processedValue, registersCount, agentLink.getReadValueType(), unitId);
                        pdu = buildWriteMultipleRegistersPDU(protocolAddress, registerData);
                        LOG.fine("Modbus TCP Write Multiple Registers - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), RegisterCount: " + registersCount + ", DataType: " + agentLink.getReadValueType());
                    }
                }
                default -> throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
            }

            // Send request and wait for response
            ModbusTcpFrame response = sendRequestAndWaitForResponse(unitId, pdu, 3000);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                onRequestFailure(messageId, "Modbus TCP write address=" + writeAddress, "Exception code: 0x" + Integer.toHexString(exceptionCode & 0xFF));
                throw new IllegalStateException("Modbus exception: 0x" + Integer.toHexString(exceptionCode & 0xFF));
            }

            LOG.fine("Modbus TCP Write Success - UnitId: " + unitId + ", Address: " + writeAddress);
            onRequestSuccess(messageId);

            // Handle attribute update based on read configuration
            if (event.getSource() != null) {
                // Not a synthetic write (from continuous write task)
                if (agentLink.getReadAddress() == null) {
                    // Write-only: update immediately based on write success
                    Attribute<?> attribute = linkedAttributes.get(event.getRef());
                    if (attribute != null) {
                        @SuppressWarnings("unchecked")
                        Attribute<Object> attr = (Attribute<Object>) attribute;
                        attr.setValue(processedValue);
                    }
                    updateLinkedAttribute(event.getRef(), processedValue);
                    LOG.finest("DEBUG doLinkedAttributeWrite triggered an updateLinkedAttribute: " + event.getRef());
                } else {
                    // Write + Read: trigger immediate read to verify write
                    LOG.finest("Triggering verification read after write for " + event.getRef());
                    triggerImmediateRead(event.getRef(), agentLink);
                }
            }
        } catch (Exception e) {
            String operation = "Modbus TCP write address=" + writeAddress;
            // Log without stack trace for expected exceptions (timeout, connection not ready)
            if (e instanceof TimeoutException || (e instanceof IllegalStateException && "Client not connected".equals(e.getMessage()))) {
                onRequestFailure(messageId, operation, e.getMessage());
            } else {
                onRequestFailure(messageId, operation, e);
            }
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    @Override
    protected void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group, Integer unitId) {
        int effectiveUnitId = unitId != null ? unitId : 1;
        String messageId = "tcp_batch_" + effectiveUnitId + "_" + memoryArea + "_" + batch.startAddress + "_" + batch.quantity;

        try {
            // Convert 1-based user address to 0-based protocol address
            int protocolAddress = batch.startAddress - 1;

            // Build function code for memory area
            byte functionCode = switch (memoryArea) {
                case COIL -> (byte) 0x01;           // Read Coils
                case DISCRETE -> (byte) 0x02;       // Read Discrete Inputs
                case HOLDING -> (byte) 0x03;        // Read Holding Registers
                case INPUT -> (byte) 0x04;          // Read Input Registers
            };

            // Build PDU for read request
            byte[] pdu = buildReadRequestPDU(functionCode, protocolAddress, batch.quantity);

            LOG.fine("Modbus TCP Read Request - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", StartAddress: " + batch.startAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Quantity: " + batch.quantity + ", AttributeCount: " + batch.attributes.size());

            // Send request and wait for response
            ModbusTcpFrame response = sendRequestAndWaitForResponse(effectiveUnitId, pdu, 3000);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity, "Exception code: 0x" + Integer.toHexString(exceptionCode & 0xFF));
                return;
            }

            // Extract data from response PDU
            byte[] data = extractDataFromResponsePDU(response.getPdu(), functionCode);
            if (data == null) {
                onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress, "Invalid response format");
                return;
            }

            LOG.finest("Modbus TCP Read Success - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", DataBytes: " + data.length);
            onRequestSuccess(messageId);

            // Extract values for each attribute in the batch
            for (int i = 0; i < batch.attributes.size(); i++) {
                AttributeRef ref = batch.attributes.get(i);
                int offset = batch.offsets.get(i);
                ModbusAgentLink agentLink = group.get(ref);

                if (agentLink == null) {
                    continue;
                }

                try {
                    int registerCount = Optional.ofNullable(agentLink.getRegistersAmount())
                        .orElse(agentLink.getReadValueType().getRegisterCount());
                    ModbusAgentLink.ModbusDataType dataType = agentLink.getReadValueType();

                    LOG.fine("Extracting value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset + ", ReadAddress: " + agentLink.getReadAddress());

                    // Extract value using shared helper method from AbstractModbusProtocol
                    Object value = extractValueFromBatchResponse(data, offset, registerCount, dataType, effectiveUnitId, functionCode);

                    if (value != null) {
                        LOG.finest("Successfully extracted value for " + ref + " - Value: " + value + ", DataType: " + dataType + ", RegisterCount: " + registerCount);
                        updateLinkedAttribute(ref, value);
                    } else {
                        LOG.warning("Extracted null value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to extract value for " + ref + " at offset " + offset + " (DataType: " + agentLink.getReadValueType() + ", RegisterCount: " + agentLink.getRegistersAmount() + "): " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            String operation = "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity;
            // Log without stack trace for expected exceptions (timeout, connection not ready)
            if (e instanceof TimeoutException || (e instanceof IllegalStateException && "Client not connected".equals(e.getMessage()))) {
                onRequestFailure(messageId, operation, e.getMessage());
            } else {
                onRequestFailure(messageId, operation, e);
            }
        }
    }

    @Override
    public String getProtocolName() {
        return "Modbus TCP Protocol";
    }

    @Override
    public String getProtocolInstanceUri() {
        return connectionString;
    }

    /**
     * Send a Modbus request and wait for response with timeout
     */
    private ModbusTcpFrame sendRequestAndWaitForResponse(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        int transactionId;
        CompletableFuture<ModbusTcpFrame> responseFuture;

        // Critical section: only synchronize sending the request
        synchronized (requestLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            // Get transaction ID and create frame
            transactionId = client.getNextTransactionId();
            ModbusTcpFrame request = new ModbusTcpFrame(transactionId, unitId, pdu);

            // Register pending request
            responseFuture = new CompletableFuture<>();
            pendingRequests.put(transactionId, responseFuture);

            // Send request
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
            // Track this transaction ID as timed-out so we can handle late responses gracefully
            timedOutRequests.put(transactionId, System.currentTimeMillis());
            throw e;
        } finally {
            pendingRequests.remove(transactionId);
        }
    }

    /**
     * Handle incoming Modbus TCP frame from client
     */
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
