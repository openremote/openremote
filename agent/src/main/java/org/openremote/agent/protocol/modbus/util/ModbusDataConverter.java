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

import org.openremote.agent.protocol.modbus.ModbusAgent;
import org.openremote.agent.protocol.modbus.ModbusAgentLink;
import org.openremote.model.syslog.SyslogCategory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Utility class for converting between Modbus register data and Java values.
 */
public final class ModbusDataConverter {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusDataConverter.class);

    private ModbusDataConverter() {
        // Utility class
    }

    /**
     * Extract a value from a batch response at the specified offset.
     * @param data the response data bytes
     * @param offset the offset in registers/coils
     * @param registerCount the number of registers for this value
     * @param dataType the Modbus data type
     * @param endianFormat the endian format to use
     * @param functionCode the function code (determines coil vs register)
     * @return the extracted value, or null if extraction fails
     */
    public static Object extractValueFromBatchResponse(byte[] data, int offset, int registerCount,
                                                       ModbusAgentLink.ModbusDataType dataType,
                                                       ModbusAgent.EndianFormat endianFormat,
                                                       byte functionCode) {
        try {
            // For coils and discrete inputs, data is bit-packed
            if (functionCode == 0x01 || functionCode == 0x02) {
                int byteIndex = offset / 8;
                int bitIndex = offset % 8;
                if (byteIndex < data.length) {
                    return ((data[byteIndex] >> bitIndex) & 0x01) == 1;
                }
                return null;
            }

            // For registers (holding/input), data is word-based (2 bytes per register)
            int byteOffset = offset * 2;
            if (byteOffset + (registerCount * 2) > data.length) {
                LOG.warning("Not enough data in response: need " + (byteOffset + registerCount * 2) + " bytes, have " + data.length);
                return null;
            }

            // Extract register bytes
            byte[] registerBytes = new byte[registerCount * 2];
            System.arraycopy(data, byteOffset, registerBytes, 0, registerBytes.length);

            // Parse multi-register value
            return parseMultiRegisterValue(registerBytes, registerCount, dataType, endianFormat);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to extract value from batch response at offset " + offset + ": " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert a Java value to register bytes for writing.
     * @param value the value to convert
     * @param registerCount the number of registers
     * @param dataType the Modbus data type
     * @param endianFormat the endian format to use
     * @return the register bytes
     */
    public static byte[] convertValueToRegisterBytes(Object value, int registerCount,
                                                     ModbusAgentLink.ModbusDataType dataType,
                                                     ModbusAgent.EndianFormat endianFormat) {
        byte[] bytes = new byte[registerCount * 2];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Apply byte order
        buffer.order(endianFormat == ModbusAgent.EndianFormat.BIG_ENDIAN ||
                endianFormat == ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP
                ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // Convert based on register count and data type
        if (registerCount == 1) {
            // Single register (16-bit)
            if (value instanceof Number) {
                int intValue = ((Number) value).intValue();
                buffer.putShort((short) (intValue & 0xFFFF));
            } else {
                throw new IllegalArgumentException("Cannot convert value to register: " + value);
            }
        } else if (registerCount == 2) {
            // Two registers (32-bit int or float)
            if (dataType == ModbusAgentLink.ModbusDataType.REAL) {
                buffer.putFloat(((Number) value).floatValue());
            } else if (value instanceof Number) {
                buffer.putInt(((Number) value).intValue());
            } else {
                throw new IllegalArgumentException("Cannot convert value to 2 registers: " + value);
            }
        } else if (registerCount == 4) {
            // Four registers (64-bit int or double)
            if (dataType == ModbusAgentLink.ModbusDataType.LREAL) {
                buffer.putDouble(((Number) value).doubleValue());
            } else if (value instanceof Number) {
                buffer.putLong(((Number) value).longValue());
            } else {
                throw new IllegalArgumentException("Cannot convert value to 4 registers: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported register count for write: " + registerCount);
        }

        // Apply word order (byte swap for multi-register values)
        if (registerCount > 1 && (endianFormat == ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP ||
                endianFormat == ModbusAgent.EndianFormat.LITTLE_ENDIAN_BYTE_SWAP)) {
            bytes = applyWordOrder(bytes, registerCount, endianFormat);
        }

        return bytes;
    }

    /**
     * Apply word order transformation for byte-swap endian formats.
     * @param data the data bytes
     * @param registerCount the number of registers
     * @param format the endian format
     * @return the transformed bytes
     */
    public static byte[] applyWordOrder(byte[] data, int registerCount, ModbusAgent.EndianFormat format) {
        // No swap for single register or non-byte-swap formats
        if (registerCount <= 1 || format == ModbusAgent.EndianFormat.BIG_ENDIAN || format == ModbusAgent.EndianFormat.LITTLE_ENDIAN) {
            return data;
        }
        // BYTE_SWAP - swap word order
        byte[] result = new byte[data.length];
        for (int i = 0; i < registerCount; i++) {
            int srcIdx = i * 2;
            int dstIdx = (registerCount - 1 - i) * 2;
            result[dstIdx] = data[srcIdx];
            result[dstIdx + 1] = data[srcIdx + 1];
        }
        return result;
    }

    /**
     * Parse a multi-register value from bytes.
     * @param dataBytes the register bytes
     * @param registerCount the number of registers
     * @param dataType the Modbus data type
     * @param endianFormat the endian format
     * @return the parsed value
     */
    public static Object parseMultiRegisterValue(byte[] dataBytes, int registerCount,
                                                 ModbusAgentLink.ModbusDataType dataType,
                                                 ModbusAgent.EndianFormat endianFormat) {
        if (registerCount == 1) {
            // Single 16-bit register - interpret based on dataType
            int high = (dataBytes[0] & 0xFF) << 8;
            int low = dataBytes[1] & 0xFF;
            int unsignedValue = high | low;

            return switch (dataType) {
                case BOOL -> unsignedValue != 0;
                case SINT -> {
                    // Signed 8-bit: use low byte only, sign-extend
                    yield (int) (byte) low;
                }
                case USINT, BYTE -> {
                    // Unsigned 8-bit: use low byte only
                    yield low;
                }
                case INT -> {
                    // Signed 16-bit: sign-extend to int
                    yield (int) (short) unsignedValue;
                }
                case UINT, WORD -> {
                    // Unsigned 16-bit
                    yield unsignedValue;
                }
                case CHAR -> (char) (unsignedValue & 0xFF);
                case WCHAR -> String.valueOf((char) unsignedValue);
                default -> unsignedValue; // Fallback to unsigned
            };
        } else if (registerCount == 2) {
            // Two registers - could be IEEE754 float or 32-bit integer
            byte[] processedBytes = applyWordOrder(dataBytes, 2, endianFormat);
            ByteBuffer buffer = ByteBuffer.wrap(processedBytes);
            buffer.order(getJavaByteOrder(endianFormat));

            return switch (dataType) {
                case REAL -> {
                    float value = buffer.getFloat();
                    // Filter out NaN and Infinity values
                    if (Float.isNaN(value) || Float.isInfinite(value)) {
                        LOG.warning("Invalid float value (NaN or Infinity), ignoring");
                        yield null;
                    }
                    yield value;
                }
                case DINT -> {
                    // Signed 32-bit integer
                    yield buffer.getInt();
                }
                case UDINT, DWORD -> {
                    // Unsigned 32-bit integer - return as long to preserve full range
                    yield buffer.getInt() & 0xFFFFFFFFL;
                }
                default -> buffer.getInt(); // Fallback to signed 32-bit
            };
        } else if (registerCount == 4) {
            // Four registers - could be 64-bit integer or double precision float
            byte[] processedBytes = applyWordOrder(dataBytes, 4, endianFormat);
            ByteBuffer buffer = ByteBuffer.wrap(processedBytes);
            buffer.order(getJavaByteOrder(endianFormat));

            return switch (dataType) {
                case LREAL -> {
                    double value = buffer.getDouble();
                    if (Double.isNaN(value) || Double.isInfinite(value)) {
                        LOG.warning("Invalid double value (NaN or Infinity), ignoring");
                        yield null;
                    }
                    yield value;
                }
                case LINT -> {
                    // Signed 64-bit integer
                    yield buffer.getLong();
                }
                case ULINT, LWORD -> {
                    // Unsigned 64-bit integer - return as BigInteger to preserve full range
                    long signedValue = buffer.getLong();
                    if (signedValue >= 0) {
                        yield BigInteger.valueOf(signedValue);
                    } else {
                        yield BigInteger.valueOf(signedValue).add(BigInteger.ONE.shiftLeft(64));
                    }
                }
                default -> buffer.getLong(); // Fallback to signed 64-bit
            };
        }

        return null;
    }

    /**
     * Get the Java ByteOrder for a Modbus endian format.
     * @param format the endian format
     * @return the corresponding ByteOrder
     */
    public static ByteOrder getJavaByteOrder(ModbusAgent.EndianFormat format) {
        return (format == ModbusAgent.EndianFormat.BIG_ENDIAN || format == ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP)
                ? ByteOrder.BIG_ENDIAN
                : ByteOrder.LITTLE_ENDIAN;
    }
}
