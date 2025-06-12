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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.AgentLink;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Optional;

//TODO: Make non-primitive parameters required
public class ModbusAgentLink extends AgentLink<ModbusAgentLink> {

    @JsonProperty(required=true)
    @JsonPropertyDescription("Poll interval in milliseconds")
    private Long pollingMillis;

    @JsonProperty(required=true)
    @JsonPropertyDescription("Memory area to read from during read request")
    private ReadMemoryArea readMemoryArea;

    @JsonProperty(required=true)
    @JsonPropertyDescription("Type to convert the returned data to. As specified by the PLC4X Modbus data types.")
    private ModbusDataType readValueType;

    @JsonProperty(required=true)
    @JsonPropertyDescription("Zero based address from which the value is read from")
    private Integer readAddress;

    @JsonPropertyDescription("Memory area to write to. \"HOLDING\" or \"COIL\" allowed.")
    private WriteMemoryArea writeMemoryArea;

    @JsonPropertyDescription("Zero-based address to which the value sent is written to")
    private Integer writeAddress;

    @JsonPropertyDescription("Set amount of registers to read. If left empty or less than 1, will use the default size for the corresponding data-type.")
    private Integer readRegistersAmount;

    public long getPollingMillis() {
        return pollingMillis;
    }

    public void setPollingMillis(long pollingMillis) {
        this.pollingMillis = pollingMillis;
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

    public Optional<Integer> getReadAddress() {
        return Optional.ofNullable(readAddress);
    }

    public void setReadAddress(Integer readAddress) {
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

    public void setWriteAddress(Integer writeAddress) {
        this.writeAddress = writeAddress;
    }

    public Optional<Integer> getReadRegistersAmount() {
        return Optional.ofNullable(readRegistersAmount);
    }

    public void setReadRegistersAmount(Integer readRegistersAmount) {
        this.readRegistersAmount = writeAddress;
    }

    public enum ReadMemoryArea {
        COIL, DISCRETE, HOLDING, INPUT
    }

    public enum ModbusDataType {
        BOOL(boolean.class, 1),
        SINT(byte.class, 1),
        USINT(short.class, 1),
        BYTE(short.class, 1),
        INT(short.class, 1),
        UINT(int.class, 1),
        WORD(int.class, 1),
        DINT(int.class, 2),
        UDINT(long.class, 2),
        DWORD(long.class, 2),
        LINT(long.class, 4),
        ULINT(BigInteger.class, 4),
        LWORD(BigInteger.class, 4),
        REAL(float.class, 2),
        LREAL(double.class, 4),
        CHAR(char.class, 1),
        WCHAR(String.class, 1);  // Assumes single wchar per entry

        private final Class<?> javaType;
        private final Integer registerCount;

        ModbusDataType(Class<?> javaType, Integer registerCount) {
            this.javaType = javaType;
            this.registerCount = registerCount;
        }

        public Class<?> getJavaType() {
            return javaType;
        }

        public Integer getRegisterCount() {
            return registerCount;
        }
    }

    public enum WriteMemoryArea {
        COIL, HOLDING
    }


    public ModbusAgentLink(String id) {super(id);}

    protected ModbusAgentLink() {
    }
}
