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
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

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

        int readAmountOfRegisters = (amountOfRegisters.isEmpty() || amountOfRegisters.get() < 1)
                ? dataType.getRegisterCount() : amountOfRegisters.get();
        String amountOfRegistersString = readAmountOfRegisters <= 1 ? "" : "["+readAmountOfRegisters+"]";

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

                Object responseValue = response.getObject(responseTag, readAmountOfRegisters-1);
                updateLinkedAttribute(ref, responseValue);
            } catch (Exception e) {
                LOG.log(Level.FINE,"Exception during Modbus Read polling: " + e.getMessage(), e);
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        int writeAddress = getOrThrowAgentLinkProperty(agentLink.getWriteAddress(), "write address");

        PlcWriteRequest.Builder builder = client.writeRequestBuilder();

        switch (agentLink.getWriteMemoryArea()){
            case COIL -> builder.addTagAddress("coil", "coil:" + writeAddress, processedValue);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + writeAddress, processedValue);
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

                int registerCount = agentLink.getReadRegistersAmount().orElse(agentLink.getReadValueType().getRegisterCount());

                try {
                    Object value;
                    if (registerCount == 1) {
                        value = response.getObject("batchRead", offset);
                    } else {
                        // For multi-register values, extract the range
                        value = response.getObject("batchRead", offset + registerCount - 1);
                    }

                    updateLinkedAttribute(ref, value);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to extract value for " + ref + " at offset " + offset + ": " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            LOG.log(Level.FINE, "Exception during batch read: " + e.getMessage(), e);
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
}
