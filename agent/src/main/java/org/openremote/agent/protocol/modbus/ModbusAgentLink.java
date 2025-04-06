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
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.AgentLink;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Optional;

//TODO: Make non-primitive parameters required
public class ModbusAgentLink extends AgentLink<ModbusAgentLink> {

    @JsonPropertyDescription("Poll interval in milliseconds")
    @NotNull
    private long refresh;

    @JsonPropertyDescription("Read type: \"coil\" (FC01), \"discrete\" (FC02), \"holding\" (FC03), \"input\" (FC04)")
    private ReadMemoryArea readMemoryArea;

    @JsonPropertyDescription("Read value type: \"int64\", \"int64_swap\", \"uint64\", \"uint64_swap\", \"float32\", \"float32_swap\", \"int32\", \"int32_swap\", \"uint32\", \"uint32_swap\", \"int16\", \"uint16\", \"int8\", \"uint8\", \"bit\"")
    private ModbusDataType readValueType;

    @JsonPropertyDescription("Zero based address for reading data")
    private int readAddress;

    @JsonPropertyDescription("Write type: \"coil\", \"holding\"")
    private WriteMemoryArea writeMemoryArea;

    @JsonPropertyDescription("Zero based address for writing data")
    private int writeAddress;

    @JsonPropertyDescription("Write value type: \"int64\", \"int64_swap\", \"float32\", \"float32_swap\", \"int32\", \"int32_swap\", \"int16\", \"bit\"")
    private ModbusDataType writeValueType;

    public long getRefresh() {
        return refresh;
    }

    public void setRefresh(long refresh) {
        this.refresh = refresh;
    }

    public ReadMemoryArea getReadMemoryArea() {
        return readMemoryArea;
    }

    public void setReadMemoryArea(ReadMemoryArea readMemoryArea) {
        this.readMemoryArea = readMemoryArea;
    }

    public ModbusDataType getReadValueType() {
        return readValueType;
    }

    public void setReadValueType(ModbusDataType readValueType) {
        this.readValueType = readValueType;
    }

    public int getReadAddress() {
        return readAddress;
    }

    public void setReadAddress(int readAddress) {
        this.readAddress = readAddress;
    }

    public WriteMemoryArea getWriteMemoryArea() {
        return writeMemoryArea;
    }

    public void setWriteMemoryArea(WriteMemoryArea writeMemoryArea) {
        this.writeMemoryArea = writeMemoryArea;
    }

    public Optional<Integer> getWriteAddress() {
        return Optional.ofNullable(writeAddress);
    }

    public void setWriteAddress(int writeAddress) {
        this.writeAddress = writeAddress;
    }

    public ModbusDataType getWriteValueType() {
        return writeValueType;
    }

    public void setWriteValueType(ModbusDataType writeValueType) {
        this.writeValueType = writeValueType;
    }

    // Enums for readMemoryArea, readValueType, and writeValueType
    public enum ReadMemoryArea {
        COIL, DISCRETE, HOLDING, INPUT
    }

    public enum ModbusDataType {
        BOOL(boolean.class),
        SINT(byte.class),
        USINT(short.class),
        BYTE(short.class),
        INT(short.class),
        UINT(int.class),
        WORD(int.class),
        DINT(int.class),
        UDINT(long.class),
        DWORD(long.class),
        LINT(long.class),
        ULINT(BigInteger.class),
        LWORD(BigInteger.class),
        REAL(float.class),
        LREAL(double.class),
        CHAR(char.class),
        WCHAR(String.class);

        private final Class<?> javaType;

        ModbusDataType(Class<?> javaType) {
            this.javaType = javaType;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }

    public enum WriteMemoryArea {
        COIL, HOLDING
    }

    public enum WriteValueType {
        INT64, INT64_SWAP, FLOAT32, FLOAT32_SWAP,
        INT32, INT32_SWAP, INT16, BIT
    }

    public ModbusAgentLink(String id) {
        super(id);
    }

    protected ModbusAgentLink() {
    }

    @JsonCreator
    public ModbusAgentLink(@JsonProperty("id") String id, @JsonProperty("refresh") long refresh, @JsonProperty("readMemoryArea") ReadMemoryArea readMemoryArea, @JsonProperty("readValueType") ModbusDataType readValueType, @JsonProperty("readAddress") int readAddress, @JsonProperty("writeMemoryArea") WriteMemoryArea writeMemoryArea, @JsonProperty("writeAddress") int writeAddress, @JsonProperty("writeValueType") ModbusDataType writeValueType) {
        super(id);
        this.refresh = refresh;
        this.readMemoryArea = readMemoryArea;
        this.readValueType = readValueType;
        this.readAddress = readAddress;
        this.writeMemoryArea = writeMemoryArea;
        this.writeAddress = writeAddress;
        this.writeValueType = writeValueType;
    }
}
