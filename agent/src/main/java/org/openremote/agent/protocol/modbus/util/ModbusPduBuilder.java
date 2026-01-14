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
package org.openremote.agent.protocol.modbus.util;

/**
 * Utility class for building and parsing Modbus Protocol Data Units (PDUs).
 */
public final class ModbusPduBuilder {

    private ModbusPduBuilder() {
        // Utility class
    }

    /**
     * Build a read request PDU.
     * @param functionCode the function code (0x01-0x04)
     * @param startAddress the starting address (0-based)
     * @param quantity the number of registers/coils to read
     * @return the PDU bytes
     */
    public static byte[] buildReadRequestPDU(byte functionCode, int startAddress, int quantity) {
        byte[] pdu = new byte[5];
        pdu[0] = functionCode;
        pdu[1] = (byte) (startAddress >> 8);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) (quantity >> 8);
        pdu[4] = (byte) (quantity & 0xFF);
        return pdu;
    }

    /**
     * Build a write single coil PDU (function code 0x05).
     * @param address the coil address (0-based)
     * @param value true for ON, false for OFF
     * @return the PDU bytes
     */
    public static byte[] buildWriteSingleCoilPDU(int address, boolean value) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0x05;
        pdu[1] = (byte) (address >> 8);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = value ? (byte) 0xFF : (byte) 0x00;
        pdu[4] = (byte) 0x00;
        return pdu;
    }

    /**
     * Build a write single register PDU (function code 0x06).
     * @param address the register address (0-based)
     * @param value the 16-bit value to write
     * @return the PDU bytes
     */
    public static byte[] buildWriteSingleRegisterPDU(int address, int value) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0x06;
        pdu[1] = (byte) (address >> 8);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = (byte) (value >> 8);
        pdu[4] = (byte) (value & 0xFF);
        return pdu;
    }

    /**
     * Build a write multiple registers PDU (function code 0x10).
     * @param startAddress the starting address (0-based)
     * @param registerData the register data bytes (2 bytes per register)
     * @return the PDU bytes
     */
    public static byte[] buildWriteMultipleRegistersPDU(int startAddress, byte[] registerData) {
        int registerCount = registerData.length / 2;
        byte[] pdu = new byte[6 + registerData.length];
        pdu[0] = (byte) 0x10;
        pdu[1] = (byte) (startAddress >> 8);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) (registerCount >> 8);
        pdu[4] = (byte) (registerCount & 0xFF);
        pdu[5] = (byte) registerData.length;
        System.arraycopy(registerData, 0, pdu, 6, registerData.length);
        return pdu;
    }

    /**
     * Extract data bytes from a response PDU.
     * @param responsePDU the response PDU
     * @param functionCode the expected function code
     * @return the data bytes, or null if invalid
     */
    public static byte[] extractDataFromResponsePDU(byte[] responsePDU, byte functionCode) {
        if (responsePDU == null || responsePDU.length < 2) {
            return null;
        }

        // Response format: [Function Code][Byte Count][Data...]
        int byteCount = responsePDU[1] & 0xFF;
        if (responsePDU.length < 2 + byteCount) {
            return null;
        }

        byte[] data = new byte[byteCount];
        System.arraycopy(responsePDU, 2, data, 0, byteCount);
        return data;
    }

    /**
     * Get a human-readable description for a Modbus exception code.
     * @param exceptionCode the exception code byte
     * @return the description string
     */
    public static String getModbusExceptionDescription(byte exceptionCode) {
        return switch (exceptionCode & 0xFF) {
            case 0x01 -> "Illegal Function";
            case 0x02 -> "Illegal Data Address";
            case 0x03 -> "Illegal Data Value";
            case 0x04 -> "Slave Device Failure";
            case 0x05 -> "Acknowledge (request in queue)";
            case 0x06 -> "Slave Device Busy";
            case 0x08 -> "Memory Parity Error";
            case 0x0A -> "Gateway Path Unavailable";
            case 0x0B -> "Gateway Target Device Failed to Respond";
            default -> "Unknown Exception";
        };
    }
}
