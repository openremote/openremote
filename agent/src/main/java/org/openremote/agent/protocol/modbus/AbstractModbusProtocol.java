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

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
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

public abstract class AbstractModbusProtocol<S extends AbstractModbusProtocol<S,T>, T extends ModbusAgent<T, S>>
        extends AbstractProtocol<T, ModbusAgentLink>{

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractModbusProtocol.class);

    /**
     * Common interface for Modbus response frames (TCP and Serial)
     */
    protected interface ModbusResponse {
        boolean isException();
        byte[] getPdu();
        int getUnitId();
        byte getFunctionCode();
    }

    protected final Map<String, Map<AttributeRef, ModbusAgentLink>> batchGroups = new ConcurrentHashMap<>();
    protected final Map<String, List<BatchReadRequest>> cachedBatches = new ConcurrentHashMap<>();
    protected final Map<String, ScheduledFuture<?>> batchReadIntervalTasks = new ConcurrentHashMap<>();
    protected final Map<AttributeRef, ScheduledFuture<?>> writeIntervalMap = new HashMap<>();

    protected String connectionString;
    protected final Object requestLock = new Object();

    public AbstractModbusProtocol(T agent) {
        super(agent);
    }

    /**
     * Get per-unitId device configuration from the agent.
     * Returns map of unitId string -> ModbusDeviceConfig, with "default" key for default config.
     */
    protected abstract Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig();

    /**
     * Protocol-specific start logic. Subclasses implement connection setup here.
     */
    protected abstract void doStartProtocol(Container container) throws Exception;

    @Override
    protected void doStop(Container container) throws Exception {
        batchReadIntervalTasks.values().forEach(future -> future.cancel(false));
        batchReadIntervalTasks.clear();
        batchGroups.clear();
        cachedBatches.clear();
        writeIntervalMap.forEach((key, value) -> value.cancel(false));
        writeIntervalMap.clear();

        // Call protocol-specific stop
        doStopProtocol(container);
    }

    /**
     * Protocol-specific stop logic. Subclasses implement connection cleanup here.
     */
    protected abstract void doStopProtocol(Container container) throws Exception;
    /**
     * Get ModbusDeviceConfig for a specific unitId, falling back to "default" key.
     */
    protected ModbusAgent.ModbusDeviceConfig getDeviceConfigForUnitId(Integer unitId) {
        ModbusAgent.DeviceConfigMap configMap = getDeviceConfig().orElse(null);

        if (configMap == null || configMap.isEmpty()) {
            return ModbusAgent.ModbusDeviceConfig.createDefault();
        }

        // Try specific unitId first
        if (unitId != null && configMap.containsKey(String.valueOf(unitId))) {
            return configMap.get(String.valueOf(unitId));
        }
        return configMap.getOrDefault("default", ModbusAgent.ModbusDeviceConfig.createDefault());
    }

    /**
     * Get EndianFormat for a specific unitId.
     * @param unitId The unit ID to look up configuration for
     * @return The EndianFormat for this unitId, or BIG_ENDIAN if not configured
     */
    protected ModbusAgent.EndianFormat getEndianFormat(Integer unitId) {
        ModbusAgent.ModbusDeviceConfig config = getDeviceConfigForUnitId(unitId);
        return config.getEndianFormat();
    }

    /**
     * Get Java ByteOrder based on EndianFormat for a specific unitId.
     * Extracts byte order from the combined endian format.
     */
    protected java.nio.ByteOrder getJavaByteOrder(Integer unitId) {
        ModbusAgent.EndianFormat format = getEndianFormat(unitId);
        return (format == ModbusAgent.EndianFormat.BIG_ENDIAN || format == ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP)
            ? java.nio.ByteOrder.BIG_ENDIAN
            : java.nio.ByteOrder.LITTLE_ENDIAN;
    }

    /**
     * Apply word order swapping to multi-register data based on EndianFormat for a specific unitId.
     * Word order determines how 16-bit registers are arranged within multi-register values.
     */
    protected byte[] applyWordOrder(byte[] data, int registerCount, Integer unitId) {
        ModbusAgent.EndianFormat format = getEndianFormat(unitId);

        // No swap single register or BIG_ENDIAN/LITTLE_ENDIAN
        if (registerCount <= 1 || format == ModbusAgent.EndianFormat.BIG_ENDIAN || format == ModbusAgent.EndianFormat.LITTLE_ENDIAN) {
            return data;
        }
        // BYTE_SWAP
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
     * Parse multi-register value from raw bytes based on data type for a specific unitId.
     * Handles byte order, word order, and type conversion.
     */
    protected Object parseMultiRegisterValue(byte[] dataBytes, int registerCount, ModbusAgentLink.ModbusDataType dataType, Integer unitId) {
        if (registerCount == 1) {
            // Single 16-bit register
            int high = (dataBytes[0] & 0xFF) << 8;
            int low = dataBytes[1] & 0xFF;
            return high | low;
        } else if (registerCount == 2) {
            // Two registers - could be IEEE754 float or 32-bit integer
            byte[] processedBytes = applyWordOrder(dataBytes, 2, unitId);

            if (dataType == ModbusAgentLink.ModbusDataType.REAL) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(processedBytes);
                buffer.order(getJavaByteOrder(unitId));
                float value = buffer.getFloat();

                // Filter out NaN and Infinity values
                if (Float.isNaN(value) || Float.isInfinite(value)) {
                    LOG.warning(getProtocolName() + " - Invalid float value (NaN or Infinity), ignoring");
                    return null;
                }
                return value;
            } else {
                // Return as 32-bit integer
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(processedBytes);
                buffer.order(getJavaByteOrder(unitId));
                return buffer.getInt();
            }
        } else if (registerCount == 4) {
            // Four registers - could be 64-bit integer or double precision float
            byte[] processedBytes = applyWordOrder(dataBytes, 4, unitId);

            if (dataType == ModbusAgentLink.ModbusDataType.LREAL) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(processedBytes);
                buffer.order(getJavaByteOrder(unitId));
                double value = buffer.getDouble();

                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    LOG.warning(getProtocolName() + " - Invalid double value (NaN or Infinity), ignoring");
                    return null;
                }
                return value;
            } else if (dataType == ModbusAgentLink.ModbusDataType.LINT) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(processedBytes);
                buffer.order(getJavaByteOrder(unitId));
                return buffer.getLong();
            } else if (dataType == ModbusAgentLink.ModbusDataType.ULINT) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(processedBytes);
                buffer.order(getJavaByteOrder(unitId));
                long signedValue = buffer.getLong();

                if (signedValue >= 0) {
                    return java.math.BigInteger.valueOf(signedValue);
                } else {
                    return java.math.BigInteger.valueOf(signedValue).add(java.math.BigInteger.ONE.shiftLeft(64));
                }
            } else {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(processedBytes);
                buffer.order(getJavaByteOrder(unitId));
                return buffer.getLong();
            }
        }

        return null;
    }

    /**
     * Represents a range of illegal registers that should not be read
     */
    protected static class RegisterRange {
        final int start;
        final int end;

        RegisterRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        boolean contains(int register) {
            return register >= start && register <= end;
        }

    }

    /**
     * Represents a batch read request for contiguous registers
     */
    protected static class BatchReadRequest {
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
     * Parses illegal registers string format: "100,130,150-180,190"
     */
    protected Set<RegisterRange> parseIllegalRegisters(String illegalRegistersStr) {
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
                    LOG.warning(getProtocolName() + " - Invalid register range format: " + part);
                }
            } else {
                try {
                    int register = Integer.parseInt(part);
                    ranges.add(new RegisterRange(register, register));
                } catch (NumberFormatException e) {
                    LOG.warning(getProtocolName() + " - Invalid register format: " + part);
                }
            }
        }
        return ranges;
    }

    /**
     * Check if a register is in the illegal ranges
     */
    protected boolean isIllegalRegister(int register, Set<RegisterRange> illegalRanges) {
        return illegalRanges.stream().anyMatch(range -> range.contains(register));
    }

    @Override
    protected void doStart(Container container) throws Exception {
        // Initialize deviceConfig if it's empty
        Optional<ModbusAgent.DeviceConfigMap> deviceConfigOpt = getDeviceConfig();
        if (deviceConfigOpt.isEmpty() || deviceConfigOpt.get().isEmpty()) {
            LOG.info(getProtocolName() + " - Initializing deviceConfig with default configuration for agent: " + agent.getName());
            ModbusAgent.DeviceConfigMap defaultConfig = new ModbusAgent.DeviceConfigMap();
            defaultConfig.put("default", ModbusAgent.ModbusDeviceConfig.createDefault());
            sendAttributeEvent(new AttributeEvent(agent.getId(), ModbusAgent.DEVICE_CONFIG, defaultConfig));
        }

        // Call protocol-specific start
        doStartProtocol(container);
    }



    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) throws RuntimeException {
            AttributeRef ref = new AttributeRef(assetId, attribute.getName());

            // Check if read configuration is present (all required parameters)
            boolean hasReadConfig = agentLink.getReadMemoryArea() != null
                    && agentLink.getReadValueType() != null
                    && agentLink.getReadAddress() != null
                    && agentLink.getUnitId() != null;

            if (hasReadConfig) {
                if (agentLink.getRequestInterval() != null) {
                    // Setup continuous read interval using batching
                    String groupKey = agent.getId() + "_" + agentLink.getUnitId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getRequestInterval();

                    batchGroups.computeIfAbsent(groupKey, k -> new ConcurrentHashMap<>()).put(ref, agentLink);
                    cachedBatches.remove(groupKey); // Invalidate cache

                    // Only schedule task if one doesn't exist yet for this group
                    if (!batchReadIntervalTasks.containsKey(groupKey)) {
                        ScheduledFuture<?> batchTask = scheduleBatchedReadIntervalTask(groupKey, agentLink.getReadMemoryArea(), agentLink.getRequestInterval(), agentLink.getUnitId());
                        batchReadIntervalTasks.put(groupKey, batchTask);
                        LOG.fine(getProtocolName() + " - Scheduled new read interval task for batch group " + groupKey);
                    }

                    LOG.fine(getProtocolName() + " - Added attribute " + ref + " to batch group " + groupKey + " (total attributes in group: " + batchGroups.get(groupKey).size() + ")");
                } else {
                    // No requestInterval: execute one-time read on connection
                    LOG.fine(getProtocolName() + " - Scheduling one-time read on connection for " + ref);
                    scheduleOneTimeRead(ref, agentLink);
                }
            } else {
                LOG.fine(getProtocolName() + " - Skipping read interval for " + ref + " - read configuration incomplete (unitId, readMemoryArea, readValueType, and readAddress all required)");
            }

            // Check if write interval is enabled (requestInterval set, writeAddress present, no read address)
            if (agentLink.getRequestInterval() != null && agentLink.getWriteAddress() != null && agentLink.getReadAddress() == null) {
                ScheduledFuture<?> writeTask = scheduleModbusWriteRequestInterval(ref, agentLink);
                writeIntervalMap.put(ref, writeTask);
                LOG.fine(getProtocolName() + " - Scheduled write interval task for attribute " + ref + " every " + agentLink.getRequestInterval() + "ms");
            }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // Remove from write interval
        ScheduledFuture<?> writeTask = writeIntervalMap.remove(attributeRef);
        if (writeTask != null) {
            writeTask.cancel(false);
        }

        // Remove from batch group (if read config was present)
        if (agentLink.getReadMemoryArea() != null) {
            String groupKey = agent.getId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getRequestInterval();
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
                    LOG.fine(getProtocolName() + " - Removed empty batch group " + groupKey);
                } else {
                    LOG.fine(getProtocolName() + " - Removed attribute " + attributeRef + " from batch group " + groupKey + " (remaining attributes: " + group.size() + ")");
                }
            }
        }
    }

    /**
     * Get the maximum register length for batching for a specific unitId.
     */
    protected Integer getMaxRegisterLength(Integer unitId) {
        ModbusAgent.ModbusDeviceConfig config = getDeviceConfigForUnitId(unitId);
        return config.getMaxRegisterLength();
    }

    /**
     * Get illegal registers string for a specific unitId.
     */
    protected String getIllegalRegistersString(Integer unitId) {
        ModbusAgent.ModbusDeviceConfig config = getDeviceConfigForUnitId(unitId);
        return config.getIllegalRegisters();
    }

    /**
     * Groups attributes into optimized batch requests
     */
    protected List<BatchReadRequest> createBatchRequests(
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
                        LOG.finest(getProtocolName() + " - Added register " + address + " to batch starting at " + currentBatch.startAddress + " (new quantity=" + newQuantity + ")");
                        continue;
                    } else {
                        // Finalize current batch
                        batches.add(currentBatch);
                        String reason = hasIllegalInGap
                            ? "illegal register " + firstIllegalRegister + " detected in gap (registers " + currentEnd + "-" + (address - 1) + ")"
                            : "would exceed maxRegisterLength (" + newQuantity + " > " + maxRegisterLength + ")";
                        LOG.fine(getProtocolName() + " - Split batch before register " + address + ": " + reason);
                    }
                }

                // Start new batch
                currentBatch = new BatchReadRequest(address, registerCount);
                currentBatch.addAttribute(entry.getKey(), 0);
                currentEnd = address + registerCount;
                LOG.fine(getProtocolName() + " - Started new batch at address " + address);
            }

            // Add final batch
            if (currentBatch != null) {
                batches.add(currentBatch);
                LOG.fine(getProtocolName() + " - Finalized batch: startAddress=" + currentBatch.startAddress + ", quantity=" + currentBatch.quantity + ", attributes=" + currentBatch.attributes.size());
            }
        }

        return batches;
    }

    /**
     * Schedule a batched read interval task for a group of attributes
     */
    protected ScheduledFuture<?> scheduleBatchedReadIntervalTask(String groupKey, ModbusAgentLink.ReadMemoryArea memoryArea, long requestInterval, Integer unitId) {
        LOG.fine(getProtocolName() + " - Scheduling batched read interval task for group " + groupKey + " every " + requestInterval + "ms");

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
                        // Get per-unitId configuration
                        int maxRegisterLength = getMaxRegisterLength(unitId);
                        String illegalRegistersStr = getIllegalRegistersString(unitId);
                        Set<RegisterRange> illegalRegistersForUnit = parseIllegalRegisters(illegalRegistersStr);

                        LOG.fine(getProtocolName() + " - Creating batch requests for group " + groupKey + " with " + group.size() + " attribute(s) (unitId=" + unitId + ", maxRegisterLength=" + maxRegisterLength + ")");
                        batches = createBatchRequests(group, illegalRegistersForUnit, maxRegisterLength);
                        cachedBatches.put(groupKey, batches);
                    }
                }

                // Execute all batches for this group
                for (int i = 0; i < batches.size(); i++) {
                    BatchReadRequest batch = batches.get(i);
                    int endAddress = batch.startAddress + batch.quantity - 1;
                    LOG.finest(getProtocolName() + " - Executing batch " + (i + 1) + "/" + batches.size() + ": registers=" + batch.startAddress + "-" + endAddress + " (quantity=" + batch.quantity + ", attributes=" + batch.attributes.size() + ")");
                    executeBatchRead(batch, memoryArea, group, unitId);
                }

            } catch (Exception e) {
                LOG.log(Level.FINE, "Exception during batched reading interval: " + e.getMessage(), e);
            }
        }, 0, requestInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Send a Modbus request and wait for response. Must be implemented by subclasses for protocol-specific communication.
     * @param unitId The Modbus unit ID
     * @param pdu The Protocol Data Unit (function code + data)
     * @param timeoutMs Timeout in milliseconds
     * @return The Modbus response
     * @throws Exception If the request fails or times out
     */
    protected abstract ModbusResponse sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception;

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Check connection status before attempting write
        if (getConnectionStatus() != ConnectionStatus.CONNECTED) {
            LOG.fine("Skipping write operation - not connected (status: " + getConnectionStatus() + ")");
            return;
        }

        int unitId = agentLink.getUnitId();
        int writeAddress = org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty(Optional.ofNullable(agentLink.getWriteAddress()), "write address");
        int registersCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(1);

        // Convert 1-based user address to 0-based protocol address
        int protocolAddress = writeAddress - 1;

        try {
            byte[] pdu;

            switch (agentLink.getWriteMemoryArea()) {
                case COIL -> {
                    // Write single coil
                    boolean value = processedValue instanceof Boolean ? (Boolean) processedValue : false;
                    pdu = buildWriteSingleCoilPDU(protocolAddress, value);
                    LOG.fine(getProtocolName() + " Write Coil - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                }
                case HOLDING -> {
                    if (registersCount == 1) {
                        // Write single register
                        int value = processedValue instanceof Number ? ((Number) processedValue).intValue() : 0;
                        pdu = buildWriteSingleRegisterPDU(protocolAddress, value);
                        LOG.fine(getProtocolName() + " Write Single Register - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Value: " + value);
                    } else {
                        // Write multiple registers using shared conversion method
                        byte[] registerData = convertValueToRegisterBytes(processedValue, registersCount, agentLink.getReadValueType(), unitId);
                        pdu = buildWriteMultipleRegistersPDU(protocolAddress, registerData);
                        LOG.fine(getProtocolName() + " Write Multiple Registers - UnitId: " + unitId + ", Address: " + writeAddress + " (0x" + Integer.toHexString(protocolAddress) + "), RegisterCount: " + registersCount + ", DataType: " + agentLink.getReadValueType());
                    }
                }
                default -> throw new IllegalStateException("Only COIL and HOLDING memory areas are supported for writing");
            }

            // Send request and wait for response
            ModbusResponse response = sendModbusRequest(unitId, pdu, 3000);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                String exceptionDesc = getModbusExceptionDescription(exceptionCode);
                LOG.warning(getProtocolName() + " - Write failed for address=" + writeAddress + ": " + exceptionDesc);
                return;
            }

            LOG.fine(getProtocolName() + " Write Success - UnitId: " + unitId + ", Address: " + writeAddress);

            // Handle attribute update based on read configuration
            if (event.getSource() != null) {
                // Not a synthetic write (from continuous write task)
                if (agentLink.getReadAddress() == null) {
                    // Write-only: update immediately based on write success
                    updateLinkedAttribute(event.getRef(), processedValue);
                    LOG.finest(getProtocolName() + " - Write-only attribute updated: " + event.getRef());
                } else {
                    // Write + Read: trigger immediate read to verify write
                    LOG.finest(getProtocolName() + " - Triggering verification read after write for " + event.getRef());
                    triggerImmediateRead(event.getRef(), agentLink);
                }
            }
        } catch (Exception e) {
            String operation = "write address=" + writeAddress;
            if (e instanceof TimeoutException) {
                LOG.warning(getProtocolName() + " - " + operation + " timed out: " + e.getMessage());
            } else {
                LOG.log(Level.WARNING, getProtocolName() + " - " + operation + " failed: " + e.getMessage(), e);
            }
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    /**
     * Execute a batch read request using the protocol-specific communication method.
     */
    protected void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group, Integer unitId) {
        // Check connection status before attempting read
        if (getConnectionStatus() != ConnectionStatus.CONNECTED) {
            LOG.fine("Skipping batch read operation - not connected (status: " + getConnectionStatus() + ")");
            return;
        }

        int effectiveUnitId = unitId != null ? unitId : 1;

        try {
            // Convert 1-based user address to 0-based protocol address
            int protocolAddress = batch.startAddress - 1;

            byte functionCode = switch (memoryArea) {
                case COIL -> (byte) 0x01;           // Read Coils
                case DISCRETE -> (byte) 0x02;       // Read Discrete Inputs
                case HOLDING -> (byte) 0x03;        // Read Holding Registers
                case INPUT -> (byte) 0x04;          // Read Input Registers
            };

            byte[] pdu = buildReadRequestPDU(functionCode, protocolAddress, batch.quantity);

            LOG.fine(getProtocolName() + " Read Request - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", StartAddress: " + batch.startAddress + " (0x" + Integer.toHexString(protocolAddress) + "), Quantity: " + batch.quantity + ", AttributeCount: " + batch.attributes.size());
            ModbusResponse response = sendModbusRequest(effectiveUnitId, pdu, 3000);

            if (response.isException()) {
                byte exceptionCode = response.getPdu()[1];
                String exceptionDesc = getModbusExceptionDescription(exceptionCode);
                LOG.warning(getProtocolName() + " - Batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity + " failed: " + exceptionDesc);
                return;
            }

            byte[] data = extractDataFromResponsePDU(response.getPdu(), functionCode);
            if (data == null) {
                LOG.warning(getProtocolName() + " - Batch read " + memoryArea + " address=" + batch.startAddress + " failed: Invalid response format");
                return;
            }

            LOG.finest(getProtocolName() + " Read Success - MemoryArea: " + memoryArea + ", UnitId: " + effectiveUnitId + ", DataBytes: " + data.length);

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

                    LOG.fine(getProtocolName() + " - Extracting value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset + ", ReadAddress: " + agentLink.getReadAddress());
                    Object value = extractValueFromBatchResponse(data, offset, registerCount, dataType, effectiveUnitId, functionCode);

                    if (value != null) {
                        LOG.finest(getProtocolName() + " - Successfully extracted value for " + ref + " - Value: " + value + ", DataType: " + dataType + ", RegisterCount: " + registerCount);
                        updateLinkedAttribute(ref, value);
                    } else {
                        LOG.warning(getProtocolName() + " - Extracted null value for " + ref + " - DataType: " + dataType + ", RegisterCount: " + registerCount + ", Offset: " + offset);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to extract value for " + ref + " at offset " + offset + " (DataType: " + agentLink.getReadValueType() + ", RegisterCount: " + agentLink.getRegistersAmount() + "): " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            String operation = "batch read " + memoryArea + " address=" + batch.startAddress + " quantity=" + batch.quantity;
            if (e instanceof TimeoutException) {
                LOG.warning(getProtocolName() + " - " + operation + " timed out: " + e.getMessage());
            } else {
                LOG.log(Level.WARNING, getProtocolName() + " - " + operation + " failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Schedule a one-time read for an attribute on connection.
     * Executes a single read request immediately using the batch read mechanism.
     */
    protected void scheduleOneTimeRead(AttributeRef ref, ModbusAgentLink agentLink) {
        // Execute the read request once, asynchronously
        scheduledExecutorService.execute(() -> {
            try {
                // Create a temporary batch for this single read
                int registerCount = Optional.ofNullable(agentLink.getRegistersAmount()).orElse(agentLink.getReadValueType().getRegisterCount());
                BatchReadRequest batch = new BatchReadRequest(agentLink.getReadAddress(), registerCount);
                batch.attributes.add(ref);
                batch.offsets.add(0);

                // Create a temporary group with this single attribute
                Map<AttributeRef, ModbusAgentLink> tempGroup = new ConcurrentHashMap<>();
                tempGroup.put(ref, agentLink);

                executeBatchRead(batch, agentLink.getReadMemoryArea(), tempGroup, agentLink.getUnitId());

                LOG.fine(getProtocolName() + " - One-time read executed for " + ref);
            } catch (Exception e) {
                LOG.log(Level.WARNING, getProtocolName() + " - Exception during one-time read for " + ref + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Schedule an interval write request for an attribute with requestInterval set.
     * This is a shared implementation used by both TCP and Serial protocols.
     */
    protected ScheduledFuture<?> scheduleModbusWriteRequestInterval(
            AttributeRef ref,
            ModbusAgentLink agentLink
    ) {
        LOG.fine("Scheduling " + getProtocolName() + " Write interval request to execute every " + agentLink.getRequestInterval() + "ms for attributeRef: " + ref);

        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Get current attribute value from linkedAttributes map
                Attribute<?> attribute = linkedAttributes.get(ref);
                if (attribute == null || attribute.getValue().isEmpty()) {
                    LOG.finest(getProtocolName() + " - Skipping write interval for " + ref + " - no value available");
                    return;
                }

                Object currentValue = attribute.getValue().orElse(null);
                if (currentValue == null) {
                    return;
                }

                // Create a synthetic AttributeEvent for the write
                AttributeEvent syntheticEvent = new AttributeEvent(ref, currentValue);
                doLinkedAttributeWrite(agentLink, syntheticEvent, currentValue);

                LOG.finest(getProtocolName() + " - Write interval executed for " + ref + " with value: " + currentValue);
            } catch (Exception e) {
                LOG.log(Level.WARNING, getProtocolName() + " - Exception during write interval for " + ref + ": " + e.getMessage(), e);
            }
        }, 0, agentLink.getRequestInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * Trigger an immediate read operation to verify a write.
     * Used when both read and write are configured to ensure attribute gets updated with actual device value.
     */
    protected void triggerImmediateRead(AttributeRef ref, ModbusAgentLink agentLink) {
        scheduledExecutorService.execute(() -> {
            try {
                // Create a single-attribute batch for this verification read
                int registerCount = Optional.ofNullable(agentLink.getRegistersAmount())
                    .orElse(agentLink.getReadValueType().getRegisterCount());
                BatchReadRequest batch = new BatchReadRequest(agentLink.getReadAddress(), registerCount);
                batch.attributes.add(ref);
                batch.offsets.add(0);


                Map<AttributeRef, ModbusAgentLink> tempGroup = new ConcurrentHashMap<>();
                tempGroup.put(ref, agentLink);

                executeBatchRead(batch, agentLink.getReadMemoryArea(), tempGroup, agentLink.getUnitId());
                LOG.finest(getProtocolName() + " - Verification read completed for " + ref);
            } catch (Exception e) {
                LOG.log(Level.WARNING, getProtocolName() + " - Failed to execute verification read for " + ref + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Extract value from batch response data.
     * Unified implementation for both TCP and Serial protocols.
     * @param data The raw response data (register/coil bytes)
     * @param offset Offset within the data (in registers for holding/input, in bits for coils/discrete)
     * @param registerCount Number of registers for this value
     * @param dataType The Modbus data type
     * @param unitId Unit ID for endian format lookup
     * @param functionCode Modbus function code (0x01=Coil, 0x02=Discrete, 0x03=Holding, 0x04=Input)
     * @return The extracted value, or null if extraction fails
     */
    protected Object extractValueFromBatchResponse(byte[] data, int offset, int registerCount,
                                                   ModbusAgentLink.ModbusDataType dataType,
                                                   Integer unitId, byte functionCode) {
        try {
            // For coils and discrete inputs, data is bit-packed
            if (functionCode == 0x01 || functionCode == 0x02) {
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
                LOG.warning(getProtocolName() + " - Not enough data in response: need " + (byteOffset + registerCount * 2) + " bytes, have " + data.length);
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
     * Convert value to register bytes for writing.
     * Unified implementation for both TCP and Serial protocols.
     * Applies byte order and word order based on unitId-specific configuration.
     * @param value The value to convert
     * @param registerCount Number of registers (1, 2, or 4)
     * @param dataType The Modbus data type
     * @param unitId Unit ID for endian format lookup
     * @return Byte array containing the register data
     */
    protected byte[] convertValueToRegisterBytes(Object value, int registerCount,
                                                 ModbusAgentLink.ModbusDataType dataType, Integer unitId) {
        byte[] bytes = new byte[registerCount * 2];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);

        // Apply byte order
        ModbusAgent.EndianFormat endianFormat = getEndianFormat(unitId);
        buffer.order(endianFormat == ModbusAgent.EndianFormat.BIG_ENDIAN ||
                     endianFormat == ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP
            ? java.nio.ByteOrder.BIG_ENDIAN : java.nio.ByteOrder.LITTLE_ENDIAN);

        // Convert based on register count and data type
        if (registerCount == 1) {
            // Single register (16-bit)
            if (value instanceof Number) {
                int intValue = ((Number) value).intValue();
                buffer.putShort((short) (intValue & 0xFFFF));
            } else {
                throw new IllegalArgumentException("Cannot convert value to register: " + value);
            }
        } else if (registerCount == 2) {
            // Two registers (32-bit int or float)
            if (dataType == ModbusAgentLink.ModbusDataType.REAL) {
                buffer.putFloat(((Number) value).floatValue());
            } else if (value instanceof Number) {
                buffer.putInt(((Number) value).intValue());
            } else {
                throw new IllegalArgumentException("Cannot convert value to 2 registers: " + value);
            }
        } else if (registerCount == 4) {
            // Four registers (64-bit int or double)
            if (dataType == ModbusAgentLink.ModbusDataType.LREAL) {
                buffer.putDouble(((Number) value).doubleValue());
            } else if (value instanceof Number) {
                buffer.putLong(((Number) value).longValue());
            } else {
                throw new IllegalArgumentException("Cannot convert value to 4 registers: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported register count for write: " + registerCount);
        }

        // Apply word order (byte swap for multi-register values)
        if (registerCount > 1 && (endianFormat == ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP ||
                                   endianFormat == ModbusAgent.EndianFormat.LITTLE_ENDIAN_BYTE_SWAP)) {
            bytes = applyWordOrder(bytes, registerCount, unitId);
        }

        return bytes;
    }

    /**
     * Get the current connection status of the protocol.
     * @return The current connection status, or DISCONNECTED if not available
     */
    protected ConnectionStatus getConnectionStatus() {
        return agent.getAgentStatus().orElse(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Translate Modbus exception code to human-readable description.
     * @param exceptionCode The Modbus exception code
     * @return Human-readable description
     */
    protected static String getModbusExceptionDescription(byte exceptionCode) {
        return switch (exceptionCode & 0xFF) {
            case 0x01 -> "Illegal Function";
            case 0x02 -> "Illegal Data Address";
            case 0x03 -> "Illegal Data Value";
            case 0x04 -> "Slave Device Failure";
            case 0x05 -> "Acknowledge (request in queue)";
            case 0x06 -> "Slave Device Busy";
            case 0x08 -> "Memory Parity Error";
            case 0x0A -> "Gateway Path Unavailable";
            case 0x0B -> "Gateway Target Device Failed to Respond";
            default -> "Unknown Exception";
        };
    }

    /**
     * Check if the agent is currently disabled.
     * @return true if the agent is disabled, false otherwise
     */
    protected boolean isAgentDisabled() {
        return agent.isDisabled().orElse(false);
    }

    /**
     * Get the protocol instance URI for identification.
     * @return The connection string for this protocol instance
     */
    public String getProtocolInstanceUri() {
        return connectionString;
    }

    /**
     * Get a short identifier for this protocol (e.g., "tcp", "serial") for message IDs.
     * @return Short protocol identifier (lowercase protocol name)
     */
    protected String getProtocolShortName() {
        return getProtocolName();
    }


    /**
     * Build Modbus PDU for read request (functions 0x01-0x04).
     * PDU format: [Function Code][Start Address High][Start Address Low][Quantity High][Quantity Low]
     */
    protected byte[] buildReadRequestPDU(byte functionCode, int startAddress, int quantity) {
        byte[] pdu = new byte[5];
        pdu[0] = functionCode;
        pdu[1] = (byte) (startAddress >> 8);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) (quantity >> 8);
        pdu[4] = (byte) (quantity & 0xFF);
        return pdu;
    }

    /**
     * Build Modbus PDU for write single coil (function 0x05).
     * PDU format: [Function Code][Address High][Address Low][Value High][Value Low]
     */
    protected byte[] buildWriteSingleCoilPDU(int address, boolean value) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0x05;
        pdu[1] = (byte) (address >> 8);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = value ? (byte) 0xFF : (byte) 0x00;
        pdu[4] = (byte) 0x00;
        return pdu;
    }

    /**
     * Build Modbus PDU for write single register (function 0x06).
     * PDU format: [Function Code][Address High][Address Low][Value High][Value Low]
     */
    protected byte[] buildWriteSingleRegisterPDU(int address, int value) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0x06;
        pdu[1] = (byte) (address >> 8);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = (byte) (value >> 8);
        pdu[4] = (byte) (value & 0xFF);
        return pdu;
    }

    /**
     * Build Modbus PDU for write multiple registers (function 0x10).
     * PDU format: [Function Code][Start Address High][Start Address Low][Quantity High][Quantity Low][Byte Count][Values...]
     */
    protected byte[] buildWriteMultipleRegistersPDU(int startAddress, byte[] registerData) {
        int registerCount = registerData.length / 2;
        byte[] pdu = new byte[6 + registerData.length];
        pdu[0] = (byte) 0x10;
        pdu[1] = (byte) (startAddress >> 8);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) (registerCount >> 8);
        pdu[4] = (byte) (registerCount & 0xFF);
        pdu[5] = (byte) registerData.length;
        System.arraycopy(registerData, 0, pdu, 6, registerData.length);
        return pdu;
    }

    /**
     * Extract register data from a Modbus read response PDU.
     * Handles both register reads (0x03, 0x04) and coil/discrete reads (0x01, 0x02).
     * Returns the data bytes (after function code and byte count).
     */
    protected byte[] extractDataFromResponsePDU(byte[] responsePDU, byte functionCode) {
        if (responsePDU == null || responsePDU.length < 2) {
            return null;
        }

        // Response format: [Function Code][Byte Count][Data...]
        int byteCount = responsePDU[1] & 0xFF;
        if (responsePDU.length < 2 + byteCount) {
            return null;
        }

        byte[] data = new byte[byteCount];
        System.arraycopy(responsePDU, 2, data, 0, byteCount);
        return data;
    }
}
