/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol.velbus.device;

import org.openremote.agent.protocol.velbus.VelbusPacket;
import org.openremote.model.value.ValueDescriptor;

import java.util.Collections;
import java.util.List;

/**
 * Base class for reading/writing a set of device properties that relate to a feature/capability.
 *
 * A {@link FeatureProcessor} will be used as a singleton and must therefore not store any device specific state.
 */
public abstract class FeatureProcessor {

    public static class PropertyDescriptor {

        protected final String name;
        protected final String displayName;
        protected final String linkName;
        protected final ValueDescriptor<?> attributeValueDescriptor;
        protected final boolean readOnly;

        public PropertyDescriptor(
            String name,
            String displayName,
            String linkName,
            ValueDescriptor<?> attributeValueDescriptor) {
            this(name, displayName, linkName, attributeValueDescriptor, false);
        }

        public PropertyDescriptor(
            String name,
            String displayName,
            String linkName,
            ValueDescriptor<?> attributeValueDescriptor,
            boolean readOnly) {
            this.name = name;
            this.displayName = displayName;
            this.linkName = linkName;
            this.attributeValueDescriptor = attributeValueDescriptor;
            this.readOnly = readOnly;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ValueDescriptor<?> getAttributeValueDescriptor() {
            return attributeValueDescriptor;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public String getLinkName() {
            return linkName;
        }
    }

    public enum LedState {
        OFF(0x00),
        ON(0x80),
        SLOW(0x40),
        FAST(0x20),
        VERYFAST(0x10);

        private static final LedState[] values = values();
        private final int code;

        LedState(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        public static LedState fromCode(int code) {
            for (LedState type : values) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return OFF;
        }
    }

    protected FeatureProcessor() {}

    public abstract List<PropertyDescriptor> getPropertyDescriptors(VelbusDeviceType deviceType);

    /**
     * Get the packets that need to be sent to the device to get the state data of this feature's properties
     */
    public List<VelbusPacket> getStatusRequestPackets(VelbusDevice device) {
        return Collections.emptyList();
    }

    /**
     * Asks the processor to generate the packets required to write the requested value to this property.
     * <p>
     * Once a processor returns a non null value then no other processors will be asked to handle the write
     */
    public abstract List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Object value);

    /**
     * Allows this feature processor to handle the inbound packet. If a processor handles a packet then it should set
     * the packets {@link VelbusPacket#isHandled()} flag.
     * <p>
     * The processor can prevent other processors from processing the packet by returning true (processors should only
     * do this if they are certain no other processor is interested in the packet).
     *
     * @return whether or not to allow other processors to process the packet.
     */
    public abstract boolean processReceivedPacket(VelbusDevice device, VelbusPacket packet);
}
