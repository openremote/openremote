package org.openremote.agent.protocol.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.AgentLink;

import java.math.BigInteger;

public class ModbusAgentLink extends AgentLink<ModbusAgentLink> {

    @JsonPropertyDescription("Identifier of Modbus TCP server or Modbus serial slave")
    private int unitId;

    @JsonPropertyDescription("Poll interval in milliseconds")
    private long refresh;

    @JsonPropertyDescription("Read type: \"coil\" (FC01), \"discrete\" (FC02), \"holding\" (FC03), \"input\" (FC04)")
    private ReadType readType;

    @JsonPropertyDescription("Read value type: \"int64\", \"int64_swap\", \"uint64\", \"uint64_swap\", \"float32\", \"float32_swap\", \"int32\", \"int32_swap\", \"uint32\", \"uint32_swap\", \"int16\", \"uint16\", \"int8\", \"uint8\", \"bit\"")
    private ModbusDataType readValueType;

    @JsonPropertyDescription("Zero based address for reading data")
    private int readAddress;

    @JsonPropertyDescription("Write type: \"coil\", \"holding\"")
    private WriteType writeType;

    @JsonPropertyDescription("Zero based address for writing data")
    private int writeAddress;

    @JsonPropertyDescription("Write value type: \"int64\", \"int64_swap\", \"float32\", \"float32_swap\", \"int32\", \"int32_swap\", \"int16\", \"bit\"")
    private WriteValueType writeValueType;

    // Getters and setters for each variable
    public int getUnitId() {
        return unitId;
    }

    public void getUnitId(int id) {
        this.unitId = id;
    }

    public long getRefresh() {
        return refresh;
    }

    public void setRefresh(long refresh) {
        this.refresh = refresh;
    }

    public ReadType getReadType() {
        return readType;
    }

    public void setReadType(ReadType readType) {
        this.readType = readType;
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

    public WriteType getWriteType() {
        return writeType;
    }

    public void setWriteType(WriteType writeType) {
        this.writeType = writeType;
    }

    public int getWriteAddress() {
        return writeAddress;
    }

    public void setWriteAddress(int writeAddress) {
        this.writeAddress = writeAddress;
    }

    public WriteValueType getWriteValueType() {
        return writeValueType;
    }

    public void setWriteValueType(WriteValueType writeValueType) {
        this.writeValueType = writeValueType;
    }

    // Enums for readType, readValueType, and writeValueType
    public enum ReadType {
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

    public enum WriteType {
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
    public ModbusAgentLink(@JsonProperty("id") String id, @JsonProperty int unitId, @JsonProperty("refresh") long refresh, @JsonProperty("readType") ReadType readType, @JsonProperty("readValueType") ModbusDataType readValueType, @JsonProperty("readAddress") int readAddress, @JsonProperty("writeType") WriteType writeType, @JsonProperty("writeAddress") int writeAddress, @JsonProperty("writeValueType") WriteValueType writeValueType) {
        super(id);
        this.unitId = unitId;
        this.refresh = refresh;
        this.readType = readType;
        this.readValueType = readValueType;
        this.readAddress = readAddress;
        this.writeType = writeType;
        this.writeAddress = writeAddress;
        this.writeValueType = writeValueType;
    }
}
