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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.value.ValueDescriptor;

import java.util.HashMap;

@Entity
public abstract class ModbusAgent<T extends ModbusAgent<T, U>, U extends AbstractModbusProtocol<U, T>> extends Agent<T, U, ModbusAgentLink> {

    public enum EndianFormat {
        BIG_ENDIAN,              // ABCD - Big byte order, Big word order
        LITTLE_ENDIAN,           // DCBA - Little byte order, Little word order
        BIG_ENDIAN_BYTE_SWAP,    // BADC - Big byte order, Little word order
        LITTLE_ENDIAN_BYTE_SWAP; // CDAB - Little byte order, Big word order

        @JsonValue
        public String getJsonValue() {
            return toString();
        }
    }

    /**
     * Configuration for a Modbus device (per unit ID).
     * Contains all device-specific settings.
     */
    public static class ModbusDeviceConfig implements java.io.Serializable {
        private EndianFormat endianFormat;
        private String illegalRegisters;
        private Integer maxRegisterLength;

        @JsonCreator
        public ModbusDeviceConfig(
                @JsonProperty("endianFormat") EndianFormat endianFormat,
                @JsonProperty("illegalRegisters") String illegalRegisters,
                @JsonProperty("maxRegisterLength") Integer maxRegisterLength) {
            this.endianFormat = endianFormat != null ? endianFormat : EndianFormat.BIG_ENDIAN;
            this.illegalRegisters = illegalRegisters != null ? illegalRegisters : "";
            this.maxRegisterLength = maxRegisterLength != null ? maxRegisterLength : 1;
        }

        /**
         * Default configuration factory method
         */
        public static ModbusDeviceConfig createDefault() {
            return new ModbusDeviceConfig(EndianFormat.BIG_ENDIAN, "", 1);
        }

        public EndianFormat getEndianFormat() {
            return endianFormat;
        }

        public void setEndianFormat(EndianFormat endianFormat) {
            this.endianFormat = endianFormat;
        }

        public String getIllegalRegisters() {
            return illegalRegisters;
        }

        public void setIllegalRegisters(String illegalRegisters) {
            this.illegalRegisters = illegalRegisters;
        }

        public Integer getMaxRegisterLength() {
            return maxRegisterLength;
        }

        public void setMaxRegisterLength(Integer maxRegisterLength) {
            this.maxRegisterLength = maxRegisterLength;
        }
    }

    // Map type for per-unitId device configuration (unitId string -> ModbusDeviceConfig)
    public static class DeviceConfigMap extends HashMap<String, ModbusDeviceConfig> {}
    public static final ValueDescriptor<DeviceConfigMap> VALUE_DEVICE_CONFIG_MAP = new ValueDescriptor<>("DeviceConfigMap", DeviceConfigMap.class);

    // Shared device configuration attribute descriptor
    public static final org.openremote.model.value.AttributeDescriptor<DeviceConfigMap> DEVICE_CONFIG =
        new org.openremote.model.value.AttributeDescriptor<>("deviceConfig", VALUE_DEVICE_CONFIG_MAP);

    // For Hydrators
    protected ModbusAgent() {}

    protected ModbusAgent(String name) {
        super(name);
    }
}
