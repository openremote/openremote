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
package org.openremote.agent.protocol.modbus.util;

import org.openremote.model.attribute.AttributeRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a batched Modbus read request that combines multiple attribute reads
 * into a single register read operation for efficiency.
 */
public class BatchReadRequest {
    private final int startAddress;
    private int quantity;
    private final List<AttributeRef> attributes;
    private final List<Integer> offsets; // Offset of each attribute within the batch

    public BatchReadRequest(int startAddress, int quantity) {
        this.startAddress = startAddress;
        this.quantity = quantity;
        this.attributes = new ArrayList<>();
        this.offsets = new ArrayList<>();
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<AttributeRef> getAttributes() {
        return attributes;
    }

    public List<Integer> getOffsets() {
        return offsets;
    }

    /**
     * Add an attribute to this batch read request.
     * @param ref the attribute reference
     * @param offset the offset within the batch (in registers/coils from startAddress)
     */
    public void addAttribute(AttributeRef ref, int offset) {
        attributes.add(ref);
        offsets.add(offset);
    }

    @Override
    public String toString() {
        return "BatchReadRequest{" +
                "startAddress=" + startAddress +
                ", quantity=" + quantity +
                ", attributeCount=" + attributes.size() +
                '}';
    }
}
