package org.openremote.agent.protocol.modbus;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;
import java.util.function.Function;

public class ModbusLinkHelper {

    /**
     * Returns a Function that, given a {@link ModbusAgentLink},
     * calls the appropriate read function on the provided {@link ModbusClient}
     * and returns the resulting {@link ModbusResponsePdu}.
     *
     * Usage example:
     * <pre>{@code
     *   Function<ModbusAgentLink, ModbusResponsePdu> readFunction =
     *       ModbusLinkHelper.read(client, 1);
     *   ModbusResponsePdu responsePdu = readFunction.apply(agentLink);
     * }</pre>
     *
     * @param client The {@link ModbusClient} used to perform operations
     * @param unitId The Modbus slave/unit identifier
     * @return A Function that takes an agentLink and returns the {@link ModbusResponsePdu}.
     */
    public static Function<ModbusAgentLink, ModbusResponsePdu> read(ModbusClient client, int unitId) {
        return (ModbusAgentLink agentLink) -> {
            try {
                return switch (agentLink.getReadType()) {
                    case COIL -> {
                        // e.g. read 1 coil starting at readAddress
                        yield client.readCoils(
                                unitId,
                                new ReadCoilsRequest(agentLink.getReadAddress(), 1)
                        );
                    }
                    case DISCRETE -> {
                        // e.g. read 1 discrete input starting at readAddress
                        yield client.readDiscreteInputs(
                                unitId,
                                new ReadDiscreteInputsRequest(agentLink.getReadAddress(), 1)
                        );
                    }
                    case HOLDING -> {
                        // e.g. read 1 holding register starting at readAddress
                        yield client.readHoldingRegisters(
                                unitId,
                                new ReadHoldingRegistersRequest(agentLink.getReadAddress(), 1)
                        );
                    }
                    case INPUT -> {
                        // e.g. read 1 input register starting at readAddress
                        yield client.readInputRegisters(
                                unitId,
                                new ReadInputRegistersRequest(agentLink.getReadAddress(), 1)
                        );
                    }
                };
            } catch (ModbusExecutionException
                     | ModbusResponseException
                     | ModbusTimeoutException e) {
                // Wrap checked exceptions in a runtime exception (or handle as appropriate)
                throw new RuntimeException("Error reading Modbus data: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Returns a Function that, given a {@link ModbusAgentLink},
     * calls the appropriate write function on the provided {@link ModbusClient}
     * and returns the resulting {@link ModbusResponsePdu}.
     *
     * Usage example:
     * <pre>{@code
     *   Function<ModbusAgentLink, ModbusResponsePdu> writeFunction =
     *       ModbusLinkHelper.write(client, 1, 1); // 1 => coil ON or register value
     *   ModbusResponsePdu responsePdu = writeFunction.apply(agentLink);
     * }</pre>
     *
     * @param client The {@link ModbusClient} used to perform operations
     * @param unitId The Modbus slave/unit identifier
     * @param value  The value to be written (could be 0/1 for coils or an integer for registers)
     * @return A Function that takes an agentLink and returns the {@link ModbusResponsePdu}.
     */
    public static Function<ModbusAgentLink, ModbusResponsePdu> write(ModbusClient client, int unitId, int value) {
        return (ModbusAgentLink agentLink) -> {
            try {
                return switch (agentLink.getWriteType()) {
                    case COIL -> {
                        // Assume single coil write: 0 => OFF, nonzero => ON
                        boolean coilValue = (value != 0);
                        yield client.writeSingleCoil(
                                unitId,
                                new WriteSingleCoilRequest(agentLink.getWriteAddress(), coilValue)
                        );
                    }
                    case HOLDING -> {
                        // Assume single register write
                        yield client.writeSingleRegister(
                                unitId,
                                new WriteSingleRegisterRequest(agentLink.getWriteAddress(), value)
                        );
                    }
                };
            } catch (ModbusExecutionException
                     | ModbusResponseException
                     | ModbusTimeoutException e) {
                // Wrap checked exceptions in a runtime exception (or handle as appropriate)
                throw new RuntimeException("Error writing Modbus data: " + e.getMessage(), e);
            }
        };
    }
}
