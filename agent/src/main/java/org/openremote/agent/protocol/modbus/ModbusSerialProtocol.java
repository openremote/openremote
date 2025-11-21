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

import com.fazecast.jSerialComm.SerialPort;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ModbusSerialProtocol extends AbstractModbusProtocol<ModbusSerialProtocol, ModbusSerialAgent> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusSerialProtocol.class);

    private SerialPort serialPort;
    private String connectionString;
    private final Object serialLock = new Object(); // Synchronization lock for serial port operations

    public ModbusSerialProtocol(ModbusSerialAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return "Modbus Serial Protocol";
    }

    @Override
    public String getProtocolInstanceUri() {
        return connectionString != null ? connectionString : "modbus-rtu://" + agent.getSerialPort().orElse("unknown");
    }

    @Override
    protected Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return agent.getDeviceConfig();
    }

    /**
     * Factory method for creating SerialPort instance.
     * This method can be overridden in tests to provide a mock serial port.
     */
    protected SerialPort createSerialPort(String portName) {
        return SerialPort.getCommPort(portName);
    }

    @Override
    protected void doStartProtocol(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTING);

        String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
        int baudRate = agent.getBaudRate();
        int dataBits = agent.getDataBits();
        int stopBits = agent.getStopBits();
        int parity = agent.getParityValue();

        connectionString = "modbus-rtu://" + portName + "?baud=" + baudRate + "&data=" + dataBits + "&stop=" + stopBits + "&parity=" + parity;

        try {
            // Open serial port directly (no shared port management)
            serialPort = createSerialPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(dataBits);
            serialPort.setNumStopBits(stopBits);
            serialPort.setParity(mapParityToSerialPort(parity));
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);

            if (serialPort.openPort()) {
                Thread.sleep(100); // Delay to allow port initialization
                setConnectionStatus(ConnectionStatus.CONNECTED);
                LOG.info("Modbus Serial Protocol started successfully: " + connectionString);
                return; // Success
            } else {
                LOG.warning("Failed to open serial port: " + portName);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Exception during serial port opening: " + e.getMessage(), e);
        }

        // Failed to open port
        LOG.log(Level.SEVERE, "Failed to start Modbus Serial Protocol for " + connectionString);
        setConnectionStatus(ConnectionStatus.ERROR);
    }

    @Override
    protected void doStopProtocol(Container container) throws Exception {
        // Close serial port directly
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            LOG.info("Closed Modbus Serial port: " + agent.getSerialPort().orElse("unknown"));
        }
        serialPort = null;

        setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        int unitId = getOrThrowAgentLinkProperty(Optional.ofNullable(agentLink.getUnitId()), "unit ID");
        int writeAddress = getOrThrowAgentLinkProperty(Optional.ofNullable(agentLink.getWriteAddress()), "write address");
        int registersCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(1);

        // Convert from 1-based user address to 0-based protocol address
        int protocolAddress = writeAddress - 1;

        boolean writeSuccess = false;
        try {
            switch (agentLink.getWriteMemoryArea()) {
                case COIL:
                    writeSuccess = writeSingleCoil(unitId, protocolAddress, (Boolean) processedValue);
                    break;
                case HOLDING:
                    if (registersCount > 1) {
                        writeSuccess = writeMultipleHoldingRegisters(unitId, protocolAddress, registersCount, processedValue, agentLink);
                    } else {
                        writeSuccess = writeSingleHoldingRegister(unitId, protocolAddress, processedValue);
                    }
                    break;
                default:
                    throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
            }

            // Handle attribute update based on read configuration
            if (writeSuccess && event.getSource() != null) {
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
                } else {
                    // Write + Read: trigger immediate read to verify write
                    LOG.finest("Triggering verification read after write for " + event.getRef());
                    triggerImmediateRead(event.getRef(), agentLink);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error writing to Modbus device: " + e.getMessage(), e);
        }
    }
    
    private boolean writeSingleCoil(int unitId, int address, boolean value) {
        // Synchronize to ensure atomic write-read cycles
        synchronized (serialLock) {
            String messageId = "write_coil_" + unitId + "_" + address;
            try {
                byte[] request = createWriteCoilRequest(unitId, (byte) 0x05, address, value);

                int bytesWritten = serialPort.writeBytes(request, request.length);
                if (bytesWritten != request.length) {
                    LOG.warning("Incomplete write: " + bytesWritten + " of " + request.length + " bytes");
                    return false;
                }

                // Read response (8 bytes for write single coil)
                byte[] response = new byte[8];
                int totalBytesRead = readWithTimeout(response, 50);

                if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                    int exceptionCode = response[2] & 0xFF;
                    onRequestFailure(messageId, "Write single coil address=" + address,
                        "Exception code: " + exceptionCode);
                    return false;
                }
                onRequestSuccess(messageId);
                return true;

            } catch (Exception e) {
                onRequestFailure(messageId, "Write single coil address=" + address, e);
                return false;
            }
        }
    }
    
    private boolean writeSingleHoldingRegister(int unitId, int address, Object value) {
        // Synchronize to ensure atomic write-read cycles
        synchronized (serialLock) {
            String messageId = "write_holding_" + unitId + "_" + address;
            try {
                int registerValue;
                if (value instanceof Integer) {
                    registerValue = (Integer) value;
                } else if (value instanceof Number) {
                    registerValue = ((Number) value).intValue();
                } else {
                    throw new IllegalArgumentException("Cannot convert value to register value: " + value);
                }

                byte[] request = createWriteRegisterRequest(unitId, (byte) 0x06, address, registerValue);

                int bytesWritten = serialPort.writeBytes(request, request.length);
                if (bytesWritten != request.length) {
                    LOG.warning("Incomplete write: " + bytesWritten + " of " + request.length + " bytes");
                    return false;
                }

                // Read response (8 bytes for write single register)
                byte[] response = new byte[8];
                int totalBytesRead = readWithTimeout(response, 50);

                if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                    int exceptionCode = response[2] & 0xFF;
                    onRequestFailure(messageId, "Write single register address=" + address,
                        "Exception code: " + exceptionCode);
                    return false;
                }
                onRequestSuccess(messageId);
                return true;

            } catch (Exception e) {
                onRequestFailure(messageId, "Write single register address=" + address, e);
                return false;
            }
        }
    }

    private boolean writeMultipleHoldingRegisters(int unitId, int address, int quantity, Object value, ModbusAgentLink agentLink) {
        // Synchronize to ensure atomic write-read cycles
        synchronized (serialLock) {
            String messageId = "write_multiple_" + unitId + "_" + address + "_" + quantity;
            try {
                // Convert value to byte array based on quantity, using unitId-specific endian format
                byte[] registerData = convertValueToRegisters(value, quantity, unitId);

                byte[] request = createWriteMultipleRegistersRequest(unitId, (byte) 0x10, address, quantity, registerData);

                int bytesWritten = serialPort.writeBytes(request, request.length);
                if (bytesWritten != request.length) {
                    LOG.warning("Incomplete write: " + bytesWritten + " of " + request.length + " bytes");
                    return false;
                }

                // Read response (8 bytes for write multiple registers)
                byte[] response = new byte[8];
                int totalBytesRead = readWithTimeout(response, 50);

                if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                    int exceptionCode = response[2] & 0xFF;
                    onRequestFailure(messageId, "Write multiple registers address=" + address + " quantity=" + quantity,
                        "Exception code: " + exceptionCode);
                    return false;
                }
                onRequestSuccess(messageId);
                return true;

            } catch (Exception e) {
                onRequestFailure(messageId, "Write multiple registers address=" + address + " quantity=" + quantity, e);
                return false;
            }
        }
    }
    
    /**
     * Flush any remaining data from the serial buffer.
     * Should be called on error paths before releasing the synchronization lock.
     */
    private void flushSerialBuffer() {
        try {
            int available = serialPort.bytesAvailable();
            if (available > 0) {
                byte[] flush = new byte[available];
                serialPort.readBytes(flush, available, 0);
                LOG.fine("Flushed " + available + " stale bytes from serial buffer");
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error flushing serial buffer: " + e.getMessage(), e);
        }
    }

    private int readWithTimeout(byte[] buffer, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int totalBytesRead = 0;

        while (totalBytesRead < buffer.length && (System.currentTimeMillis() - startTime) < timeoutMs) {
            int available = serialPort.bytesAvailable();
            if (available > 0) {
                int bytesRead = serialPort.readBytes(buffer, buffer.length - totalBytesRead, totalBytesRead);
                if (bytesRead > 0) { totalBytesRead += bytesRead; }
            }
            Thread.sleep(5);
        }

        return totalBytesRead;
    }
    
    /**
     * Wrap a Modbus PDU in RTU framing (Unit ID + PDU + CRC16).
     * This is the serial-specific framing, while PDU building is shared with TCP.
     */
    private byte[] buildRTUFrame(int unitId, byte[] pdu) {
        byte[] frame = new byte[1 + pdu.length + 2]; // unitId + PDU + CRC
        frame[0] = (byte) unitId;
        System.arraycopy(pdu, 0, frame, 1, pdu.length);

        int crc = calculateCRC16(frame, 0, 1 + pdu.length);
        frame[1 + pdu.length] = (byte) (crc & 0xFF);
        frame[2 + pdu.length] = (byte) (crc >> 8);

        return frame;
    }

    private byte[] createModbusRequest(int unitId, byte functionCode, int startAddress, int quantity) {
        // Use shared PDU building method from AbstractModbusProtocol
        byte[] pdu = buildReadRequestPDU(functionCode, startAddress, quantity);
        return buildRTUFrame(unitId, pdu);
    }

    private byte[] createWriteCoilRequest(int unitId, byte functionCode, int coilAddress, boolean value) {
        // Use shared PDU building method from AbstractModbusProtocol
        byte[] pdu = buildWriteSingleCoilPDU(coilAddress, value);
        return buildRTUFrame(unitId, pdu);
    }

    private byte[] createWriteRegisterRequest(int unitId, byte functionCode, int registerAddress, int value) {
        // Use shared PDU building method from AbstractModbusProtocol
        byte[] pdu = buildWriteSingleRegisterPDU(registerAddress, value);
        return buildRTUFrame(unitId, pdu);
    }

    private byte[] createWriteMultipleRegistersRequest(int unitId, byte functionCode, int startAddress, int quantity, byte[] registerData) {
        // Use shared PDU building method from AbstractModbusProtocol
        byte[] pdu = buildWriteMultipleRegistersPDU(startAddress, registerData);
        return buildRTUFrame(unitId, pdu);
    }

    /**
     * Convert a value to register bytes based on the number of registers.
     * Applies byte and word order based on unitId-specific configuration.
     */
    private byte[] convertValueToRegisters(Object value, int registerCount, Integer unitId) {
        byte[] data;

        if (registerCount == 1) {
            // Single register (16-bit)
            int intValue;
            if (value instanceof Integer) {
                intValue = (Integer) value;
            } else if (value instanceof Number) {
                intValue = ((Number) value).intValue();
            } else {
                throw new IllegalArgumentException("Cannot convert value to register: " + value);
            }
            data = new byte[2];
            data[0] = (byte) (intValue >> 8);
            data[1] = (byte) (intValue & 0xFF);
        } else if (registerCount == 2) {
            // Two registers (32-bit int or float)
            data = new byte[4];
            if (value instanceof Float) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(4);
                buffer.order(getJavaByteOrder(unitId));
                buffer.putFloat((Float) value);
                data = buffer.array();
            } else if (value instanceof Integer) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(4);
                buffer.order(getJavaByteOrder(unitId));
                buffer.putInt((Integer) value);
                data = buffer.array();
            } else if (value instanceof Number) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(4);
                buffer.order(getJavaByteOrder(unitId));
                buffer.putInt(((Number) value).intValue());
                data = buffer.array();
            } else {
                throw new IllegalArgumentException("Cannot convert value to 2 registers: " + value);
            }
            // Apply word order
            data = applyWordOrder(data, 2, unitId);
        } else if (registerCount == 4) {
            // Four registers (64-bit int or double)
            data = new byte[8];
            if (value instanceof Double) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(8);
                buffer.order(getJavaByteOrder(unitId));
                buffer.putDouble((Double) value);
                data = buffer.array();
            } else if (value instanceof Long) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(8);
                buffer.order(getJavaByteOrder(unitId));
                buffer.putLong((Long) value);
                data = buffer.array();
            } else if (value instanceof Number) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(8);
                buffer.order(getJavaByteOrder(unitId));
                buffer.putLong(((Number) value).longValue());
                data = buffer.array();
            } else {
                throw new IllegalArgumentException("Cannot convert value to 4 registers: " + value);
            }
            // Apply word order
            data = applyWordOrder(data, 4, unitId);
        } else {
            throw new IllegalArgumentException("Unsupported register count for write: " + registerCount);
        }

        return data;
    }
    
    private int calculateCRC16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = crc >> 1;
                }
            }
        }
        return crc;
    }
    
    /**
     * Map parity integer value to jSerialComm parity constant
     * @param parity 0=NONE, 1=ODD, 2=EVEN, 3=MARK, 4=SPACE
     * @return jSerialComm parity constant
     */
    private int mapParityToSerialPort(int parity) {
        switch (parity) {
            case 0:
                return SerialPort.NO_PARITY;
            case 1:
                return SerialPort.ODD_PARITY;
            case 2:
                return SerialPort.EVEN_PARITY;
            case 3:
                return SerialPort.MARK_PARITY;
            case 4:
                return SerialPort.SPACE_PARITY;
            default:
                LOG.warning("Unknown parity value " + parity + ", defaulting to EVEN");
                return SerialPort.EVEN_PARITY; // Default for Modbus RTU
        }
    }

    /**
     * Execute a batch read request and distribute values to attributes
     */
    @Override
    protected void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group, Integer unitId) {
        byte functionCode;
        switch (memoryArea) {
            case COIL:
                functionCode = 0x01;
                break;
            case DISCRETE:
                functionCode = 0x02;
                break;
            case HOLDING:
                functionCode = 0x03;
                break;
            case INPUT:
                functionCode = 0x04;
                break;
            default:
                throw new IllegalArgumentException("Unsupported read memory area: " + memoryArea);
        }

        // Convert from 1-based user address to 0-based protocol address
        int protocolAddress = batch.startAddress - 1;

        // Perform the batch read
        byte[] response = performModbusBatchRead(unitId, functionCode, protocolAddress, batch.quantity);

        if (response == null) {
            return;
        }

        // Distribute values to each attribute in the batch
        for (int i = 0; i < batch.attributes.size(); i++) {
            AttributeRef attrRef = batch.attributes.get(i);
            int offset = batch.offsets.get(i);
            ModbusAgentLink agentLink = group.get(attrRef);

            if (agentLink == null) {
                continue;
            }

            try {
                Object value = extractValueFromBatchResponse(response, offset, agentLink.getReadValueType(), functionCode, unitId);
                if (value != null) {
                    updateLinkedAttribute(attrRef, value);
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error extracting value for " + attrRef + " from batch: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Perform a batch Modbus read and return the raw response data
     */
    private byte[] performModbusBatchRead(int unitId, byte functionCode, int address, int quantity) {
        // Synchronize to ensure atomic write-read cycles
        synchronized (serialLock) {
            String messageId = "batch_" + unitId + "_" + functionCode + "_" + address + "_" + quantity;
            LOG.fine("Performing batch Modbus read: unit=" + unitId + ", function=" + functionCode + ", address=" + address + ", quantity=" + quantity);
            try {
                byte[] request = createModbusRequest(unitId, functionCode, address, quantity);

                int bytesWritten = serialPort.writeBytes(request, request.length);
                if (bytesWritten != request.length) {
                    LOG.warning("Incomplete write: " + bytesWritten + " of " + request.length + " bytes");
                    return null;
                }

                // Calculate expected response length
                int expectedResponseLength;
                if (functionCode == 0x01 || functionCode == 0x02) {
                    expectedResponseLength = 5 + ((quantity + 7) / 8);
                } else {
                    expectedResponseLength = 5 + (quantity * 2);
                }

                byte[] response = new byte[expectedResponseLength];
                int totalBytesRead = readWithTimeout(response, 150);

                if (totalBytesRead == expectedResponseLength && response[0] == unitId && response[1] == functionCode) {
                    onRequestSuccess(messageId);
                    try {
                        Thread.sleep(10); //Modbus Frame spacing
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return response;
                } else if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                    int exceptionCode = response[2] & 0xFF;
                    int endAddress = address + quantity - 1;
                    onRequestFailure(messageId, "Batch read function=" + functionCode + " registers=" + address + "-" + endAddress + " unit=" + unitId,
                        "Exception code: " + exceptionCode + " - This may indicate illegal/unsupported registers in range. Consider adding them to 'illegalRegisters' agent configuration.");
                    flushSerialBuffer();
                } else if (totalBytesRead == 0) {
                    int endAddress = address + quantity - 1;
                    onRequestFailure(messageId, "Batch read function=" + functionCode + " registers=" + address + "-" + endAddress + " unit=" + unitId,
                        "Timeout - no response received");
                    flushSerialBuffer();
                } else if (totalBytesRead != expectedResponseLength) {
                    int endAddress = address + quantity - 1;
                    onRequestFailure(messageId, "Batch read function=" + functionCode + " registers=" + address + "-" + endAddress + " unit=" + unitId,
                        "Invalid response - received " + totalBytesRead + " bytes, expected " + expectedResponseLength + " - This may indicate illegal/unsupported registers in range. Consider adding them to 'illegalRegisters' agent configuration.");
                    flushSerialBuffer();
                } else {
                    onRequestFailure(messageId, "Batch read function=" + functionCode + " address=" + address + " unit=" + unitId,
                        "Response validation failed - Unit ID or Function Code mismatch (received unitId=" + (response[0] & 0xFF) + ", expected=" + unitId + ", received function=" + (response[1] & 0xFF) + ", expected=" + functionCode + ")");
                    flushSerialBuffer();
                }

            } catch (Exception e) {
                onRequestFailure(messageId, "Batch read function=" + functionCode + " address=" + address + " quantity=" + quantity, e);
                flushSerialBuffer();
            }

            return null;
        }
    }

    /**
     * Extract a value from a batch response at a specific offset
     */
    private Object extractValueFromBatchResponse(byte[] response, int registerOffset, ModbusAgentLink.ModbusDataType dataType, byte functionCode, Integer unitId) {
        int byteCount = response[2] & 0xFF;

        if (functionCode == 0x01 || functionCode == 0x02) {
            // Coils/Discrete - offset is in bits
            int byteIndex = 3 + (registerOffset / 8);
            int bitIndex = registerOffset % 8;
            if (byteIndex < response.length) {
                return ((response[byteIndex] >> bitIndex) & 0x01) != 0;
            }
        } else if (functionCode == 0x03 || functionCode == 0x04) {
            // Holding/Input registers - offset is in registers (2 bytes each)
            int byteOffset = 3 + (registerOffset * 2);
            int registerCount = dataType.getRegisterCount();

            if (byteOffset + (registerCount * 2) <= response.length) {
                byte[] dataBytes = new byte[registerCount * 2];
                System.arraycopy(response, byteOffset, dataBytes, 0, registerCount * 2);
                return parseMultiRegisterValue(dataBytes, registerCount, dataType, unitId);
            }
        }

        return null;
    }

    /**
     * Trigger an immediate read operation to verify a write.
     * Used when both read and write are configured to ensure attribute gets updated with actual device value.
     */
    private void triggerImmediateRead(AttributeRef ref, ModbusAgentLink agentLink) {
        scheduledExecutorService.execute(() -> {
            try {
                // Create a single-attribute batch for this verification read
                int registerCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(agentLink.getReadValueType().getRegisterCount());
                BatchReadRequest batch = new BatchReadRequest(agentLink.getReadAddress(), registerCount);
                batch.attributes.add(ref);
                batch.offsets.add(0);

                // Create temporary group
                Map<AttributeRef, ModbusAgentLink> tempGroup = new ConcurrentHashMap<>();
                tempGroup.put(ref, agentLink);

                // Execute the verification read
                executeBatchRead(batch, agentLink.getReadMemoryArea(), tempGroup, agentLink.getUnitId());
                LOG.finest("Verification read completed for " + ref);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to execute verification read for " + ref + ": " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected ScheduledFuture<?> scheduleModbusPollingWriteRequest(AttributeRef ref, ModbusAgentLink agentLink) {
        LOG.fine("Scheduling Modbus Write polling request to execute every " + agentLink.getRequestInterval() + "ms for attributeRef: " + ref);

        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Get current attribute value from linkedAttributes map
                // This map is kept up-to-date by doLinkedAttributeWrite after successful writes
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
                LOG.log(Level.FINE, "Exception during Modbus Write polling for " + ref + ": " + e.getMessage(), e);
            }
        }, 0, agentLink.getRequestInterval(), TimeUnit.MILLISECONDS);
    }

}
