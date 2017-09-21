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
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

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
        protected final AttributeType attributeType;
        protected final boolean readOnly;
        protected final boolean executable;

        public PropertyDescriptor(
            String name,
            String displayName,
            String linkName,
            AttributeType attributeType) {
            this(name, displayName, linkName, attributeType, false, false);
        }

        public PropertyDescriptor(
            String name,
            String displayName,
            String linkName,
            AttributeType attributeType,
            boolean readOnly,
            boolean executable) {
            this.name = name;
            this.displayName = displayName;
            this.linkName = linkName;
            this.attributeType = attributeType;
            this.readOnly = readOnly;
            this.executable = executable;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public AttributeType getAttributeType() {
            return attributeType;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public boolean isExecutable() {
            return executable;
        }

        public String getLinkName() {
            return linkName;
        }
    }

    public enum LedState implements DevicePropertyValue<LedState> {
        OFF(0x00),
        ON(0x80),
        SLOW(0x40),
        FAST(0x20),
        VERYFAST(0x10);

        private static final LedState[] values = values();
        private int code;

        LedState(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        @Override
        public Value toValue(ValueType valueType) {
            return EnumUtil.enumToValue(this, valueType);
        }

        @Override
        public LedState getPropertyValue() {
            return this;
        }

        public static LedState fromCode(int code) {
            for (LedState type : LedState.values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return OFF;
        }

    }

    public static class IntDevicePropertyValue implements DevicePropertyValue<Integer> {

        public static final IntDevicePropertyValue ZERO = new IntDevicePropertyValue(0);
        private int value;

        public IntDevicePropertyValue(int value) {
            this.value = value;
        }

        @Override
        public Value toValue(ValueType valueType) {
            switch (valueType) {
                case OBJECT:
                    return null;
                case ARRAY:
                    return null;
                case STRING:
                    return Values.create(Integer.toString(value));
                case NUMBER:
                    return Values.create(value);
                case BOOLEAN:
                    return value == 0 ? Values.create(false) : value == 1 ? Values.create(true) : null;
            }

            return null;
        }

        @Override
        public Integer getPropertyValue() {
            return value;
        }
    }

    public static class DoubleDevicePropertyValue implements DevicePropertyValue<Double> {

        public static final DoubleDevicePropertyValue ZERO = new DoubleDevicePropertyValue(0d);
        private double value;

        public DoubleDevicePropertyValue(double value) {
            this.value = value;
        }

        @Override
        public Value toValue(ValueType valueType) {
            switch (valueType) {
                case OBJECT:
                    return null;
                case ARRAY:
                    return null;
                case STRING:
                    return Values.create(Double.toString(value));
                case NUMBER:
                    return Values.create(value);
                case BOOLEAN:
                    return value == 0d ? Values.create(false) : value == 1d ? Values.create(true) : null;
            }

            return null;
        }

        @Override
        public Double getPropertyValue() {
            return value;
        }
    }

    public static class BooleanDevicePropertyValue implements DevicePropertyValue<Boolean> {

        public static final BooleanDevicePropertyValue FALSE = new BooleanDevicePropertyValue(false);
        public static final BooleanDevicePropertyValue TRUE = new BooleanDevicePropertyValue(true);
        private boolean value;

        public BooleanDevicePropertyValue(boolean value) {
            this.value = value;
        }

        @Override
        public Value toValue(ValueType valueType) {
            switch (valueType) {
                case OBJECT:
                    return null;
                case ARRAY:
                    return null;
                case STRING:
                    return Values.create(Boolean.toString(value));
                case NUMBER:
                    return Values.create(value ? 1d : 0d);
                case BOOLEAN:
                    return Values.create(value);
            }

            return null;
        }

        @Override
        public Boolean getPropertyValue() {
            return value;
        }
    }

    public static class StringDevicePropertyValue implements DevicePropertyValue<String> {

        public static final StringDevicePropertyValue EMPTY = new StringDevicePropertyValue("");
        private String value;

        public StringDevicePropertyValue(String value) {
            this.value = value;
        }

        @Override
        public Value toValue(ValueType valueType) {
            switch (valueType) {
                case OBJECT:
                    return null;
                case ARRAY:
                    return null;
                case STRING:
                    return Values.create(value);
                case NUMBER:
                    return null;
                case BOOLEAN:
                    return Values.create(value);
            }

            return null;
        }

        @Override
        public String getPropertyValue() {
            return value;
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
    public abstract List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Value value);

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
