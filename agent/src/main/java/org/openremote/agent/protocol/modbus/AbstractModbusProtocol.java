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
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractModbusProtocol<S extends AbstractModbusProtocol<S,T>, T extends ModbusAgent<T, S>>
        extends AbstractProtocol<T, ModbusAgentLink>{

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractModbusProtocol.class);

    protected final Map<String, Map<AttributeRef, ModbusAgentLink>> batchGroups = new ConcurrentHashMap<>();
    protected final Map<String, List<BatchReadRequest>> cachedBatches = new ConcurrentHashMap<>();
    protected final Map<String, ScheduledFuture<?>> batchPollingTasks = new ConcurrentHashMap<>();
    protected final Map<AttributeRef, ScheduledFuture<?>> writePollingMap = new HashMap<>();

    protected Set<RegisterRange> illegalRegisters = new HashSet<>();

    // Track failed message identifiers - only clear ERROR state when this set is empty
    protected final Set<String> failedMessageIds = ConcurrentHashMap.newKeySet();

    /**
     * Get per-unitId device configuration from the agent.
     * Returns map of unitId string -> ModbusDeviceConfig, with "default" key for default config.
     */
    protected abstract Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig();

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

        // Fall back to "default" key, or create default config if not present
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
        // BIG_ENDIAN and BIG_ENDIAN_BYTE_SWAP use big-endian byte order
        // LITTLE_ENDIAN and LITTLE_ENDIAN_BYTE_SWAP use little-endian byte order
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

        // No swapping needed for single register or BIG_ENDIAN/LITTLE_ENDIAN formats
        if (registerCount <= 1 || format == ModbusAgent.EndianFormat.BIG_ENDIAN || format == ModbusAgent.EndianFormat.LITTLE_ENDIAN) {
            return data;
        }

        // BYTE_SWAP formats: reverse the order of registers
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
                    LOG.warning("Invalid float value (NaN or Infinity), ignoring");
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
                    LOG.warning("Invalid double value (NaN or Infinity), ignoring");
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

    public AbstractModbusProtocol(T agent) {
        super(agent);
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

        boolean overlaps(int start, int end) {
            return !(this.end < start || this.start > end);
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
                    LOG.warning("Invalid register range format: " + part);
                }
            } else {
                try {
                    int register = Integer.parseInt(part);
                    ranges.add(new RegisterRange(register, register));
                } catch (NumberFormatException e) {
                    LOG.warning("Invalid register format: " + part);
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
            LOG.info("Initializing deviceConfig with default configuration for agent: " + agent.getName());
            ModbusAgent.DeviceConfigMap defaultConfig = new ModbusAgent.DeviceConfigMap();
            defaultConfig.put("default", ModbusAgent.ModbusDeviceConfig.createDefault());
            sendAttributeEvent(new AttributeEvent(agent.getId(), ModbusAgent.DEVICE_CONFIG, defaultConfig));
        }

        // Call protocol-specific start
        doStartProtocol(container);
    }

    /**
     * Protocol-specific start logic. Subclasses implement connection setup here.
     */
    protected abstract void doStartProtocol(Container container) throws Exception;

    @Override
    protected void doStop(Container container) throws Exception {
        batchPollingTasks.values().forEach(future -> future.cancel(false));
        batchPollingTasks.clear();
        batchGroups.clear();
        cachedBatches.clear();

        writePollingMap.forEach((key, value) -> value.cancel(false));
        writePollingMap.clear();

        // Call protocol-specific stop
        doStopProtocol(container);
    }

    /**
     * Protocol-specific stop logic. Subclasses implement connection cleanup here.
     */
    protected abstract void doStopProtocol(Container container) throws Exception;

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) throws RuntimeException {
            AttributeRef ref = new AttributeRef(assetId, attribute.getName());

            // Check if read configuration is present (all required parameters)
            boolean hasReadConfig = agentLink.getReadMemoryArea() != null
                    && agentLink.getReadValueType() != null
                    && agentLink.getReadAddress() != null
                    && agentLink.getUnitId() != null;

            if (hasReadConfig) {
                // Setup read polling using batching (works for single or multiple attributes)
                // Group by agent, unitId, memory area, and polling interval
                String groupKey = agent.getId() + "_" + agentLink.getUnitId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getPollingMillis();

                batchGroups.computeIfAbsent(groupKey, k -> new ConcurrentHashMap<>()).put(ref, agentLink);
                cachedBatches.remove(groupKey); // Invalidate cache

                // Only schedule task if one doesn't exist yet for this group
                if (!batchPollingTasks.containsKey(groupKey)) {
                    ScheduledFuture<?> batchTask = scheduleBatchedPollingTask(groupKey, agentLink.getReadMemoryArea(), agentLink.getPollingMillis(), agentLink.getUnitId());
                    batchPollingTasks.put(groupKey, batchTask);
                    LOG.fine("Scheduled new polling task for batch group " + groupKey);
                }

                LOG.fine("Added attribute " + ref + " to batch group " + groupKey + " (total attributes in group: " + batchGroups.get(groupKey).size() + ")");
            } else {
                LOG.fine("Skipping read polling for " + ref + " - read configuration incomplete (unitId, readMemoryArea, readValueType, and readAddress all required)");
            }

            // Check if write polling is enabled and reading is not
            if (Optional.ofNullable(agentLink.getWriteWithPollingRate()).orElse(false) && Optional.ofNullable(agentLink.getWriteAddress()).isPresent() && agentLink.getReadAddress() == null) {
                ScheduledFuture<?> writeTask = scheduleModbusPollingWriteRequest(ref, agentLink);
                writePollingMap.put(ref, writeTask);
                LOG.fine("Scheduled write polling task for attribute " + ref + " every " + agentLink.getPollingMillis() + "ms");
            }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // Remove from write polling
        ScheduledFuture<?> writeTask = writePollingMap.remove(attributeRef);
        if (writeTask != null) {
            writeTask.cancel(false);
            LOG.fine("Cancelled write polling task for attribute " + attributeRef);
        }

        // Remove from batch group (if read config was present)
        if (agentLink.getReadMemoryArea() != null) {
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
                LOG.fine("Started new batch at address " + address);
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
     * Schedule a batched polling task for a group of attributes
     */
    protected ScheduledFuture<?> scheduleBatchedPollingTask(String groupKey, ModbusAgentLink.ReadMemoryArea memoryArea, long pollingMillis, Integer unitId) {
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
                        // Get per-unitId configuration
                        int maxRegisterLength = getMaxRegisterLength(unitId);
                        String illegalRegistersStr = getIllegalRegistersString(unitId);
                        Set<RegisterRange> illegalRegistersForUnit = parseIllegalRegisters(illegalRegistersStr);

                        LOG.info("Creating batch requests for group " + groupKey + " with " + group.size() + " attribute(s) (unitId=" + unitId + ", maxRegisterLength=" + maxRegisterLength + ")");
                        batches = createBatchRequests(group, illegalRegistersForUnit, maxRegisterLength);
                        cachedBatches.put(groupKey, batches);
                    }
                }

                // Execute all batches for this group
                for (int i = 0; i < batches.size(); i++) {
                    BatchReadRequest batch = batches.get(i);
                    int endAddress = batch.startAddress + batch.quantity - 1;
                    LOG.finest("Executing batch " + (i + 1) + "/" + batches.size() + ": registers=" + batch.startAddress + "-" + endAddress + " (quantity=" + batch.quantity + ", attributes=" + batch.attributes.size() + ")");
                    executeBatchRead(batch, memoryArea, group, unitId);
                }

            } catch (Exception e) {
                LOG.log(Level.FINE, "Exception during batched polling: " + e.getMessage(), e);
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Execute a batch read request. Must be implemented by subclasses for protocol-specific communication.
     */
    protected abstract void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group, Integer unitId);

    /**
     * Schedule a polling write request for an attribute with writeWithPollingRate enabled.
     * Subclasses must implement protocol-specific polling write logic.
     */
    protected abstract ScheduledFuture<?> scheduleModbusPollingWriteRequest(
            AttributeRef ref,
            ModbusAgentLink agentLink
    );

    /**
     * Get the current connection status of the protocol.
     * @return The current connection status, or DISCONNECTED if not available
     */
    protected ConnectionStatus getConnectionStatus() {
        return agent.getAgentStatus().orElse(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Check if the agent is currently disabled.
     * @return true if the agent is disabled, false otherwise
     */
    protected boolean isAgentDisabled() {
        return agent.isDisabled().orElse(false);
    }

    /**
     * Called when a Modbus request succeeds. Removes this message from failed set.
     * If all messages have succeeded (failed set is empty), recovers connection to CONNECTED.
     * @param messageId Unique identifier for this message/request
     */
    protected void onRequestSuccess(String messageId) {
        failedMessageIds.remove(messageId);

        // Don't change status if agent is disabled
        if (isAgentDisabled()) {
            return;
        }

        if (getConnectionStatus() == ConnectionStatus.ERROR && failedMessageIds.isEmpty()) {
            setConnectionStatus(ConnectionStatus.CONNECTED);
            LOG.info("Connection recovered - all messages now succeeding");
        }
    }

    /**
     * Called when a Modbus request fails. Adds this message to failed set and sets connection status to ERROR.
     * @param messageId Unique identifier for this message/request
     * @param operation Description of the operation that failed
     * @param e The exception that occurred
     */
    protected void onRequestFailure(String messageId, String operation, Exception e) {
        failedMessageIds.add(messageId);

        if (!isAgentDisabled() && getConnectionStatus() != ConnectionStatus.ERROR ) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.log(Level.WARNING, "Request failed for " + operation + " [id=" + messageId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Called when a Modbus request times out or receives invalid response.
     * Adds this message to failed set and sets connection status to ERROR.
     * @param messageId Unique identifier for this message/request
     * @param operation Description of the operation that failed
     * @param reason Reason for the failure
     */
    protected void onRequestFailure(String messageId, String operation, String reason) {
        failedMessageIds.add(messageId);

        if (!isAgentDisabled() && getConnectionStatus() != ConnectionStatus.ERROR) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.warning("Request failed for " + operation + " [id=" + messageId + "]: " + reason);
        }
    }
}
