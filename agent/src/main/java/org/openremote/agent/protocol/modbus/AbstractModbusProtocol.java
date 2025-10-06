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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

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

    protected final Map<AttributeRef, ScheduledFuture<?>> pollingMap = new HashMap<>();
    protected final Map<String, Map<AttributeRef, ModbusAgentLink>> batchGroups = new ConcurrentHashMap<>();
    protected final Map<String, List<BatchReadRequest>> cachedBatches = new ConcurrentHashMap<>();
    protected final Map<String, ScheduledFuture<?>> batchPollingTasks = new ConcurrentHashMap<>();
    protected final Map<AttributeRef, ScheduledFuture<?>> writePollingMap = new HashMap<>();

    protected Set<RegisterRange> illegalRegisters = new HashSet<>();

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

    /**
     * Get illegal registers configuration from agent. Subclasses must implement.
     */
    protected abstract Optional<String> getIllegalRegistersConfig();

    @Override
    protected void doStart(Container container) throws Exception {
        // Parse illegal registers from agent config
        illegalRegisters = parseIllegalRegisters(getIllegalRegistersConfig().orElse(""));
        LOG.info("Loaded " + illegalRegisters.size() + " illegal register ranges for modbus agent " + agent.getId());

        // Call protocol-specific start
        doStartProtocol(container);
    }

    /**
     * Protocol-specific start logic. Subclasses implement connection setup here.
     */
    protected abstract void doStartProtocol(Container container) throws Exception;

    @Override
    protected void doStop(Container container) throws Exception {
        pollingMap.forEach((key, value) -> value.cancel(false));
        pollingMap.clear();

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

        // Check if batching is enabled (maxRegisterLength > 1)
        Integer maxRegisterLength = getMaxRegisterLength();
        if (maxRegisterLength > 1) {
            // Use batching logic
            String groupKey = agent.getId() + "_" + agentLink.getReadMemoryArea() + "_" + agentLink.getPollingMillis();

            batchGroups.computeIfAbsent(groupKey, k -> new ConcurrentHashMap<>()).put(ref, agentLink);
            cachedBatches.remove(groupKey); // Invalidate cache

            // Only schedule task if one doesn't exist yet for this group
            if (!batchPollingTasks.containsKey(groupKey)) {
                ScheduledFuture<?> batchTask = scheduleBatchedPollingTask(groupKey, agentLink.getReadMemoryArea(), agentLink.getPollingMillis());
                batchPollingTasks.put(groupKey, batchTask);
                LOG.fine("Scheduled new polling task for batch group " + groupKey);
            }

            LOG.fine("Added attribute " + ref + " to batch group " + groupKey + " (total attributes in group: " + batchGroups.get(groupKey).size() + ")");
        } else {
            // Use individual polling (legacy mode, maxRegisterLength = 1)
            pollingMap.put(ref,
                    scheduleModbusPollingReadRequest(
                        ref,
                        agentLink.getPollingMillis(),
                        agentLink.getReadMemoryArea(),
                        agentLink.getReadValueType(),
                        agentLink.getRegistersAmount(),
                        agentLink.getReadAddress()
                    )
            );
        }

        // Check if write polling is enabled
        if (agentLink.getWriteWithPollingRate().orElse(false) && agentLink.getWriteAddress().isPresent()) {
            ScheduledFuture<?> writeTask = scheduleModbusPollingWriteRequest(ref, agentLink);
            writePollingMap.put(ref, writeTask);
            LOG.fine("Scheduled write polling task for attribute " + ref + " every " + agentLink.getPollingMillis() + "ms");
        }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // Remove from individual polling
        ScheduledFuture<?> pollTask = pollingMap.remove(attributeRef);
        if (pollTask != null) {
            pollTask.cancel(false);
        }

        // Remove from write polling
        ScheduledFuture<?> writeTask = writePollingMap.remove(attributeRef);
        if (writeTask != null) {
            writeTask.cancel(false);
            LOG.fine("Cancelled write polling task for attribute " + attributeRef);
        }

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

    /**
     * Get the maximum register length for batching. Override in subclass if agent supports this configuration.
     */
    protected Integer getMaxRegisterLength() {
        return 1; // Default: no batching
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
                    .sorted(Comparator.comparingInt(e -> e.getValue().getReadAddress().orElse(0)))
                    .collect(Collectors.toList());

            BatchReadRequest currentBatch = null;
            int currentEnd = -1;

            for (Map.Entry<AttributeRef, ModbusAgentLink> entry : sortedAttributes) {
                ModbusAgentLink link = entry.getValue();
                int address = link.getReadAddress().orElse(0);
                int registerCount = link.getRegistersAmount().orElse(link.getReadValueType().getRegisterCount());

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
                        LOG.info("Creating batch requests for group " + groupKey + " with " + group.size() + " attribute(s) (maxRegisterLength=" + getMaxRegisterLength() + ")");
                        batches = createBatchRequests(group, illegalRegisters, getMaxRegisterLength());
                        cachedBatches.put(groupKey, batches);
                        LOG.info("Created " + batches.size() + " batch(es) for group " + groupKey);
                    }
                }

                // Execute all batches for this group
                for (int i = 0; i < batches.size(); i++) {
                    BatchReadRequest batch = batches.get(i);
                    int endAddress = batch.startAddress + batch.quantity - 1;
                    LOG.finest("Executing batch " + (i + 1) + "/" + batches.size() + ": registers=" + batch.startAddress + "-" + endAddress + " (quantity=" + batch.quantity + ", attributes=" + batch.attributes.size() + ")");
                    executeBatchRead(batch, memoryArea, group);
                }

            } catch (Exception e) {
                LOG.log(Level.FINE, "Exception during batched polling: " + e.getMessage(), e);
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Execute a batch read request. Must be implemented by subclasses for protocol-specific communication.
     */
    protected abstract void executeBatchRead(BatchReadRequest batch, ModbusAgentLink.ReadMemoryArea memoryArea, Map<AttributeRef, ModbusAgentLink> group);

    /**
     * Schedule individual polling request for a single attribute (used when batching is disabled).
     * Subclasses must implement protocol-specific polling logic.
     */
    protected abstract ScheduledFuture<?> scheduleModbusPollingReadRequest(
            AttributeRef ref,
            long pollingMillis,
            ModbusAgentLink.ReadMemoryArea readType,
            ModbusAgentLink.ModbusDataType dataType,
            Optional<Integer> amountOfRegisters,
            Optional<Integer> readAddress
    );

    /**
     * Schedule a polling write request for an attribute with writeWithPollingRate enabled.
     * Subclasses must implement protocol-specific polling write logic.
     */
    protected abstract ScheduledFuture<?> scheduleModbusPollingWriteRequest(
            AttributeRef ref,
            ModbusAgentLink agentLink
    );
}
