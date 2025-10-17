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
        setConnectionStatus(ConnectionStatus.CONNECTING);

        connectionString = "modbus-tcp://" + agent.getHost().orElseThrow()
                + ":" + agent.getPort().orElseThrow()
                + "?unit-identifier=" + agent.getUnitId()
                + "&byte-order=" + agent.getEndianFormat().getJsonValue();

        // Retry logic with exponential backoff
        int maxRetries = 3;
        int retryDelayMs = 500; // Start with 500ms
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                LOG.info("Attempting to connect to Modbus TCP " + agent.getHost().orElse("unknown") + ":" + agent.getPort().orElse(502) + " (attempt " + attempt + "/" + maxRetries + ")");

                client = PlcDriverManager.getDefault().getConnectionManager().getConnection(connectionString);
                client.connect();

                if (client.isConnected()) {
                    setConnectionStatus(ConnectionStatus.CONNECTED);
                    LOG.info("Modbus TCP connection established successfully: " + connectionString);
                    return; // Success - exit method
                } else {
                    lastException = new PlcConnectionException("PLC4X client connected but isConnected() returned false");
                    LOG.warning("Modbus TCP connection failed on attempt " + attempt + "/" + maxRetries + ": Client not in connected state");

                    // Close failed connection attempt
                    try {
                        if (client != null) {
                            client.close();
                            client = null;
                        }
                    } catch (Exception closeEx) {
                        LOG.log(Level.FINE, "Error closing failed connection: " + closeEx.getMessage(), closeEx);
                    }
                }
            } catch (Exception e) {
                lastException = e;
                LOG.log(Level.WARNING, "Exception during Modbus TCP connection (attempt " + attempt + "/" + maxRetries + "): " + e.getMessage(), e);

                // Close failed connection attempt
                try {
                    if (client != null) {
                        client.close();
                        client = null;
                    }
                } catch (Exception closeEx) {
                    LOG.log(Level.FINE, "Error closing failed connection: " + closeEx.getMessage(), closeEx);
                }
            }

            // If not the last attempt, wait before retrying
            if (attempt < maxRetries) {
                try {
                    LOG.info("Waiting " + retryDelayMs + "ms before retry...");
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // Exponential backoff: 500ms, 1000ms, 2000ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.warning("Retry delay interrupted, aborting connection attempt");
                    break;
                }
            }
        }

        // All retries exhausted
        LOG.log(Level.SEVERE, "Failed to create PLC4X connection after " + maxRetries + " attempts: " + agent);
        setConnectionStatus(ConnectionStatus.ERROR);
        throw lastException != null ? lastException : new PlcConnectionException("Failed to connect after " + maxRetries + " attempts");
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
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        LOG.finest("DEBUG doLinkedAttributeWrite triggered: " + agentLink + ", event: " + event + ", processedValue: " + processedValue);
        int writeAddress = getOrThrowAgentLinkProperty(Optional.ofNullable(agentLink.getWriteAddress()), "write address");
        int registersCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(1);
        String messageId = "tcp_write_" + event.getRef().getId() + "_" + event.getRef().getName() + "_" + writeAddress;

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
                onRequestFailure(messageId, "Modbus TCP write address=" + writeAddress, "PLC Write Response code is not OK: " + response.getResponseCode(response.getTagNames().stream().findFirst().orElseThrow()));
                throw new IllegalStateException("PLC Write Response code is something other than \"OK\"");
            }
            onRequestSuccess(messageId);

            // Only update attribute value when:
            // -the event source isn't a synthetic polling task write
            // -there is no reading action set (the read action should trigger the update)
            if (event.getSource() != null & agentLink.getReadAddress() == null) {
                // First update the local map so polling sees the new value immediately
                Attribute<?> attribute = linkedAttributes.get(event.getRef());
                if (attribute != null) {
                    @SuppressWarnings("unchecked")
                    Attribute<Object> attr = (Attribute<Object>) attribute;
                    attr.setValue(processedValue);
                }
                // Then send the event to the system
                updateLinkedAttribute(event.getRef(), processedValue);
                LOG.finest("DEBUG doLinkedAttributeWrite triggered an updateLinkedAttribute: " + event.getRef());
            }
            // Polling writes have null source, so they don't trigger updateLinkedAttribute
        } catch (Exception e) {
            onRequestFailure(messageId, "Modbus TCP write address=" + writeAddress, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group) {
        String messageId = "tcp_batch_" + memoryArea + "_" + batch.startAddress + "_" + batch.quantity;
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
                onRequestFailure(messageId, "Modbus TCP batch read " + tagAddress, "Response code: " + response.getResponseCode("batchRead"));
                return;
            }

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
                    int registerCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(agentLink.getReadValueType().getRegisterCount());
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
            onRequestFailure(messageId, "Modbus TCP batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity, e);
        }
    }

    @Override
    protected ScheduledFuture<?> scheduleModbusPollingWriteRequest(AttributeRef ref, ModbusAgentLink agentLink) {
        LOG.fine("Scheduling Modbus Write polling request to execute every " + agentLink.getPollingMillis() + "ms for attributeRef: " + ref);

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
     * Extract a multi-register value from PLC4X batch read response.
     * PLC4X batch reads return raw SHORT arrays without data type conversion,
     * so we need to manually convert based on the data type.
     */
    private Object extractValueFromBatchResponse(PlcReadResponse response, String tag, int offset, int registerCount, ModbusAgentLink.ModbusDataType dataType) {
        try {
            if (registerCount == 1) {
                // Single register - PLC4X handles this correctly
                return response.getObject(tag, offset);
            }

            // Multi-register: extract bytes and use shared parsing logic
            byte[] dataBytes = new byte[registerCount * 2];
            for (int i = 0; i < registerCount; i++) {
                short value = response.getShort(tag, offset + i);
                dataBytes[i * 2] = (byte) ((value >> 8) & 0xFF);
                dataBytes[i * 2 + 1] = (byte) (value & 0xFF);
            }

            return parseMultiRegisterValue(dataBytes, registerCount, dataType);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to extract value from batch response at offset " + offset + ": " + e.getMessage(), e);
            return null;
        }
    }
}
