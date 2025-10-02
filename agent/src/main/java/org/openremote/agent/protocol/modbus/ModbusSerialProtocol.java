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
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ModbusSerialProtocol extends AbstractProtocol<ModbusSerialAgent, ModbusAgentLink> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusSerialProtocol.class);

    private static final java.util.concurrent.atomic.AtomicInteger groupCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    protected final Map<AttributeRef, ScheduledFuture<?>> pollingMap = new ConcurrentHashMap<>();
    protected final Map<String, Map<AttributeRef, ModbusAgentLink>> batchGroups = new ConcurrentHashMap<>(); // Key: agentName_groupNum_memoryArea_pollingInterval
    protected final Map<String, List<BatchReadRequest>> cachedBatches = new ConcurrentHashMap<>(); // Cached batch requests per group
    protected final Map<String, ScheduledFuture<?>> batchPollingTasks = new ConcurrentHashMap<>();
    private final Object modbusLock = new Object();
    private SerialPortWrapper serialPort;
    private String connectionString;
    private Set<RegisterRange> illegalRegisters;

    // Serial port wrapper interface for testing
    public interface SerialPortWrapper {
        boolean openPort();
        boolean closePort();
        boolean isOpen();
        int writeBytes(byte[] buffer, long bytesToWrite);
        int readBytes(byte[] buffer, long bytesToRead, long offset);
        int bytesAvailable();
    }

    // Real SerialPort implementation
    private static class RealSerialPortWrapper implements SerialPortWrapper {
        private final SerialPort port;

        RealSerialPortWrapper(SerialPort port) {
            this.port = port;
        }

        @Override
        public boolean openPort() {
            return port.openPort();
        }

        @Override
        public boolean closePort() {
            return port.closePort();
        }

        @Override
        public boolean isOpen() {
            return port.isOpen();
        }

        @Override
        public int writeBytes(byte[] buffer, long bytesToWrite) {
            return port.writeBytes(buffer, (int) bytesToWrite);
        }

        @Override
        public int readBytes(byte[] buffer, long bytesToRead, long offset) {
            return port.readBytes(buffer, (int) bytesToRead, (int) offset);
        }

        @Override
        public int bytesAvailable() {
            return port.bytesAvailable();
        }
    }

    // For testing: inject a mock serial port wrapper
    public static SerialPortWrapper mockSerialPortForTesting = null;

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
    protected void doStart(Container container) throws Exception {
        // Parse illegal registers
        illegalRegisters = parseIllegalRegisters(agent.getIllegalRegisters().orElse(""));
        LOG.info("Loaded " + illegalRegisters.size() + " illegal register ranges for modbus agent" + agent.getId());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> startTask = executor.submit(() -> {
            try {
                setConnectionStatus(ConnectionStatus.CONNECTING);

                String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
                int baudRate = agent.getBaudRate();
                int dataBits = agent.getDataBits();
                int stopBits = agent.getStopBits();
                int parity = agent.getParityValue();

                connectionString = "modbus-rtu://" + portName + "?baud=" + baudRate + "&data=" + dataBits + "&stop=" + stopBits + "&parity=" + parity;

                // Use mock serial port for testing if available
                if (mockSerialPortForTesting != null) {
                    serialPort = mockSerialPortForTesting;
                    LOG.info("Using mock serial port for testing");
                } else {
                    SerialPort sp = SerialPort.getCommPort(portName);
                    sp.setBaudRate(baudRate);
                    sp.setNumDataBits(dataBits);
                    sp.setNumStopBits(stopBits);
                    sp.setParity(mapParityToSerialPort(agent.getParityValue()));
                    sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 0);
                    serialPort = new RealSerialPortWrapper(sp);
                }

                if (serialPort.openPort()) {
                    setConnectionStatus(ConnectionStatus.CONNECTED);
                    String parityName = agent.getParity().name();
                    LOG.info("Modbus on serial device started successfully, " + portName + " (" + baudRate + "," + dataBits + "," + parityName + "," + stopBits + ")");
                } else {
                    setConnectionStatus(ConnectionStatus.ERROR);
                    throw new RuntimeException("Failed to open serial port: " + portName);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to start Modbus Serial Protocol: " + e.getMessage(), e);
                setConnectionStatus(ConnectionStatus.ERROR);
                throw new RuntimeException(e);
            }
        });

        try {
            startTask.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            startTask.cancel(true);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw new RuntimeException("Failed to start Modbus Serial Protocol: timeout after 10 seconds", e);
        } catch (ExecutionException e) {
            throw new Exception("Failed to start Modbus Serial Protocol", e.getCause());
        } finally {
            executor.shutdown();
        }
    }

    @Override
    protected void doStop(Container container) throws Exception {
        pollingMap.values().forEach(future -> future.cancel(false));
        pollingMap.clear();

        batchPollingTasks.values().forEach(future -> future.cancel(false));
        batchPollingTasks.clear();
        batchGroups.clear();
        cachedBatches.clear();

        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            LOG.info("Disconnected from Modbus RTU device");
        }

        setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) throws RuntimeException {
        AttributeRef ref = new AttributeRef(assetId, attribute.getName());

        // Create group key: agenId_memoryArea_pollingInterval
        String groupKey = agent.getId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getPollingMillis();

        // Add to batch group and invalidate cached batches
        batchGroups.computeIfAbsent(groupKey, k -> new ConcurrentHashMap<>()).put(ref, agentLink);
        cachedBatches.remove(groupKey); // Invalidate cache

        // Only schedule task if one doesn't exist yet for this group
        if (!batchPollingTasks.containsKey(groupKey)) {
            ScheduledFuture<?> batchTask = scheduleBatchedPollingTask(groupKey, agentLink.getReadMemoryArea(), agentLink.getPollingMillis());
            batchPollingTasks.put(groupKey, batchTask);
            LOG.fine("Scheduled new polling task for batch group " + groupKey);
        }

        LOG.fine("Added attribute " + ref + " to batch group " + groupKey + " (total attributes in group: " + batchGroups.get(groupKey).size() + ")");
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // Remove from batch group
        String groupKey = agent.getId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getPollingMillis();
        Map<AttributeRef, ModbusAgentLink> group = batchGroups.get(groupKey);
        if (group != null) {
            group.remove(attributeRef);
            cachedBatches.remove(groupKey); // Invalidate cache

            // If group is empty, cancel batch task
            if (group.isEmpty()) {
                batchGroups.remove(groupKey);
                cachedBatches.remove(groupKey);
                ScheduledFuture<?> task = batchPollingTasks.remove(groupKey);
                if (task != null) {
                    task.cancel(false);
                }
                LOG.fine("Removed empty batch group " + groupKey);
            } else {
                LOG.info("Removed attribute " + attributeRef + " from batch group " + groupKey + " (remaining attributes: " + group.size() + ")");
            }
        }
    }

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        int writeAddress = getOrThrowAgentLinkProperty(agentLink.getWriteAddress(), "write address");
        
        try {
            switch (agentLink.getWriteMemoryArea()) {
                case COIL:
                    writeSingleCoil(agent.getUnitId(), writeAddress, (Boolean) processedValue);
                    break;
                case HOLDING:
                    writeSingleHoldingRegister(agent.getUnitId(), writeAddress, processedValue);
                    break;
                default:
                    throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error writing to Modbus device: " + e.getMessage(), e);
        }
    }

    protected ScheduledFuture<?> scheduleModbusPollingReadRequest(AttributeRef ref,
                                                                  long pollingMillis,
                                                                  ModbusAgentLink.ReadMemoryArea readType,
                                                                  ModbusAgentLink.ModbusDataType dataType,
                                                                  Optional<Integer> amountOfRegisters,
                                                                  Optional<Integer> readAddress) {

        int address = readAddress.orElseThrow(() -> new RuntimeException("Read Address is empty! Unable to schedule read request."));
        int readAmount = (amountOfRegisters.isEmpty() || amountOfRegisters.get() < 1)
                ? dataType.getRegisterCount() : amountOfRegisters.get();

        LOG.finest("Scheduling Modbus Read polling request to execute every " + pollingMillis + "ms for attributeRef: " + ref);
        
        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                Object value = readModbusValue(readType, agent.getUnitId(), address, readAmount, dataType);
                if (value != null) {
                    updateLinkedAttribute(ref, value);
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Exception during Modbus Read polling request for " + ref + ": " + e.getMessage(), e);
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    private Object readModbusValue(ModbusAgentLink.ReadMemoryArea memoryArea, int unitId, int address, int quantity, ModbusAgentLink.ModbusDataType dataType) {
        if (serialPort == null || !serialPort.isOpen()) {
            LOG.warning("Serial port not connected");
            return null;
        }
        
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
        
        return performModbusRead(unitId, functionCode, address, quantity, dataType);
    }
    
    private Object performModbusRead(int unitId, byte functionCode, int address, int quantity, ModbusAgentLink.ModbusDataType dataType) {
        synchronized (modbusLock) {
            LOG.info("Performing Modbus Read" + unitId + functionCode + address + quantity);
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
                    expectedResponseLength = 5 + ((quantity + 7) / 8); // Boolean values packed in bytes
                } else {
                    expectedResponseLength = 5 + (quantity * 2); // Each register is 2 bytes
                }

                byte[] response = new byte[expectedResponseLength];
                int totalBytesRead = readWithTimeout(response, 50);

                if (totalBytesRead >= 5 && response[0] == unitId && response[1] == functionCode) {
                    LOG.info("-------------MODBUS DEBUG RESPONSE------------- Address:"+ address  );

                    return parseModbusResponse(response, functionCode, dataType);
                } else if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                    int exceptionCode = response[2] & 0xFF;
                    LOG.warning("Modbus exception response - Exception code: " + exceptionCode + " (Function: " + functionCode + ", Address: " + address + ") - Resetting agent");
                    resetAgent();
                } else if (totalBytesRead == 0) {
                    LOG.warning("Modbus read timeout - no response received (Function: " + functionCode + ", Address: " + address + ", Unit: " + unitId + ")");
                } else {
                    LOG.warning("Modbus invalid response - received " + totalBytesRead + " bytes, expected " + expectedResponseLength + " (Function: " + functionCode + ", Address: " + address + ", Unit: " + unitId + ")");
                }

            } catch (Exception e) {
                LOG.log(Level.FINE, "Error reading from Modbus device: " + e.getMessage(), e);
            }

            return null;
        }
    }
    
    private Object parseModbusResponse(byte[] response, byte functionCode, ModbusAgentLink.ModbusDataType dataType) {
        int byteCount = response[2] & 0xFF;

        if (functionCode == 0x01 || functionCode == 0x02) {
            // Read coils or discrete inputs
            int numBits = byteCount * 8;

            if (byteCount == 1 && ((response[3] & 0xFE) == 0)) {
                // Single bit optimization: if only one byte and only first bit is set
                return (response[3] & 0x01) != 0;
            }

            // Multiple bits: return boolean array
            boolean[] bits = new boolean[numBits];
            for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
                int currentByte = response[3 + byteIndex] & 0xFF;
                for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                    int overallBitIndex = byteIndex * 8 + bitIndex;
                    if (overallBitIndex < numBits) {
                        bits[overallBitIndex] = ((currentByte >> bitIndex) & 0x01) != 0;
                    }
                }
            }
            return bits;
        } else if (functionCode == 0x03 || functionCode == 0x04) {
            // Read holding or input registers
            if (byteCount == 2) {
                // Single 16-bit register
                int high = (response[3] & 0xFF) << 8;
                int low = response[4] & 0xFF;
                return high | low;
            } else if (byteCount == 4) {
                // Two registers - could be IEEE754 float or 32-bit integer
                byte[] dataBytes = new byte[4];
                System.arraycopy(response, 3, dataBytes, 0, 4);

                if (dataType == ModbusAgentLink.ModbusDataType.REAL) {
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    float value = buffer.getFloat();

                    // Filter out NaN and Infinity values to prevent database issues
                    if (Float.isNaN(value) || Float.isInfinite(value)) {
                        LOG.warning("Modbus response contains invalid float value (NaN or Infinity), ignoring update");
                        return null;
                    }

                    return value;
                } else {
                    // Return as 32-bit integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    return buffer.getInt();
                }
            } else if (byteCount == 8) {
                // Four registers - could be 64-bit integer or double precision float
                byte[] dataBytes = new byte[8];
                System.arraycopy(response, 3, dataBytes, 0, 8);

                if (dataType == ModbusAgentLink.ModbusDataType.LREAL) {
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    double value = buffer.getDouble();

                    // Filter out NaN and Infinity values to prevent database issues
                    if (Double.isNaN(value) || Double.isInfinite(value)) {
                        LOG.warning("Modbus response contains invalid double value (NaN or Infinity), ignoring update");
                        return null;
                    }

                    return value;
                } else if (dataType == ModbusAgentLink.ModbusDataType.LINT) {
                    // 64-bit signed integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    return buffer.getLong();
                } else if (dataType == ModbusAgentLink.ModbusDataType.ULINT) {
                    // 64-bit unsigned integer - use BigInteger
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    long signedValue = buffer.getLong();

                    // Convert to unsigned BigInteger
                    if (signedValue >= 0) {
                        return java.math.BigInteger.valueOf(signedValue);
                    } else {
                        // Handle negative as unsigned
                        return java.math.BigInteger.valueOf(signedValue).add(java.math.BigInteger.ONE.shiftLeft(64));
                    }
                } else {
                    // Default: treat as 64-bit signed integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    return buffer.getLong();
                }
            }
        }
        
        return null;
    }
    
    private boolean writeSingleCoil(int unitId, int address, boolean value) {
        synchronized (modbusLock) {
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
                    LOG.warning("Modbus exception response on write coil - Exception code: " + exceptionCode + " (Address: " + address + ") - Resetting agent");
                    resetAgent();
                    return false;
                }
                return true;


            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error writing single coil: " + e.getMessage(), e);
                return false;
            }
        }
    }
    
    private boolean writeSingleHoldingRegister(int unitId, int address, Object value) {
        synchronized (modbusLock) {
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
                    LOG.warning("Modbus exception response on write register - Exception code: " + exceptionCode + " (Address: " + address + ") - Resetting agent");
                    resetAgent();
                    return false;
                }
                return true;


            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error writing single register: " + e.getMessage(), e);
                return false;
            }
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
    
    private byte[] createModbusRequest(int unitId, byte functionCode, int startAddress, int quantity) {
        byte[] frame = new byte[8];
        frame[0] = (byte) unitId;
        frame[1] = functionCode;
        frame[2] = (byte) (startAddress >> 8);
        frame[3] = (byte) (startAddress & 0xFF);
        frame[4] = (byte) (quantity >> 8);
        frame[5] = (byte) (quantity & 0xFF);
        
        int crc = calculateCRC16(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) (crc >> 8);
        
        return frame;
    }
    
    private byte[] createWriteCoilRequest(int unitId, byte functionCode, int coilAddress, boolean value) {
        byte[] frame = new byte[8];
        frame[0] = (byte) unitId;
        frame[1] = functionCode;
        frame[2] = (byte) (coilAddress >> 8);
        frame[3] = (byte) (coilAddress & 0xFF);
        frame[4] = value ? (byte) 0xFF : (byte) 0x00;
        frame[5] = (byte) 0x00;
        
        int crc = calculateCRC16(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) (crc >> 8);
        
        return frame;
    }
    
    private byte[] createWriteRegisterRequest(int unitId, byte functionCode, int registerAddress, int value) {
        byte[] frame = new byte[8];
        frame[0] = (byte) unitId;
        frame[1] = functionCode;
        frame[2] = (byte) (registerAddress >> 8);
        frame[3] = (byte) (registerAddress & 0xFF);
        frame[4] = (byte) (value >> 8);
        frame[5] = (byte) (value & 0xFF);
        
        int crc = calculateCRC16(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) (crc >> 8);
        
        return frame;
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
     * Schedule a batched polling task for a group of attributes
     */
    protected ScheduledFuture<?> scheduleBatchedPollingTask(String groupKey, ModbusAgentLink.ReadMemoryArea memoryArea, long pollingMillis) {
        LOG.info("Scheduling batched polling task for group " + groupKey + " every " + pollingMillis + "ms");

        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                Map<AttributeRef, ModbusAgentLink> group = batchGroups.get(groupKey);
                if (group == null || group.isEmpty()) {
                    return;
                }

                List<BatchReadRequest> batches;
                synchronized (cachedBatches) {
                    batches = cachedBatches.get(groupKey);
                    if (batches == null) {
                        LOG.info("Creating batch requests for group " + groupKey + " with " + group.size() + " attribute(s) (maxRegisterLength=" + agent.getMaxRegisterLength() + ")");
                        batches = createBatchRequests(group, illegalRegisters, agent.getMaxRegisterLength());
                        cachedBatches.put(groupKey, batches);
                        LOG.info("Created and cached " + batches.size() + " batch request(s) from " + group.size() + " attribute(s)");
                    }
                }

                // Execute each batch request
                for (int i = 0; i < batches.size(); i++) {
                    BatchReadRequest batch = batches.get(i);
                    int endAddress = batch.startAddress + batch.quantity - 1;
                    LOG.finest("Executing batch " + (i + 1) + "/" + batches.size() + ": registers=" + batch.startAddress + "-" + endAddress + " (quantity=" + batch.quantity + ", attributes=" + batch.attributes.size() + ")");
                    executeBatchRead(batch, memoryArea, group);
                }

            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception during batched Modbus polling for group " + groupKey + ": " + e.getMessage(), e);
            }
        }, pollingMillis, pollingMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Execute a batch read request and distribute values to attributes
     */
    private void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group) {
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

        // Perform the batch read
        byte[] response = performModbusBatchRead(agent.getUnitId(), functionCode, batch.startAddress, batch.quantity);

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
                Object value = extractValueFromBatchResponse(response, offset, agentLink.getReadValueType(), functionCode);
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
        synchronized (modbusLock) {
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
                int totalBytesRead = readWithTimeout(response, 50);

                if (totalBytesRead == expectedResponseLength && response[0] == unitId && response[1] == functionCode) {
                    return response;
                } else if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                    int exceptionCode = response[2] & 0xFF;
                    int endAddress = address + quantity - 1;
                    LOG.warning("Modbus exception response in batch read - Exception code: " + exceptionCode + " (Function: " + functionCode + ", Registers: " + address + "-" + endAddress + ", Unit: " + unitId + ") - This may indicate illegal/unsupported registers in range. Consider adding them to 'illegalRegisters' agent configuration. Resetting agent.");
                    resetAgent();
                } else if (totalBytesRead == 0) {
                    int endAddress = address + quantity - 1;
                    LOG.warning("Modbus batch read timeout - no response received (Function: " + functionCode + ", Registers: " + address + "-" + endAddress + ", Unit: " + unitId + ")");
                } else if (totalBytesRead != expectedResponseLength) {
                    int endAddress = address + quantity - 1;
                    LOG.warning("Modbus invalid batch response - received " + totalBytesRead + " bytes, expected " + expectedResponseLength + " (Function: " + functionCode + ", Registers: " + address + "-" + endAddress + ", Unit: " + unitId + ") - This may indicate illegal/unsupported registers in range. Consider adding them to 'illegalRegisters' agent configuration.");
                } else {
                    LOG.warning("Modbus batch response validation failed - Unit ID or Function Code mismatch (received unitId=" + (response[0] & 0xFF) + ", expected=" + unitId + ", received function=" + (response[1] & 0xFF) + ", expected=" + functionCode + ")");
                }

            } catch (Exception e) {
                LOG.log(Level.FINE, "Error in batch Modbus read: " + e.getMessage(), e);
            }

            return null;
        }
    }

    /**
     * Extract a value from a batch response at a specific offset
     */
    private Object extractValueFromBatchResponse(byte[] response, int registerOffset, ModbusAgentLink.ModbusDataType dataType, byte functionCode) {
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
                if (registerCount == 1) {
                    // Single 16-bit register
                    int high = (response[byteOffset] & 0xFF) << 8;
                    int low = response[byteOffset + 1] & 0xFF;
                    return high | low;
                } else if (registerCount == 2) {
                    // Two registers - could be float or 32-bit int
                    byte[] dataBytes = new byte[4];
                    System.arraycopy(response, byteOffset, dataBytes, 0, 4);

                    if (dataType == ModbusAgentLink.ModbusDataType.REAL) {
                        ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        float value = buffer.getFloat();

                        if (Float.isNaN(value) || Float.isInfinite(value)) {
                            LOG.warning("Batch response contains invalid float value (NaN or Infinity), ignoring");
                            return null;
                        }

                        return value;
                    } else {
                        ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        return buffer.getInt();
                    }
                } else if (registerCount == 4) {
                    // Four registers - could be 64-bit integer or double precision float
                    byte[] dataBytes = new byte[8];
                    System.arraycopy(response, byteOffset, dataBytes, 0, 8);

                    if (dataType == ModbusAgentLink.ModbusDataType.LREAL) {
                        ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        double value = buffer.getDouble();

                        if (Double.isNaN(value) || Double.isInfinite(value)) {
                            LOG.warning("Batch response contains invalid double value (NaN or Infinity), ignoring");
                            return null;
                        }

                        return value;
                    } else if (dataType == ModbusAgentLink.ModbusDataType.LINT) {
                        // 64-bit signed integer
                        ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        return buffer.getLong();
                    } else if (dataType == ModbusAgentLink.ModbusDataType.ULINT) {
                        // 64-bit unsigned integer - use BigInteger
                        ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        long signedValue = buffer.getLong();

                        // Convert to unsigned BigInteger
                        if (signedValue >= 0) {
                            return java.math.BigInteger.valueOf(signedValue);
                        } else {
                            // Handle negative as unsigned
                            return java.math.BigInteger.valueOf(signedValue).add(java.math.BigInteger.ONE.shiftLeft(64));
                        }
                    } else {
                        // Default: treat as 64-bit signed integer
                        ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        return buffer.getLong();
                    }
                }
            }
        }

        return null;
    }

    // ===== Helper classes for batching =====

    /**
     * Represents a range of illegal registers that cannot be queried
     */
    private static class RegisterRange {
        final int start;
        final int end;

        RegisterRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        boolean contains(int register) {
            return register >= start && register <= end;
        }

        boolean overlaps(int start, int end) {
            return !(this.end < start || this.start > end);
        }
    }

    /**
     * Parses illegal registers string format: "100,130,150-180,190"
     */
    private Set<RegisterRange> parseIllegalRegisters(String illegalRegisters) {
        Set<RegisterRange> ranges = new HashSet<>();
        if (illegalRegisters == null || illegalRegisters.trim().isEmpty()) {
            return ranges;
        }

        String[] parts = illegalRegisters.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] rangeParts = part.split("-");
                try {
                    int start = Integer.parseInt(rangeParts[0].trim());
                    int end = Integer.parseInt(rangeParts[1].trim());
                    ranges.add(new RegisterRange(start, end));
                } catch (NumberFormatException e) {
                    LOG.warning("Invalid register range format: " + part);
                }
            } else {
                try {
                    int register = Integer.parseInt(part);
                    ranges.add(new RegisterRange(register, register));
                } catch (NumberFormatException e) {
                    LOG.warning("Invalid register number: " + part);
                }
            }
        }
        return ranges;
    }

    /**
     * Checks if a register is in the illegal registers list
     */
    private boolean isIllegalRegister(int register, Set<RegisterRange> illegalRanges) {
        return illegalRanges.stream().anyMatch(range -> range.contains(register));
    }

    /**
     * Represents a batch read request for contiguous registers
     */
    private static class BatchReadRequest {
        final int startAddress;
        int quantity;
        final List<AttributeRef> attributes;
        final List<Integer> offsets; // Offset of each attribute within the batch

        BatchReadRequest(int startAddress, int quantity) {
            this.startAddress = startAddress;
            this.quantity = quantity;
            this.attributes = new ArrayList<>();
            this.offsets = new ArrayList<>();
        }

        void addAttribute(AttributeRef ref, int offset) {
            attributes.add(ref);
            offsets.add(offset);
        }
    }

    /**
     * Groups attributes into optimized batch requests
     */
    private List<BatchReadRequest> createBatchRequests(
            Map<AttributeRef, ModbusAgentLink> attributeLinks,
            Set<RegisterRange> illegalRanges,
            int maxRegisterLength) {

        // Group by memory area and sort by address
        Map<ModbusAgentLink.ReadMemoryArea, List<Map.Entry<AttributeRef, ModbusAgentLink>>> groupedByMemoryArea =
                attributeLinks.entrySet().stream()
                        .collect(Collectors.groupingBy(e -> e.getValue().getReadMemoryArea()));

        List<BatchReadRequest> batches = new ArrayList<>();

        for (Map.Entry<ModbusAgentLink.ReadMemoryArea, List<Map.Entry<AttributeRef, ModbusAgentLink>>> group : groupedByMemoryArea.entrySet()) {
            // Sort by address
            List<Map.Entry<AttributeRef, ModbusAgentLink>> sortedAttributes = group.getValue().stream()
                    .sorted(Comparator.comparingInt(e -> e.getValue().getReadAddress().orElse(0)))
                    .collect(Collectors.toList());

            BatchReadRequest currentBatch = null;
            int currentEnd = -1;

            for (Map.Entry<AttributeRef, ModbusAgentLink> entry : sortedAttributes) {
                ModbusAgentLink link = entry.getValue();
                int address = link.getReadAddress().orElse(0);
                int registerCount = link.getReadRegistersAmount().orElse(link.getReadValueType().getRegisterCount());

                // Check if we can add to current batch
                if (currentBatch != null) {
                    int gap = address - currentEnd;
                    boolean hasIllegalInGap = false;
                    int firstIllegalRegister = -1;

                    // Check if there are illegal registers in the gap
                    for (int i = currentEnd; i < address; i++) {
                        if (isIllegalRegister(i, illegalRanges)) {
                            hasIllegalInGap = true;
                            firstIllegalRegister = i;
                            break;
                        }
                    }

                    int newQuantity = address + registerCount - currentBatch.startAddress;

                    // Add to current batch if: no illegal registers in gap and within max length
                    if (!hasIllegalInGap && newQuantity <= maxRegisterLength) {
                        int offset = address - currentBatch.startAddress;
                        currentBatch.addAttribute(entry.getKey(), offset);
                        currentBatch.quantity = newQuantity;
                        currentEnd = address + registerCount;
                        LOG.finest("Added register " + address + " to batch starting at " + currentBatch.startAddress + " (new quantity=" + newQuantity + ")");
                        continue;
                    } else {
                        // Finalize current batch
                        batches.add(currentBatch);
                        String reason = hasIllegalInGap
                            ? "illegal register " + firstIllegalRegister + " detected in gap (registers " + currentEnd + "-" + (address - 1) + ")"
                            : "would exceed maxRegisterLength (" + newQuantity + " > " + maxRegisterLength + ")";
                        LOG.info("Split batch before register " + address + ": " + reason);
                    }
                }

                // Start new batch
                currentBatch = new BatchReadRequest(address, registerCount);
                currentBatch.addAttribute(entry.getKey(), 0);
                currentEnd = address + registerCount;
                LOG.fine("Started new batch at address " + address + " (previous batch would have exceeded maxRegisterLength=" + maxRegisterLength + ")");
            }

            // Add final batch
            if (currentBatch != null) {
                batches.add(currentBatch);
                LOG.fine("Finalized batch: startAddress=" + currentBatch.startAddress + ", quantity=" + currentBatch.quantity + ", attributes=" + currentBatch.attributes.size());
            }
        }

        return batches;
    }

    /**
     * Reset the agent by reconnecting the serial port and recreating polling tasks
     */
    private void resetAgent() {
        scheduledExecutorService.execute(() -> {
            try {
                LOG.info("Resetting Modbus Serial agent - reconnecting serial port");

                // Cancel all existing batch polling tasks and clear caches
                batchPollingTasks.values().forEach(future -> future.cancel(false));
                batchPollingTasks.clear();
                cachedBatches.clear(); // Clear cached batches on reset

                // Close serial port
                if (serialPort != null && serialPort.isOpen()) {
                    serialPort.closePort();
                    LOG.info("Serial port closed for reset");
                }

                // Wait before reconnecting
                Thread.sleep(1000);

                // Reopen serial port
                setConnectionStatus(ConnectionStatus.CONNECTING);

                String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
                int baudRate = agent.getBaudRate();
                int dataBits = agent.getDataBits();
                int stopBits = agent.getStopBits();

                // Use mock serial port for testing if available
                if (mockSerialPortForTesting != null) {
                    serialPort = mockSerialPortForTesting;
                    LOG.info("Using mock serial port for testing (reset)");
                } else {
                    SerialPort sp = SerialPort.getCommPort(portName);
                    sp.setBaudRate(baudRate);
                    sp.setNumDataBits(dataBits);
                    sp.setNumStopBits(stopBits);
                    sp.setParity(mapParityToSerialPort(agent.getParityValue()));
                    sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 0);
                    serialPort = new RealSerialPortWrapper(sp);
                }

                if (!serialPort.openPort()) {
                    setConnectionStatus(ConnectionStatus.ERROR);
                    throw new RuntimeException("Failed to reopen serial port: " + portName);
                }

                LOG.info("Serial port reopened successfully");
                setConnectionStatus(ConnectionStatus.CONNECTED);

                // Recreate batch polling tasks for all groups
                Map<String, Map<AttributeRef, ModbusAgentLink>> groupsCopy = new HashMap<>(batchGroups);
                for (Map.Entry<String, Map<AttributeRef, ModbusAgentLink>> entry : groupsCopy.entrySet()) {
                    String groupKey = entry.getKey();
                    Map<AttributeRef, ModbusAgentLink> group = entry.getValue();

                    if (!group.isEmpty()) {
                        // Extract memoryArea and pollingInterval from groupKey: agentName_memoryArea_pollingInterval
                        String[] parts = groupKey.split("_");
                        ModbusAgentLink.ReadMemoryArea memoryArea = ModbusAgentLink.ReadMemoryArea.valueOf(parts[1]);
                        long pollingMillis = Long.parseLong(parts[2]);

                        ScheduledFuture<?> batchTask = scheduleBatchedPollingTask(groupKey, memoryArea, pollingMillis);
                        batchPollingTasks.put(groupKey, batchTask);
                        LOG.info("Recreated batch polling task for group " + groupKey + " with " + group.size() + " attributes");
                    }
                }

                LOG.info("Modbus Serial agent reset complete");

            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to reset Modbus Serial agent: " + e.getMessage(), e);
                setConnectionStatus(ConnectionStatus.ERROR);
            }
        });
    }
}
