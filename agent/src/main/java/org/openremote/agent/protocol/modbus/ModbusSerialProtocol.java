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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ModbusSerialProtocol extends AbstractProtocol<ModbusSerialAgent, ModbusAgentLink> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusSerialProtocol.class);
    
    protected final Map<AttributeRef, ScheduledFuture<?>> pollingMap = new ConcurrentHashMap<>();
    private SerialPort serialPort;
    private String connectionString;
    
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
        try {
            setConnectionStatus(ConnectionStatus.CONNECTING);
            
            String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
            int baudRate = agent.getBaudRate();
            int dataBits = agent.getDataBits();
            int stopBits = agent.getStopBits();
            int parity = agent.getParity();
            
            connectionString = "modbus-rtu://" + portName + "?baud=" + baudRate + "&data=" + dataBits + "&stop=" + stopBits + "&parity=" + parity;
            
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(dataBits);
            serialPort.setNumStopBits(stopBits);
            serialPort.setParity(mapParityToSerialPort(agent.getParity())); // Configurable parity
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 2000, 0);
            
            if (serialPort.openPort()) {
                setConnectionStatus(ConnectionStatus.CONNECTED);
                String parityName = agent.getParityEnum().name();
                LOG.info("Connected to Modbus RTU device on " + portName + " (" + baudRate + "," + dataBits + "," + parityName + "," + stopBits + ")");
            } else {
                setConnectionStatus(ConnectionStatus.ERROR);
                throw new RuntimeException("Failed to open serial port: " + portName);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to start Modbus Serial Protocol: " + e.getMessage(), e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }
    }

    @Override
    protected void doStop(Container container) throws Exception {
        pollingMap.values().forEach(future -> future.cancel(false));
        pollingMap.clear();
        
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            LOG.info("Disconnected from Modbus RTU device");
        }
        
        setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) throws RuntimeException {
        AttributeRef ref = new AttributeRef(assetId, attribute.getName());
        ScheduledFuture<?> pollingTask = scheduleModbusPollingReadRequest(
            ref,
            agentLink.getPollingMillis(),
            agentLink.getReadMemoryArea(),
            agentLink.getReadValueType(),
            agentLink.getReadRegistersAmount(),
            agentLink.getReadAddress()
        );
        pollingMap.put(ref, pollingTask);
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        ScheduledFuture<?> pollTask = pollingMap.remove(attributeRef);
        if (pollTask != null) {
            pollTask.cancel(false);
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

        LOG.log(Level.FINE, "Scheduling Modbus Read polling request to execute every " + pollingMillis + "ms for attributeRef: " + ref);
        
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
            int totalBytesRead = readWithTimeout(response, 1000);
            
            if (totalBytesRead >= 5 && response[0] == unitId && response[1] == functionCode) {
                return parseModbusResponse(response, functionCode, dataType);
            } else if (totalBytesRead > 0 && (response[1] & 0x80) != 0) {
                LOG.warning("Modbus exception response - Exception code: " + (response[2] & 0xFF));
            }
            
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error reading from Modbus device: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    private Object parseModbusResponse(byte[] response, byte functionCode, ModbusAgentLink.ModbusDataType dataType) {
        int byteCount = response[2] & 0xFF;
        
        if (functionCode == 0x01 || functionCode == 0x02) {
            // Read coils or discrete inputs - return boolean
            return (response[3] & 0x01) != 0;
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
                    return buffer.getFloat();
                } else {
                    // Return as 32-bit integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    return buffer.getInt();
                }
            }
        }
        
        return null;
    }
    
    private boolean writeSingleCoil(int unitId, int address, boolean value) {
        try {
            byte[] request = createWriteCoilRequest(unitId, (byte) 0x05, address, value);
            
            int bytesWritten = serialPort.writeBytes(request, request.length);
            if (bytesWritten != request.length) {
                LOG.warning("Incomplete write: " + bytesWritten + " of " + request.length + " bytes");
                return false;
            }
            
            // Read response (8 bytes for write single coil)
            byte[] response = new byte[8];
            int totalBytesRead = readWithTimeout(response, 1000);
            
            return totalBytesRead >= 8 && response[0] == unitId && response[1] == 0x05;
            
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error writing single coil: " + e.getMessage(), e);
            return false;
        }
    }
    
    private boolean writeSingleHoldingRegister(int unitId, int address, Object value) {
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
            int totalBytesRead = readWithTimeout(response, 1000);
            
            return totalBytesRead >= 8 && response[0] == unitId && response[1] == 0x06;
            
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error writing single register: " + e.getMessage(), e);
            return false;
        }
    }
    
    private int readWithTimeout(byte[] buffer, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int totalBytesRead = 0;
        
        while (totalBytesRead < buffer.length && (System.currentTimeMillis() - startTime) < timeoutMs) {
            int available = serialPort.bytesAvailable();
            if (available > 0) {
                int bytesRead = serialPort.readBytes(buffer, buffer.length - totalBytesRead, totalBytesRead);
                totalBytesRead += bytesRead;
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
                LOG.warning("Unknown parity value " + parity + ", defaulting to EVEN_PARITY");
                return SerialPort.EVEN_PARITY; // Default for Modbus RTU
        }
    }
}
