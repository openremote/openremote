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

import org.openremote.agent.protocol.modbus.util.BatchReadRequest;
import org.openremote.agent.protocol.modbus.util.ModbusDataConverter;
import org.openremote.agent.protocol.modbus.util.ModbusFrame;
import org.openremote.agent.protocol.modbus.util.ModbusPduBuilder;
import org.openremote.agent.protocol.modbus.util.ModbusProtocolCallback;
import org.openremote.agent.protocol.modbus.util.RegisterRange;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Helper class containing shared Modbus protocol logic for batching, polling,
 * data conversion, and PDU building. Used by both ModbusTcpProtocol and ModbusSerialProtocol.
 *
 * @param <F> the frame type (ModbusTcpFrame or ModbusSerialFrame)
 */
public class ModbusExecutor<F extends ModbusFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusExecutor.class);
    private final ModbusProtocolCallback<F> callback;
    private final Map<String, Map<AttributeRef, ModbusAgentLink>> batchGroups = new ConcurrentHashMap<>();
    private final Map<String, List<BatchReadRequest>> cachedBatches = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> batchReadIntervalTasks = new ConcurrentHashMap<>();
    private final Map<AttributeRef, ScheduledFuture<?>> writeIntervalMap = new HashMap<>();
    final Object requestLock = new Object();
    protected int timeoutMs = 3000;

    public ModbusExecutor(ModbusProtocolCallback<F> callback) {
        this.callback = callback;
    }

    // ========== Lifecycle ==========

    public void onStart() {
        // Initialize deviceConfig if empty
        Optional<ModbusAgent.DeviceConfigMap> deviceConfigOpt = callback.getDeviceConfig();
        if (deviceConfigOpt.isEmpty() || deviceConfigOpt.get().isEmpty()) {
            LOG.info(callback.getProtocolName() + " - Initializing deviceConfig with default configuration for agent: " + callback.getModbusAgent().getName());
            ModbusAgent.DeviceConfigMap defaultConfig = new ModbusAgent.DeviceConfigMap();
            defaultConfig.put("default", ModbusAgent.ModbusDeviceConfig.createDefault());
            callback.publishAttributeEvent(new AttributeEvent(callback.getModbusAgent().getId(), ModbusAgent.DEVICE_CONFIG, defaultConfig));
        }
    }

    public void onStop() {
        batchReadIntervalTasks.values().forEach(future -> future.cancel(false));
        batchReadIntervalTasks.clear();
        batchGroups.clear();
        cachedBatches.clear();
        writeIntervalMap.forEach((key, value) -> value.cancel(false));
        writeIntervalMap.clear();
    }

    // ========== Attribute linking ==========

    public void linkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef ref = new AttributeRef(assetId, attribute.getName());

        // Check if read configuration is present (all required parameters)
        boolean hasReadConfig = agentLink.getReadMemoryArea() != null
                && agentLink.getReadValueType() != null
                && agentLink.getReadAddress() != null
                && agentLink.getUnitId() != null;

        if (hasReadConfig) {
            if (agentLink.getRequestInterval() != null) {
                // Setup continuous read interval using batching
                String groupKey = callback.getModbusAgent().getId() + "_" + agentLink.getUnitId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getRequestInterval();

                batchGroups.computeIfAbsent(groupKey, k -> new ConcurrentHashMap<>()).put(ref, agentLink);
                cachedBatches.remove(groupKey); // Invalidate cache

                // Only schedule task if one doesn't exist yet for this group
                if (!batchReadIntervalTasks.containsKey(groupKey)) {
                    ScheduledFuture<?> batchTask = scheduleBatchedReadIntervalTask(groupKey, agentLink.getReadMemoryArea(), agentLink.getRequestInterval(), agentLink.getUnitId());
                    batchReadIntervalTasks.put(groupKey, batchTask);
                    LOG.fine(callback.getProtocolName() + " - Scheduled new read interval task for batch group " + groupKey);
                }

                LOG.fine(callback.getProtocolName() + " - Added attribute " + ref + " to batch group " + groupKey + " (total attributes in group: " + batchGroups.get(groupKey).size() + ")");
            } else {
                // No requestInterval: execute one-time read on connection
                LOG.fine(callback.getProtocolName() + " - Scheduling one-time read on connection for " + ref);
                scheduleOneTimeRead(ref, agentLink);
            }
        } else {
            LOG.fine(callback.getProtocolName() + " - Skipping read interval for " + ref + " - read configuration incomplete (unitId, readMemoryArea, readValueType, and readAddress all required)");
        }

        // Check if write interval is enabled (requestInterval set, writeAddress present, no read address)
        if (agentLink.getRequestInterval() != null && agentLink.getWriteAddress() != null && agentLink.getReadAddress() == null) {
            ScheduledFuture<?> writeTask = scheduleModbusWriteRequestInterval(ref, agentLink);
            writeIntervalMap.put(ref, writeTask);
            LOG.fine(callback.getProtocolName() + " - Scheduled write interval task for attribute " + ref + " every " + agentLink.getRequestInterval() + "ms");
        }
    }

    public void unlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // Remove from write interval
        ScheduledFuture<?> writeTask = writeIntervalMap.remove(attributeRef);
        if (writeTask != null) {
            writeTask.cancel(false);
        }

        // Remove from batch group (if read config was present)
        if (agentLink.getReadMemoryArea() != null) {
            String groupKey = callback.getModbusAgent().getId() + "_" + agentLink.getUnitId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getRequestInterval();
            Map<AttributeRef, ModbusAgentLink> group = batchGroups.get(groupKey);
            if (group != null) {
                group.remove(attributeRef);
                cachedBatches.remove(groupKey); // Invalidate cache

                // If group is empty, cancel batch task
                if (group.isEmpty()) {
                    batchGroups.remove(groupKey);
                    cachedBatches.remove(groupKey);
                    ScheduledFuture<?> task = batchReadIntervalTasks.remove(groupKey);
                    if (task != null) {
                        task.cancel(false);
                    }
                    LOG.fine(callback.getProtocolName() + " - Removed empty batch group " + groupKey);
                } else {
                    LOG.fine(callback.getProtocolName() + " - Removed attribute " + attributeRef + " from batch group " + groupKey + " (remaining attributes: " + group.size() + ")");
                }
            }
        }
    }

    // ========== Write handling ==========

    public void handleAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Check connection status before attempting write
        if (callback.getConnectionStatus() != ConnectionStatus.CONNECTED) {
            LOG.fine("Skipping write operation - not connected (status: " + callback.getConnectionStatus() + ")");
            return;
        }

        int unitId = agentLink.getUnitId() != null ? agentLink.getUnitId() : 1;
        int writeAddress = AgentLink.getOrThrowAgentLinkProperty(Optional.ofNullable(agentLink.getWriteAddress()), "write address");
        int registersCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(1);

        // Convert 1-based user address to 0-based protocol address
        int protocolAddress = writeAddress - 1;

        try {
            byte[] pdu;

            switch (agentLink.getWriteMemoryArea()) {
                case COIL -> {
                    // Write single coil
                    boolean value = processedValue instanceof Boolean ? (Boolean) processedValue : false;
                    pdu = ModbusPduBuilder.buildWriteSingleCoilPDU(protocolAddress, value);
                    LOG.fine(callback.getProtocolName() + " Write Coil - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                }
                case HOLDING -> {
                    if (registersCount == 1) {
                        // Write single register
                        int value = processedValue instanceof Number ? ((Number) processedValue).intValue() : 0;
                        pdu = ModbusPduBuilder.buildWriteSingleRegisterPDU(protocolAddress, value);
                        LOG.fine(callback.getProtocolName() + " Write Single Register - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                    } else {
                        // Write multiple registers using shared conversion method
                        ModbusAgent.EndianFormat endianFormat = getEndianFormat(unitId);
                        byte[] registerData = ModbusDataConverter.convertValueToRegisterBytes(processedValue, registersCount, agentLink.getReadValueType(), endianFormat);
                        pdu = ModbusPduBuilder.buildWriteMultipleRegistersPDU(protocolAddress, registerData);
                        LOG.fine(callback.getProtocolName() + " Write Multiple Registers - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), RegisterCount: " + registersCount + ", DataType: " + agentLink.getReadValueType());
                    }
                }
                default -> throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
            }

            // Send request and wait for response
            F response = callback.sendModbusRequest(unitId, pdu, timeoutMs);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                String exceptionDesc = ModbusPduBuilder.getModbusExceptionDescription(exceptionCode);
                LOG.warning(callback.getProtocolName() + " - Write failed for address=" + writeAddress + " UnitId= " + unitId + ": " + exceptionDesc + " Event: " + event);
                return;
            }

            LOG.fine(callback.getProtocolName() + " Write Success - UnitId: " + unitId + ", Address: " + writeAddress);

            // Handle attribute update based on read configuration
            if (event.getSource() != null) {
                // Not a synthetic write (from continuous write task)
                if (agentLink.getReadAddress() == null) {
                    // Write-only: update the local linkedAttributes cache and notify the system
                    Attribute<?> attribute = callback.getLinkedAttributes().get(event.getRef());
                    if (attribute != null) {
                        @SuppressWarnings("unchecked")
                        Attribute<Object> attr = (Attribute<Object>) attribute;
                        attr.setValue(processedValue);
                    }
                    callback.updateLinkedAttribute(event.getRef(), processedValue);
                    LOG.finest(callback.getProtocolName() + " - Write-only attribute updated: " + event.getRef());
                } else {
                    // Write + Read: trigger immediate read to verify write
                    LOG.finest(callback.getProtocolName() + " - Triggering verification read after write for " + event.getRef());
                    triggerImmediateRead(event.getRef(), agentLink);
                }
            }
        } catch (Exception e) {
            String operation = "write address=" + writeAddress;
            if (e instanceof TimeoutException) {
                LOG.warning(callback.getProtocolName() + " - " + operation + " timed out: " + e.getMessage());
            } else {
                LOG.log(Level.WARNING, callback.getProtocolName() + " - " + operation + " failed: " + e.getMessage() + " Event: " + event, e);
            }
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    // ========== Batching logic ==========

    public List<BatchReadRequest> createBatchRequests(
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
                    .sorted(Comparator.comparingInt(e -> Optional.ofNullable(e.getValue().getReadAddress()).orElse(0)))
                    .collect(Collectors.toList());

            BatchReadRequest currentBatch = null;
            int currentEnd = -1;

            for (Map.Entry<AttributeRef, ModbusAgentLink> entry : sortedAttributes) {
                ModbusAgentLink link = entry.getValue();
                int address = Optional.ofNullable(link.getReadAddress()).orElse(0);
                int registerCount = Optional.ofNullable(link.getRegistersAmount()).orElse(link.getReadValueType().getRegisterCount());

                // Check if we can add to current batch
                if (currentBatch != null) {
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

                    int newQuantity = address + registerCount - currentBatch.getStartAddress();

                    // Add to current batch if: no illegal registers in gap and within max length
                    if (!hasIllegalInGap && newQuantity <= maxRegisterLength) {
                        int offset = address - currentBatch.getStartAddress();
                        currentBatch.addAttribute(entry.getKey(), offset);
                        currentBatch.setQuantity(newQuantity);
                        currentEnd = address + registerCount;
                        LOG.finest(callback.getProtocolName() + " - Added register " + address + " to batch starting at " + currentBatch.getStartAddress() + " (new quantity=" + newQuantity + ")");
                        continue;
                    } else {
                        // Finalize current batch
                        batches.add(currentBatch);
                        String reason = hasIllegalInGap
                                ? "illegal register " + firstIllegalRegister + " detected in gap (registers " + currentEnd + "-" + (address - 1) + ")"
                                : "would exceed maxRegisterLength (" + newQuantity + " > " + maxRegisterLength + ")";
                        LOG.fine(callback.getProtocolName() + " - Split batch before register " + address + ": " + reason);
                    }
                }

                // Start new batch
                currentBatch = new BatchReadRequest(address, registerCount);
                currentBatch.addAttribute(entry.getKey(), 0);
                currentEnd = address + registerCount;
                LOG.fine(callback.getProtocolName() + " - Started new batch at address " + address);
            }

            // Add final batch
            if (currentBatch != null) {
                batches.add(currentBatch);
                LOG.fine(callback.getProtocolName() + " - Finalized batch: startAddress=" + currentBatch.getStartAddress() + ", quantity=" + currentBatch.getQuantity() + ", attributes=" + currentBatch.getAttributes().size());
            }
        }

        return batches;
    }

    protected ScheduledFuture<?> scheduleBatchedReadIntervalTask(String groupKey, ModbusAgentLink.ReadMemoryArea memoryArea, long requestInterval, Integer unitId) {
        LOG.fine(callback.getProtocolName() + " - Scheduling batched read interval task for group " + groupKey + " every " + requestInterval + "ms");

        return callback.getScheduledExecutorService().scheduleWithFixedDelay(() -> {
            try {
                Map<AttributeRef, ModbusAgentLink> group = batchGroups.get(groupKey);
                if (group == null || group.isEmpty()) {
                    return;
                }

                List<BatchReadRequest> batches;
                synchronized (cachedBatches) {
                    batches = cachedBatches.get(groupKey);
                    if (batches == null) {
                        // Get per-unitId configuration
                        int maxRegisterLength = getMaxRegisterLength(unitId);
                        String illegalRegistersStr = getIllegalRegistersString(unitId);
                        Set<RegisterRange> illegalRegistersForUnit = parseIllegalRegisters(illegalRegistersStr);

                        LOG.fine(callback.getProtocolName() + " - Creating batch requests for group " + groupKey + " with " + group.size() + " attribute(s) (unitId=" + unitId + ", maxRegisterLength=" + maxRegisterLength + ")");
                        batches = createBatchRequests(group, illegalRegistersForUnit, maxRegisterLength);
                        cachedBatches.put(groupKey, batches);
                    }
                }

                // Execute all batches for this group
                for (int i = 0; i < batches.size(); i++) {
                    BatchReadRequest batch = batches.get(i);
                    int endAddress = batch.getStartAddress() + batch.getQuantity() - 1;
                    LOG.finest(callback.getProtocolName() + " - Executing batch " + (i + 1) + "/" + batches.size() + ": registers=" + batch.getStartAddress() + "-" + endAddress + " (quantity=" + batch.getQuantity() + ", attributes=" + batch.getAttributes().size() + ")");
                    executeBatchRead(batch, memoryArea, group, unitId);
                }

            } catch (Exception e) {
                LOG.log(Level.FINE, "Exception during batched reading interval: " + e.getMessage(), e);
            }
        }, 0, requestInterval, TimeUnit.MILLISECONDS);
    }

    protected void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group, Integer unitId) {
        // Check connection status before attempting read
        if (callback.getConnectionStatus() != ConnectionStatus.CONNECTED) {
            LOG.fine("Skipping batch read operation - not connected (status: " + callback.getConnectionStatus() + ")");
            return;
        }

        int effectiveUnitId = unitId != null ? unitId : 1;

        try {
            // Convert 1-based user address to 0-based protocol address
            int protocolAddress = batch.getStartAddress() - 1;

            byte functionCode = switch (memoryArea) {
                case COIL -> (byte) 0x01;           // Read Coils
                case DISCRETE -> (byte) 0x02;       // Read Discrete Inputs
                case HOLDING -> (byte) 0x03;        // Read Holding Registers
                case INPUT -> (byte) 0x04;          // Read Input Registers
            };

            byte[] pdu = ModbusPduBuilder.buildReadRequestPDU(functionCode, protocolAddress, batch.getQuantity());

            LOG.fine(callback.getProtocolName() + " Read Request - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", StartAddress: " + batch.getStartAddress() + " (0x" + Integer.toHexString(protocolAddress) + "), Quantity: " + batch.getQuantity() + ", AttributeCount: " + batch.getAttributes().size());
            F response = callback.sendModbusRequest(effectiveUnitId, pdu, timeoutMs);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                String exceptionDesc = ModbusPduBuilder.getModbusExceptionDescription(exceptionCode);
                LOG.warning(callback.getProtocolName() + " - Batch read " + memoryArea + ", UnitId: " + effectiveUnitId + " address=" + batch.getStartAddress() + " quantity=" + batch.getQuantity() + " failed: " + exceptionDesc);
                return;
            }

            byte[] data = ModbusPduBuilder.extractDataFromResponsePDU(response.getPdu(), functionCode);
            if (data == null) {
                LOG.warning(callback.getProtocolName() + " - Batch read " + memoryArea + " address=" + batch.getStartAddress() + " failed: Invalid response format");
                return;
            }

            LOG.finest(callback.getProtocolName() + " Read Success - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", DataBytes: " + data.length);

            for (int i = 0; i < batch.getAttributes().size(); i++) {
                AttributeRef ref = batch.getAttributes().get(i);
                int offset = batch.getOffsets().get(i);
                ModbusAgentLink agentLink = group.get(ref);

                if (agentLink == null) {
                    continue;
                }

                try {
                    int registerCount = Optional.ofNullable(agentLink.getRegistersAmount())
                            .orElse(agentLink.getReadValueType().getRegisterCount());
                    ModbusAgentLink.ModbusDataType dataType = agentLink.getReadValueType();

                    LOG.fine(callback.getProtocolName() + " - Extracting value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset + ", ReadAddress: " + agentLink.getReadAddress());
                    ModbusAgent.EndianFormat endianFormat = getEndianFormat(effectiveUnitId);
                    Object value = ModbusDataConverter.extractValueFromBatchResponse(data, offset, registerCount, dataType, endianFormat, functionCode);

                    if (value != null) {
                        LOG.finest(callback.getProtocolName() + " - Successfully extracted value for " + ref + " - Value: " + value + ", DataType: " + dataType + ", RegisterCount: " + registerCount);
                        callback.updateLinkedAttribute(ref, value);
                    } else {
                        LOG.warning(callback.getProtocolName() + " - Extracted null value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to extract value for " + ref + " at offset " + offset + " (DataType: " + agentLink.getReadValueType() + ", RegisterCount: " + agentLink.getRegistersAmount() + "): " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            String operation = "batch read " + memoryArea + " address=" + batch.getStartAddress() + " quantity=" + batch.getQuantity() + ", AttrGroup: " + group;
            if (e instanceof TimeoutException) {
                LOG.warning(callback.getProtocolName() + " - " + operation + " timed out: " + e.getMessage());
            } else {
                LOG.log(Level.WARNING, callback.getProtocolName() + " - " + operation + " failed: " + e.getMessage(), e);
            }
        }
    }

    protected void scheduleOneTimeRead(AttributeRef ref, ModbusAgentLink agentLink) {
        // Execute the read request once, asynchronously
        callback.getScheduledExecutorService().execute(() -> {
            try {
                // Create a temporary batch for this single read
                int registerCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(agentLink.getReadValueType().getRegisterCount());
                BatchReadRequest batch = new BatchReadRequest(agentLink.getReadAddress(), registerCount);
                batch.addAttribute(ref, 0);

                // Create a temporary group with this single attribute
                Map<AttributeRef, ModbusAgentLink> tempGroup = new ConcurrentHashMap<>();
                tempGroup.put(ref, agentLink);

                executeBatchRead(batch, agentLink.getReadMemoryArea(), tempGroup, agentLink.getUnitId());

                LOG.fine(callback.getProtocolName() + " - One-time read executed for " + ref);
            } catch (Exception e) {
                LOG.log(Level.WARNING, callback.getProtocolName() + " - Exception during one-time read for " + ref + ": " + e.getMessage(), e);
            }
        });
    }

    protected ScheduledFuture<?> scheduleModbusWriteRequestInterval(AttributeRef ref, ModbusAgentLink agentLink) {
        LOG.fine("Scheduling " + callback.getProtocolName() + " Write interval request to execute every " + agentLink.getRequestInterval() + "ms for attributeRef: " + ref);

        return callback.getScheduledExecutorService().scheduleWithFixedDelay(() -> {
            try {
                // Get current attribute value from linkedAttributes map
                Attribute<?> attribute = callback.getLinkedAttributes().get(ref);
                if (attribute == null || attribute.getValue().isEmpty()) {
                    LOG.finest(callback.getProtocolName() + " - Skipping write interval for " + ref + " - no value available");
                    return;
                }

                Object currentValue = attribute.getValue().orElse(null);
                if (currentValue == null) {
                    return;
                }

                // Create a synthetic AttributeEvent for the write
                AttributeEvent syntheticEvent = new AttributeEvent(ref, currentValue);
                handleAttributeWrite(agentLink, syntheticEvent, currentValue);

                LOG.finest(callback.getProtocolName() + " - Write interval executed for " + ref + " with value: " + currentValue);
            } catch (Exception e) {
                LOG.log(Level.WARNING, callback.getProtocolName() + " - Exception during write interval for " + ref + ": " + e.getMessage(), e);
            }
        }, 0, agentLink.getRequestInterval(), TimeUnit.MILLISECONDS);
    }

    protected void triggerImmediateRead(AttributeRef ref, ModbusAgentLink agentLink) {
        callback.getScheduledExecutorService().execute(() -> {
            try {
                // Create a single-attribute batch for this verification read
                int registerCount = Optional.ofNullable(agentLink.getRegistersAmount())
                        .orElse(agentLink.getReadValueType().getRegisterCount());
                BatchReadRequest batch = new BatchReadRequest(agentLink.getReadAddress(), registerCount);
                batch.addAttribute(ref, 0);

                Map<AttributeRef, ModbusAgentLink> tempGroup = new ConcurrentHashMap<>();
                tempGroup.put(ref, agentLink);

                executeBatchRead(batch, agentLink.getReadMemoryArea(), tempGroup, agentLink.getUnitId());
                LOG.finest(callback.getProtocolName() + " - Verification read completed for " + ref);
            } catch (Exception e) {
                LOG.log(Level.WARNING, callback.getProtocolName() + " - Failed to execute verification read for " + ref + ": " + e.getMessage(), e);
            }
        });
    }

    // ========== Device config helpers ==========

    public ModbusAgent.ModbusDeviceConfig getDeviceConfigForUnitId(Integer unitId) {
        ModbusAgent.DeviceConfigMap configMap = callback.getDeviceConfig().orElse(null);

        if (configMap == null || configMap.isEmpty()) {
            return ModbusAgent.ModbusDeviceConfig.createDefault();
        }

        // Try specific unitId first
        if (unitId != null && configMap.containsKey(String.valueOf(unitId))) {
            return configMap.get(String.valueOf(unitId));
        }
        return configMap.getOrDefault("default", ModbusAgent.ModbusDeviceConfig.createDefault());
    }

    public Integer getMaxRegisterLength(Integer unitId) {
        ModbusAgent.ModbusDeviceConfig config = getDeviceConfigForUnitId(unitId);
        return config.getMaxRegisterLength();
    }

    public String getIllegalRegistersString(Integer unitId) {
        ModbusAgent.ModbusDeviceConfig config = getDeviceConfigForUnitId(unitId);
        return config.getIllegalRegisters();
    }

    public ModbusAgent.EndianFormat getEndianFormat(Integer unitId) {
        ModbusAgent.ModbusDeviceConfig config = getDeviceConfigForUnitId(unitId);
        return config.getEndianFormat();
    }

    // ========== Utilities ==========

    public Set<RegisterRange> parseIllegalRegisters(String illegalRegistersStr) {
        Set<RegisterRange> ranges = new HashSet<>();
        if (illegalRegistersStr == null || illegalRegistersStr.trim().isEmpty()) {
            return ranges;
        }

        String[] parts = illegalRegistersStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] rangeParts = part.split("-");
                try {
                    int start = Integer.parseInt(rangeParts[0].trim());
                    int end = Integer.parseInt(rangeParts[1].trim());
                    ranges.add(new RegisterRange(start, end));
                } catch (NumberFormatException e) {
                    LOG.warning(callback.getProtocolName() + " - Invalid register range format: " + part);
                }
            } else {
                try {
                    int register = Integer.parseInt(part);
                    ranges.add(new RegisterRange(register, register));
                } catch (NumberFormatException e) {
                    LOG.warning(callback.getProtocolName() + " - Invalid register format: " + part);
                }
            }
        }
        return ranges;
    }

    public boolean isIllegalRegister(int register, Set<RegisterRange> illegalRanges) {
        return illegalRanges.stream().anyMatch(range -> range.contains(register));
    }
}
