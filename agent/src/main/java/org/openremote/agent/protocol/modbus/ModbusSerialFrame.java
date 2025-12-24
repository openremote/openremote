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

/**
 * Represents a Modbus RTU frame (Unit ID + PDU + CRC16)
 */
public class ModbusSerialFrame implements AbstractModbusProtocol.ModbusResponse {
    private final int unitId;
    private final byte[] pdu;
    private byte[] fullFrame; // Cached full frame with CRC

    public ModbusSerialFrame(int unitId, byte[] pdu) {
        this.unitId = unitId;
        this.pdu = pdu;
    }

    public static ModbusSerialFrame fromBytes(byte[] frameBytes) {
        if (frameBytes == null || frameBytes.length < 4) {
            // Minimum: 1 byte unit ID + 1 byte function code + 2 bytes CRC
            return null;
        }

        int unitId = frameBytes[0] & 0xFF;
        int pduLength = frameBytes.length - 3; // Exclude unit ID and 2-byte CRC
        byte[] pdu = new byte[pduLength];
        System.arraycopy(frameBytes, 1, pdu, 0, pduLength);

        return new ModbusSerialFrame(unitId, pdu);
    }

    public int getUnitId() {
        return unitId;
    }

    public byte[] getPdu() {
        return pdu;
    }

    public byte getFunctionCode() {
        return pdu != null && pdu.length > 0 ? pdu[0] : 0;
    }

    public boolean isException() {
        return pdu != null && pdu.length > 0 && (pdu[0] & 0x80) != 0;
    }

    public byte[] pack() {
        if (fullFrame != null) {
            return fullFrame;
        }

        fullFrame = new byte[1 + pdu.length + 2];
        fullFrame[0] = (byte) unitId;
        System.arraycopy(pdu, 0, fullFrame, 1, pdu.length);

        int crc = calculateCRC16(fullFrame, 0, 1 + pdu.length);
        fullFrame[1 + pdu.length] = (byte) (crc & 0xFF);        // CRC Low
        fullFrame[2 + pdu.length] = (byte) ((crc >> 8) & 0xFF); // CRC High

        return fullFrame;
    }

    public static boolean isValidCRC(byte[] frameBytes) {
        if (frameBytes == null || frameBytes.length < 4) {
            return false;
        }

        int receivedCRC = (frameBytes[frameBytes.length - 2] & 0xFF) |
                         ((frameBytes[frameBytes.length - 1] & 0xFF) << 8);
        int calculatedCRC = calculateCRC16(frameBytes, 0, frameBytes.length - 2);

        return receivedCRC == calculatedCRC;
    }

    private static int calculateCRC16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = crc >> 1;
                }
            }
        }
        return crc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ModbusSerialFrame{unitId=").append(unitId);
        sb.append(", functionCode=0x").append(String.format("%02X", getFunctionCode()));
        sb.append(", pduLength=").append(pdu.length);
        sb.append(", exception=").append(isException());
        sb.append("}");
        return sb.toString();
    }
}
