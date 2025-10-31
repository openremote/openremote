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

    // Custom map type for per-unitId EndianFormat configuration
    public static class EndianFormatMap extends HashMap<String, EndianFormat> {}
    public static final ValueDescriptor<EndianFormatMap> VALUE_ENDIAN_FORMAT_MAP = new ValueDescriptor<>("EndianFormatMap", EndianFormatMap.class);

    // For Hydrators
    protected ModbusAgent() {}

    protected ModbusAgent(String name) {
        super(name);
    }
}
