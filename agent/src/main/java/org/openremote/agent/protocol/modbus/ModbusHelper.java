//package org.openremote.agent.protocol.modbus;
//
//import com.digitalpetri.modbus.client.ModbusClient;
//import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
//import com.digitalpetri.modbus.exceptions.ModbusResponseException;
//import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
//import com.digitalpetri.modbus.pdu.*;
//
//import static org.openremote.agent.protocol.modbus.ModbusAgentLink.WriteType;
//import static org.openremote.agent.protocol.modbus.ModbusAgentLink.ReadType;
//
//public class ModbusHelper {
//
//    /**
//     * Performs a read operation on the provided {@link ModbusClient}
//     * and returns the resulting {@link ModbusResponsePdu}.
//     *
//     * @param client The {@link ModbusClient} used to perform operations
//     * @param unitId The Modbus slave/unit identifier
//     * @param readType The type of read operation to perform
//     * @param readAddress The address to read from
//     * @return The {@link ModbusResponsePdu} resulting from the read operation.
//     * @throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException
//     */
//    public static ModbusResponsePdu read(ModbusClient client, int unitId, ReadType readType, int readAddress)
//            throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {
//        return switch (readType) {
//            case COIL -> client.readCoils(unitId, new ReadCoilsRequest(readAddress, 1));
//            case DISCRETE -> client.readDiscreteInputs(unitId, new ReadDiscreteInputsRequest(readAddress, 1));
//            case HOLDING -> client.readHoldingRegisters(unitId, new ReadHoldingRegistersRequest(readAddress, 1));
//            case INPUT -> client.readInputRegisters(unitId, new ReadInputRegistersRequest(readAddress, 1));
//            default -> throw new ModbusExecutionException("Unsupported read type: " + readType);
//        };
//    }
//
//    /**
//     * Performs a write operation on the provided {@link ModbusClient}
//     * and returns the resulting {@link ModbusResponsePdu}.
//     *
//     * @param client The {@link ModbusClient} used to perform operations
//     * @param unitId The Modbus slave/unit identifier
//     * @param writeType The type of write operation to perform
//     * @param writeAddress The address to write to
//     * @param value The value to be written
//     * @return The {@link ModbusResponsePdu} resulting from the write operation.
//     * @throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException
//     */
//    public static ModbusResponsePdu write(ModbusClient client, int unitId, WriteType writeType, int writeAddress, int value)
//            throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {
//        return switch (writeType) {
//            case COIL -> {
//                boolean coilValue = (value != 0);
//                yield client.writeSingleCoil(unitId, new WriteSingleCoilRequest(writeAddress, coilValue));
//            }
//            case HOLDING -> {
//                yield client.writeSingleRegister(unitId, new WriteSingleRegisterRequest(writeAddress, value));
//            }
//            default -> throw new ModbusExecutionException("Unsupported write type: " + writeType);
//        };
//    }
//
//    public static Object parseResponse(ModbusResponsePdu response) throws ModbusExecutionException {
//        return switch (response) {
//            case ReadCoilsResponse r -> r.coils();
//            case ReadDiscreteInputsResponse r -> r.inputs();
//            case ReadHoldingRegistersResponse r -> r.registers();
//            case ReadInputRegistersResponse r -> r.registers();
//            case WriteSingleCoilResponse r -> r.value();
//            case WriteSingleRegisterResponse r -> r.value();
//            case WriteMultipleCoilsResponse r -> r.quantity();
//            case WriteMultipleRegistersResponse r -> r.quantity();
//            case MaskWriteRegisterResponse r -> r.orMask();
//            case ReadWriteMultipleRegistersResponse r -> r.registers();
//            default -> throw new ModbusExecutionException("Unexpected response type: " + response.getClass().getName());
//        };
//    }
//}
