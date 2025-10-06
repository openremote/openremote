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

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.PlcWriteResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent>{

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusTcpProtocol.class);

    private PlcConnection client = null;
    private String connectionString;

    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    @Override
    protected Optional<String> getIllegalRegistersConfig() {
        return agent.getIllegalRegisters();
    }

    @Override
    protected void doStartProtocol(Container container) throws Exception {
        try {
            setConnectionStatus(ConnectionStatus.CONNECTING);

            // Convert byte/word order to PLC4X format
            String byteOrderParam = convertToPlc4xByteOrder(agent.getByteOrder(), agent.getWordOrder());

            connectionString = "modbus-tcp://" + agent.getHost().orElseThrow()
                    + ":" + agent.getPort().orElseThrow()
                    + "?unit-identifier=" + agent.getUnitId()
                    + "&byte-order=" + byteOrderParam;

            client = PlcDriverManager.getDefault().getConnectionManager().getConnection(connectionString);
            client.connect();

            setConnectionStatus(client.isConnected() ? ConnectionStatus.CONNECTED : ConnectionStatus.ERROR);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create PLC4X connection for protocol instance: " + agent, e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }
    }

    /**
     * Convert our byte/word order enums to PLC4X byte order format.
     */
    private String convertToPlc4xByteOrder(ModbusAgent.EndianOrder byteOrder, ModbusAgent.EndianOrder wordOrder) {
        if (byteOrder == ModbusAgent.EndianOrder.BIG && wordOrder == ModbusAgent.EndianOrder.BIG) {
            return "BIG_ENDIAN";  // ABCD
        } else if (byteOrder == ModbusAgent.EndianOrder.LITTLE && wordOrder == ModbusAgent.EndianOrder.LITTLE) {
            return "LITTLE_ENDIAN";  // DCBA
        } else if (byteOrder == ModbusAgent.EndianOrder.BIG && wordOrder == ModbusAgent.EndianOrder.LITTLE) {
            return "BIG_ENDIAN_BYTE_SWAP";  // BADC
        } else { // LITTLE byte, BIG word
            return "LITTLE_ENDIAN_BYTE_SWAP";  // CDAB
        }
    }

    @Override
    protected void doStopProtocol(Container container) throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Override
    protected Integer getMaxRegisterLength() {
        return agent.getMaxRegisterLength();
    }

    @Override
    protected ScheduledFuture<?> scheduleModbusPollingReadRequest(
            AttributeRef ref,
            long pollingMillis,
            ModbusAgentLink.ReadMemoryArea readType,
            ModbusAgentLink.ModbusDataType dataType,
            Optional<Integer> amountOfRegisters,
            Optional<Integer> readAddress) {

        PlcReadRequest.Builder builder = client.readRequestBuilder();

        int registersCount = (amountOfRegisters.isEmpty() || amountOfRegisters.get() < 1)
                ? dataType.getRegisterCount() : amountOfRegisters.get();
        String amountOfRegistersString = registersCount <= 1 ? "" : "["+registersCount+"]";

        int address = readAddress.orElseThrow(() -> new RuntimeException("Read Address is empty! Unable to schedule read request."));

        switch (readType) {
            case COIL -> builder.addTagAddress("coils", "coil:" + address + ":" + dataType + amountOfRegistersString);
            case DISCRETE -> builder.addTagAddress("discreteInputs", "discrete-input:" + address + ":" + dataType + amountOfRegistersString);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + address + ":" + dataType + amountOfRegistersString);
            case INPUT -> builder.addTagAddress("inputRegisters", "input-register:" + address + ":" + dataType + amountOfRegistersString);
            default -> throw new IllegalArgumentException("Unsupported read type: " + readType);
        }
        PlcReadRequest readRequest = builder.build();

        LOG.log(Level.FINE,"Scheduling Modbus Read Value polling request to execute every " + pollingMillis + "ms for attributeRef: " + ref);
        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                PlcReadResponse response = readRequest.execute().get(3, TimeUnit.SECONDS);

                String responseTag = response.getTagNames().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Could not retrieve the requested value from the response"));

                // PLC4X returns values at index 0 for all cases (single or multi-register)
                Object responseValue = response.getObject(responseTag, 0);
                LOG.log(Level.INFO, "TCP Read: tag=" + responseTag + ", registers=" + registersCount + ", value=" + responseValue + ", type=" + (responseValue != null ? responseValue.getClass().getName() : "null"));
                updateLinkedAttribute(ref, responseValue);
            } catch (Exception e) {
                LOG.log(Level.WARNING,"Exception during Modbus Read polling for " + ref + ": " + e.getMessage(), e);
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        int writeAddress = getOrThrowAgentLinkProperty(agentLink.getWriteAddress(), "write address");
        int registersCount = agentLink.getRegistersAmount().orElse(1);

        PlcWriteRequest.Builder builder = client.writeRequestBuilder();

        String amountString = registersCount <= 1 ? "" : "[" + registersCount + "]";

        switch (agentLink.getWriteMemoryArea()){
            case COIL -> builder.addTagAddress("coil", "coil:" + writeAddress + amountString, processedValue);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + writeAddress + amountString, processedValue);
            default -> throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
        }

        PlcWriteRequest request = builder.build();

        try {
            PlcWriteResponse response = request.execute().get(3, TimeUnit.SECONDS);
            if (response.getResponseCode(response.getTagNames().stream().findFirst().orElseThrow()) != PlcResponseCode.OK){
                throw new IllegalStateException("PLC Write Response code is something other than \"OK\"");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group) {
        try {
            PlcReadRequest.Builder builder = client.readRequestBuilder();

            // Build PLC4X tag for reading the batch
            String memoryAreaTag = switch (memoryArea) {
                case COIL -> "coil";
                case DISCRETE -> "discrete-input";
                case HOLDING -> "holding-register";
                case INPUT -> "input-register";
            };

            String tagAddress = memoryAreaTag + ":" + batch.startAddress;
            if (batch.quantity > 1) {
                tagAddress += "[" + batch.quantity + "]";
            }

            builder.addTagAddress("batchRead", tagAddress);
            PlcReadRequest readRequest = builder.build();

            // Execute the batch read
            PlcReadResponse response = readRequest.execute().get(3, TimeUnit.SECONDS);

            if (response.getResponseCode("batchRead") != PlcResponseCode.OK) {
                LOG.warning("Batch read failed for " + tagAddress + ": " + response.getResponseCode("batchRead"));
                return;
            }

            // Extract values for each attribute in the batch
            for (int i = 0; i < batch.attributes.size(); i++) {
                AttributeRef ref = batch.attributes.get(i);
                int offset = batch.offsets.get(i);
                ModbusAgentLink agentLink = group.get(ref);

                if (agentLink == null) {
                    continue;
                }

                try {
                    int registerCount = agentLink.getRegistersAmount().orElse(agentLink.getReadValueType().getRegisterCount());
                    ModbusAgentLink.ModbusDataType dataType = agentLink.getReadValueType();

                    // Extract value using helper method that handles multi-register conversion
                    Object value = extractValueFromBatchResponse(response, "batchRead", offset, registerCount, dataType);

                    if (value != null) {
                        LOG.fine("Extracted value from batch for " + ref + ": " + value + " (type: " + dataType + ", registers: " + registerCount + ")");
                        updateLinkedAttribute(ref, value);
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to extract value for " + ref + " at offset " + offset + ": " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            LOG.log(Level.FINE, "Exception during batch read: " + e.getMessage(), e);
        }
    }

    @Override
    protected ScheduledFuture<?> scheduleModbusPollingWriteRequest(AttributeRef ref, ModbusAgentLink agentLink) {
        LOG.fine("Scheduling Modbus Write polling request to execute every " + agentLink.getPollingMillis() + "ms for attributeRef: " + ref);

        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Get current attribute value
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
        }, 0, agentLink.getPollingMillis(), TimeUnit.MILLISECONDS);
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
     * Convert agent's EndianOrder to Java's ByteOrder
     */
    private ByteOrder getJavaByteOrder() {
        return agent.getByteOrder() == ModbusAgent.EndianOrder.BIG
            ? ByteOrder.BIG_ENDIAN
            : ByteOrder.LITTLE_ENDIAN;
    }

    /**
     * Apply word order swapping to multi-register data.
     * Word order determines how 16-bit registers are arranged within multi-register values.
     */
    private byte[] applyWordOrder(byte[] data, int registerCount) {
        // If word order is BIG or only one register, no swapping needed
        if (agent.getWordOrder() == ModbusAgent.EndianOrder.BIG || registerCount <= 1) {
            return data;
        }

        // LITTLE word order: reverse the order of registers
        byte[] result = new byte[data.length];
        for (int i = 0; i < registerCount; i++) {
            int srcIdx = i * 2;
            int dstIdx = (registerCount - 1 - i) * 2;
            result[dstIdx] = data[srcIdx];
            result[dstIdx + 1] = data[srcIdx + 1];
        }

        return result;
    }

    /**
     * Extract a multi-register value from PLC4X batch read response.
     * PLC4X batch reads return raw SHORT arrays without data type conversion,
     * so we need to manually convert based on the data type.
     */
    private Object extractValueFromBatchResponse(PlcReadResponse response, String tag, int offset, int registerCount, ModbusAgentLink.ModbusDataType dataType) {
        try {
            if (registerCount == 1) {
                // Single register - PLC4X handles this correctly
                return response.getObject(tag, offset);
            } else if (registerCount == 2) {
                // Two registers - DINT or REAL
                byte[] dataBytes = new byte[4];
                for (int i = 0; i < 2; i++) {
                    short value = response.getShort(tag, offset + i);
                    dataBytes[i * 2] = (byte) ((value >> 8) & 0xFF);
                    dataBytes[i * 2 + 1] = (byte) (value & 0xFF);
                }

                // Apply word order (register arrangement)
                dataBytes = applyWordOrder(dataBytes, 2);

                if (dataType == ModbusAgentLink.ModbusDataType.REAL) {
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(getJavaByteOrder());
                    float value = buffer.getFloat();

                    if (Float.isNaN(value) || Float.isInfinite(value)) {
                        LOG.warning("Batch read contains invalid float value (NaN or Infinity), ignoring");
                        return null;
                    }
                    return value;
                } else {
                    // DINT - 32-bit integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(getJavaByteOrder());
                    return buffer.getInt();
                }
            } else if (registerCount == 4) {
                // Four registers - LINT, ULINT, or LREAL
                byte[] dataBytes = new byte[8];
                for (int i = 0; i < 4; i++) {
                    short value = response.getShort(tag, offset + i);
                    dataBytes[i * 2] = (byte) ((value >> 8) & 0xFF);
                    dataBytes[i * 2 + 1] = (byte) (value & 0xFF);
                }

                // Apply word order (register arrangement)
                dataBytes = applyWordOrder(dataBytes, 4);

                if (dataType == ModbusAgentLink.ModbusDataType.LREAL) {
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(getJavaByteOrder());
                    double value = buffer.getDouble();

                    if (Double.isNaN(value) || Double.isInfinite(value)) {
                        LOG.warning("Batch read contains invalid double value (NaN or Infinity), ignoring");
                        return null;
                    }
                    return value;
                } else if (dataType == ModbusAgentLink.ModbusDataType.LINT) {
                    // 64-bit signed integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(getJavaByteOrder());
                    return buffer.getLong();
                } else if (dataType == ModbusAgentLink.ModbusDataType.ULINT) {
                    // 64-bit unsigned integer - use BigInteger
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(getJavaByteOrder());
                    long signedValue = buffer.getLong();

                    if (signedValue >= 0) {
                        return java.math.BigInteger.valueOf(signedValue);
                    } else {
                        // Handle negative as unsigned
                        return java.math.BigInteger.valueOf(signedValue).add(java.math.BigInteger.ONE.shiftLeft(64));
                    }
                } else {
                    // Default: treat as 64-bit signed integer
                    ByteBuffer buffer = ByteBuffer.wrap(dataBytes);
                    buffer.order(getJavaByteOrder());
                    return buffer.getLong();
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to extract value from batch response at offset " + offset + ": " + e.getMessage(), e);
            return null;
        }

        return null;
    }
}
