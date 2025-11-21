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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusTcpProtocol.class);

    private ModbusTcpIOClient client = null;
    private String connectionString;
    private final Object requestLock = new Object();
    private final Map<Integer, CompletableFuture<ModbusTcpIOClient.ModbusTcpFrame>> pendingRequests = new ConcurrentHashMap<>();

    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    @Override
    protected Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return agent.getDeviceConfig();
    }

    @Override
    protected void doStartProtocol(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTING);

        String host = agent.getHost().orElseThrow(() -> new IllegalStateException("Host not configured"));
        int port = agent.getPort().orElseThrow(() -> new IllegalStateException("Port not configured"));
        connectionString = "modbus-tcp://" + host + ":" + port;

        // Create and connect client
        client = new ModbusTcpIOClient(host, port);

        // Set up message consumer to handle incoming frames
        client.addMessageConsumer(this::handleIncomingFrame);

        // Retry logic with exponential backoff
        int maxRetries = 3;
        int retryDelayMs = 500;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                LOG.info("Attempting to connect to Modbus TCP " + host + ":" + port + " (attempt " + attempt + "/" + maxRetries + ")");

                client.connect();

                // Wait a bit for connection to establish
                Thread.sleep(100);

                if (client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                    setConnectionStatus(ConnectionStatus.CONNECTED);
                    LOG.info("Modbus TCP connection established successfully: " + connectionString);
                    return; // Success
                } else {
                    lastException = new Exception("Client not in connected state: " + client.getConnectionStatus());
                    LOG.warning("Modbus TCP connection failed on attempt " + attempt + "/" + maxRetries + ": " + lastException.getMessage());

                    try {
                        client.disconnect();
                    } catch (Exception closeEx) {
                        LOG.log(Level.FINE, "Error closing failed connection: " + closeEx.getMessage(), closeEx);
                    }
                }
            } catch (Exception e) {
                lastException = e;
                LOG.log(Level.WARNING, "Exception during Modbus TCP connection (attempt " + attempt + "/" + maxRetries + "): " + e.getMessage(), e);

                try {
                    if (client != null) {
                        client.disconnect();
                    }
                } catch (Exception closeEx) {
                    LOG.log(Level.FINE, "Error closing failed connection: " + closeEx.getMessage(), closeEx);
                }
            }

            // Wait before retrying
            if (attempt < maxRetries) {
                try {
                    LOG.info("Waiting " + retryDelayMs + "ms before retry...");
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.warning("Retry delay interrupted, aborting connection attempt");
                    break;
                }
            }
        }

        // All retries exhausted
        LOG.log(Level.SEVERE, "Failed to create Modbus TCP connection after " + maxRetries + " attempts: " + agent);
        setConnectionStatus(ConnectionStatus.ERROR);
        throw lastException != null ? lastException : new Exception("Failed to connect after " + maxRetries + " attempts");
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
                    LOG.info("Modbus TCP Write Coil - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                }
                case HOLDING -> {
                    if (registersCount == 1) {
                        // Write single register
                        int value = processedValue instanceof Number ? ((Number) processedValue).intValue() : 0;
                        pdu = buildWriteSingleRegisterPDU(protocolAddress, value);
                        LOG.info("Modbus TCP Write Single Register - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                    } else {
                        // Write multiple registers
                        byte[] registerData = convertToRegisterBytes(processedValue, registersCount, agentLink.getWriteValueType(), unitId);
                        pdu = buildWriteMultipleRegistersPDU(protocolAddress, registerData);
                        LOG.info("Modbus TCP Write Multiple Registers - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), RegisterCount: " + registersCount + ", DataType: " + agentLink.getWriteValueType());
                    }
                }
                default -> throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
            }

            // Send request and wait for response
            ModbusTcpIOClient.ModbusTcpFrame response = sendRequestAndWaitForResponse(unitId, pdu, 3000);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                LOG.warning("Modbus TCP Write Failed - UnitId: " + unitId + ", Address: " + writeAddress + ", Exception: 0x" + Integer.toHexString(exceptionCode & 0xFF));
                onRequestFailure(messageId, "Modbus TCP write address=" + writeAddress, "Exception code: 0x" + Integer.toHexString(exceptionCode & 0xFF));
                throw new IllegalStateException("Modbus exception: 0x" + Integer.toHexString(exceptionCode & 0xFF));
            }

            LOG.info("Modbus TCP Write Success - UnitId: " + unitId + ", Address: " + writeAddress);
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
            onRequestFailure(messageId, "Modbus TCP write address=" + writeAddress, e);
            throw new RuntimeException(e);
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

            LOG.info("Modbus TCP Read Request - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", StartAddress: " + batch.startAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Quantity: " + batch.quantity + ", AttributeCount: " + batch.attributes.size());

            // Send request and wait for response
            ModbusTcpIOClient.ModbusTcpFrame response = sendRequestAndWaitForResponse(effectiveUnitId, pdu, 3000);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                LOG.warning("Modbus TCP Read Failed - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", Address: " + batch.startAddress + ", Quantity: " + batch.quantity + ", Exception: 0x" + Integer.toHexString(exceptionCode & 0xFF));
                onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity, "Exception code: 0x" + Integer.toHexString(exceptionCode & 0xFF));
                return;
            }

            // Extract data from response PDU
            byte[] data = extractDataFromResponsePDU(response.getPdu(), functionCode);
            if (data == null) {
                LOG.warning("Failed to extract data from Modbus TCP response");
                onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress, "Invalid response format");
                return;
            }

            LOG.info("Modbus TCP Read Success - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", DataBytes: " + data.length);
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

                    // Extract value using helper method
                    Object value = extractValueFromBatchResponse(data, offset, registerCount, dataType, effectiveUnitId, memoryArea);

                    if (value != null) {
                        LOG.info("Successfully extracted value for " + ref + " - Value: " + value + ", DataType: " + dataType + ", RegisterCount: " + registerCount);
                        updateLinkedAttribute(ref, value);
                    } else {
                        LOG.warning("Extracted null value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to extract value for " + ref + " at offset " + offset + " (DataType: " + agentLink.getReadValueType() + ", RegisterCount: " + agentLink.getRegistersAmount() + "): " + e.getMessage(), e);
                }
            }

        } catch (TimeoutException e) {
            LOG.warning("Modbus TCP Read Timeout - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", Address: " + batch.startAddress + ", Quantity: " + batch.quantity);
            onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity, "Timeout");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Modbus TCP Read Exception - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", Address: " + batch.startAddress + ", Quantity: " + batch.quantity + ": " + e.getMessage(), e);
            onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity, e);
        }
    }

    @Override
    protected ScheduledFuture<?> scheduleModbusPollingWriteRequest(AttributeRef ref, ModbusAgentLink agentLink) {
        LOG.fine("Scheduling Modbus Write polling request to execute every " + agentLink.getRequestInterval() + "ms for attributeRef: " + ref);

        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Get current attribute value from linkedAttributes map
                Attribute<?> attribute = linkedAttributes.get(ref);
                if (attribute == null || attribute.getValue().isEmpty()) {
                    LOG.finest("Skipping write poll for " + ref + " - no value available");
                    return;
                }

                Object currentValue = attribute.getValue().orElse(null);
                if (currentValue == null) {
                    return;
                }

                // Create a synthetic AttributeEvent for the write
                AttributeEvent syntheticEvent = new AttributeEvent(ref, currentValue);

                // Perform the write using the existing write logic
                doLinkedAttributeWrite(agentLink, syntheticEvent, currentValue);

                LOG.finest("Write poll executed for " + ref + " with value: " + currentValue);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception during Modbus Write polling for " + ref + ": " + e.getMessage(), e);
            }
        }, 0, agentLink.getRequestInterval(), TimeUnit.MILLISECONDS);
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
    private ModbusTcpIOClient.ModbusTcpFrame sendRequestAndWaitForResponse(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        synchronized (requestLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            // Get transaction ID and create frame
            int transactionId = client.getNextTransactionId();
            ModbusTcpIOClient.ModbusTcpFrame request = new ModbusTcpIOClient.ModbusTcpFrame(transactionId, unitId, pdu);

            // Register pending request
            CompletableFuture<ModbusTcpIOClient.ModbusTcpFrame> responseFuture = new CompletableFuture<>();
            pendingRequests.put(transactionId, responseFuture);

            try {
                // Send request
                client.sendMessage(request);
                LOG.finest(() -> "Sent Modbus TCP request - TxID: " + transactionId + ", UnitID: " + unitId + ", FC: 0x" + Integer.toHexString(pdu[0] & 0xFF));

                // Wait for response
                ModbusTcpIOClient.ModbusTcpFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                LOG.finest(() -> "Received Modbus TCP response - TxID: " + response.getTransactionId() + ", UnitID: " + response.getUnitId() + ", FC: 0x" + Integer.toHexString(response.getFunctionCode() & 0xFF));

                return response;
            } catch (TimeoutException e) {
                LOG.warning("Modbus TCP request timeout - TxID: " + transactionId + ", UnitID: " + unitId);
                throw e;
            } finally {
                pendingRequests.remove(transactionId);
            }
        }
    }

    /**
     * Handle incoming Modbus TCP frame from client
     */
    private void handleIncomingFrame(ModbusTcpIOClient.ModbusTcpFrame frame) {
        LOG.finest(() -> "Received frame - TxID: " + frame.getTransactionId() + ", UnitID: " + frame.getUnitId() + ", FC: 0x" + Integer.toHexString(frame.getFunctionCode() & 0xFF));

        // Find pending request for this transaction ID
        CompletableFuture<ModbusTcpIOClient.ModbusTcpFrame> future = pendingRequests.get(frame.getTransactionId());
        if (future != null) {
            future.complete(frame);
        } else {
            LOG.warning("Received response for unknown transaction ID: " + frame.getTransactionId());
        }
    }

    /**
     * Trigger an immediate read operation to verify a write
     */
    private void triggerImmediateRead(AttributeRef ref, ModbusAgentLink agentLink) {
        scheduledExecutorService.execute(() -> {
            try {
                int registerCount = Optional.ofNullable(agentLink.getRegistersAmount())
                    .orElse(agentLink.getReadValueType().getRegisterCount());
                BatchReadRequest batch = new BatchReadRequest(agentLink.getReadAddress(), registerCount);
                batch.attributes.add(ref);
                batch.offsets.add(0);

                Map<AttributeRef, ModbusAgentLink> tempGroup = new ConcurrentHashMap<>();
                tempGroup.put(ref, agentLink);

                executeBatchRead(batch, agentLink.getReadMemoryArea(), tempGroup, agentLink.getUnitId());
                LOG.finest("Verification read completed for " + ref);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to execute verification read for " + ref + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Extract value from batch response data
     */
    private Object extractValueFromBatchResponse(byte[] data, int offset, int registerCount,
                                                 ModbusAgentLink.ModbusDataType dataType,
                                                 Integer unitId,
                                                 ModbusAgentLink.ReadMemoryArea memoryArea) {
        try {
            // For coils and discrete inputs, data is bit-packed
            if (memoryArea == ModbusAgentLink.ReadMemoryArea.COIL ||
                memoryArea == ModbusAgentLink.ReadMemoryArea.DISCRETE) {
                int byteIndex = offset / 8;
                int bitIndex = offset % 8;
                if (byteIndex < data.length) {
                    return ((data[byteIndex] >> bitIndex) & 0x01) == 1;
                }
                return null;
            }

            // For registers (holding/input), data is word-based (2 bytes per register)
            int byteOffset = offset * 2;
            if (byteOffset + (registerCount * 2) > data.length) {
                LOG.warning("Not enough data in response: need " + (byteOffset + registerCount * 2) + " bytes, have " + data.length);
                return null;
            }

            // Extract register bytes
            byte[] registerBytes = new byte[registerCount * 2];
            System.arraycopy(data, byteOffset, registerBytes, 0, registerBytes.length);

            // Parse multi-register value
            return parseMultiRegisterValue(registerBytes, registerCount, dataType, unitId);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to extract value from batch response at offset " + offset + ": " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert value to register bytes for writing
     */
    private byte[] convertToRegisterBytes(Object value, int registerCount, ModbusAgentLink.ModbusDataType dataType, Integer unitId) {
        byte[] bytes = new byte[registerCount * 2];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Apply byte order
        EndianFormat endianFormat = getEndianFormat(unitId);
        buffer.order(endianFormat == EndianFormat.BIG_ENDIAN || endianFormat == EndianFormat.BIG_ENDIAN_BYTE_SWAP
            ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // Convert based on data type
        switch (dataType) {
            case INT16 -> buffer.putShort(((Number) value).shortValue());
            case UINT16 -> buffer.putShort((short) (((Number) value).intValue() & 0xFFFF));
            case INT32 -> buffer.putInt(((Number) value).intValue());
            case UINT32 -> buffer.putInt((int) (((Number) value).longValue() & 0xFFFFFFFFL));
            case INT64 -> buffer.putLong(((Number) value).longValue());
            case FLOAT32 -> buffer.putFloat(((Number) value).floatValue());
            case FLOAT64 -> buffer.putDouble(((Number) value).doubleValue());
            default -> throw new IllegalArgumentException("Unsupported write data type: " + dataType);
        }

        // Apply byte swap if needed
        if (endianFormat == EndianFormat.BIG_ENDIAN_BYTE_SWAP || endianFormat == EndianFormat.LITTLE_ENDIAN_BYTE_SWAP) {
            for (int i = 0; i < bytes.length; i += 2) {
                byte temp = bytes[i];
                bytes[i] = bytes[i + 1];
                bytes[i + 1] = temp;
            }
        }

        return bytes;
    }
}
