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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.AgentLink;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Optional;

//TODO: Make non-primitive parameters required
public class ModbusAgentLink extends AgentLink<ModbusAgentLink> {

    @NotNull
    @JsonPropertyDescription("Poll interval in milliseconds")
    private long pollingMillis;

    @NotNull
    @JsonPropertyDescription("Memory area to read from during read request")
    private ReadMemoryArea readMemoryArea;

    @NotNull
    @JsonPropertyDescription("Type to convert the returned data to. As specified by the PLC4X Modbus data types.")
    private ModbusDataType readValueType;

    @NotNull
    @JsonPropertyDescription("Zero based address from which the value is read from")
    private int readAddress;

    @NotNull
    @JsonPropertyDescription("Memory area to write to. \"HOLDING\" or \"COIL\" allowed.")
    private WriteMemoryArea writeMemoryArea;

    @NotNull
    @JsonPropertyDescription("Zero-based address to which the value sent is written to")
    private int writeAddress;

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
        return Optional.of(writeAddress);
    }

    public void setWriteAddress(int writeAddress) {
        this.writeAddress = writeAddress;
    }

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


    public ModbusAgentLink(String id) {super(id);}

    protected ModbusAgentLink() {
    }
}
