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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.openremote.model.syslog.SyslogCategory;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Decodes Modbus RTU frames from bytes.
 * Frame format: [Unit ID (1 byte)][PDU (variable)][CRC16 (2 bytes)]
 */
public class ModbusRTUDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusRTUDecoder.class);
    private static final int MAX_FRAME_SIZE = 256; // Modbus RTU max frame size

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Need at least 4 bytes: Unit ID (1) + Function Code (1) + CRC (2)
        if (in.readableBytes() < 4) {
            return;
        }

        // Mark reader index in case we need to reset
        in.markReaderIndex();

        // Read unit ID and function code without consuming
        int unitId = in.getUnsignedByte(in.readerIndex());
        int functionCode = in.getUnsignedByte(in.readerIndex() + 1);

        // Determine expected frame length based on function code
        int expectedLength = determineExpectedLength(in, functionCode);

        if (expectedLength < 0) {
            // Can't determine length yet, wait for more data
            return;
        }

        if (in.readableBytes() < expectedLength) {
            // Wait for complete frame
            in.resetReaderIndex();
            return;
        }

        // Read complete frame
        byte[] frameBytes = new byte[expectedLength];
        in.readBytes(frameBytes);

        // Validate CRC
        if (!ModbusSerialFrame.isValidCRC(frameBytes)) {
            LOG.warning("Invalid CRC in Modbus RTU frame, discarding: UnitId=" + unitId +
                       ", FunctionCode=0x" + Integer.toHexString(functionCode));
            return;
        }

        // Create frame and add to output
        ModbusSerialFrame frame = ModbusSerialFrame.fromBytes(frameBytes);
        if (frame != null) {
            out.add(frame);
            LOG.finest("Decoded Modbus RTU frame: " + frame);
        }
    }

    /**
     * Determine the expected frame length based on function code and available data.
     * Returns -1 if we need more data to determine the length.
     */
    private int determineExpectedLength(ByteBuf in, int functionCode) {
        // For exception responses (function code with 0x80 bit set)
        if ((functionCode & 0x80) != 0) {
            // Exception response: Unit ID + Function Code + Exception Code + CRC = 5 bytes
            return 5;
        }

        // For normal responses, we need to read the byte count field
        switch (functionCode) {
            case 0x01: // Read Coils
            case 0x02: // Read Discrete Inputs
            case 0x03: // Read Holding Registers
            case 0x04: // Read Input Registers
                // Response format: Unit ID + FC + Byte Count + Data + CRC
                if (in.readableBytes() >= 3) {
                    int byteCount = in.getUnsignedByte(in.readerIndex() + 2);
                    return 1 + 1 + 1 + byteCount + 2; // Unit ID + FC + Byte Count + Data + CRC
                }
                return -1; // Need more data

            case 0x05: // Write Single Coil
            case 0x06: // Write Single Register
                // Response format: Unit ID + FC + Address (2) + Value (2) + CRC = 8 bytes
                return 8;

            case 0x0F: // Write Multiple Coils
            case 0x10: // Write Multiple Registers
                // Response format: Unit ID + FC + Address (2) + Quantity (2) + CRC = 8 bytes
                return 8;

            case 0x16: // Mask Write Register
                // Response format: Unit ID + FC + Address (2) + AND Mask (2) + OR Mask (2) + CRC = 10 bytes
                return 10;

            case 0x17: // Read/Write Multiple Registers
                // Response format: Unit ID + FC + Byte Count + Data + CRC
                if (in.readableBytes() >= 3) {
                    int byteCount = in.getUnsignedByte(in.readerIndex() + 2);
                    return 1 + 1 + 1 + byteCount + 2;
                }
                return -1;

            default:
                LOG.warning("Unknown Modbus function code: 0x" + Integer.toHexString(functionCode));
                // Try to read a reasonable minimum frame
                return -1;
        }
    }
}
