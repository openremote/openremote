package org.openremote.model.telematics.parameter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;

/**
 * Standard parser implementations for common telematics data types.
 * <p>
 * These parsers handle the most common binary formats used by GPS trackers:
 * <ul>
 *   <li>Fixed-width integers (1, 2, 4, 8 bytes, signed/unsigned)</li>
 *   <li>Floating point with multipliers</li>
 *   <li>Strings (fixed-length, null-terminated, length-prefixed)</li>
 *   <li>Hex-encoded identifiers</li>
 *   <li>Boolean flags</li>
 * </ul>
 */
public final class StandardParsers {

    private StandardParsers() {}

    // ========== Boolean Parsers ==========

    /**
     * Parses a single byte as a boolean (0 = false, non-zero = true).
     */
    public static final ParameterParser<Boolean> BOOLEAN = new ParameterParser<>() {
        @Override
        public Boolean parse(ByteBuf buffer, int length) {
            return buffer.readByte() != 0;
        }

        @Override
        public int getExpectedLength() {
            return 1;
        }
    };

    // ========== Integer Parsers ==========

    /**
     * Parses a single unsigned byte as an Integer (0-255).
     */
    public static final ParameterParser<Integer> UNSIGNED_BYTE = new ParameterParser<>() {
        @Override
        public Integer parse(ByteBuf buffer, int length) {
            return (int) buffer.readUnsignedByte();
        }

        @Override
        public int getExpectedLength() {
            return 1;
        }
    };

    /**
     * Parses a single signed byte as an Integer (-128 to 127).
     */
    public static final ParameterParser<Integer> SIGNED_BYTE = new ParameterParser<>() {
        @Override
        public Integer parse(ByteBuf buffer, int length) {
            return (int) buffer.readByte();
        }

        @Override
        public int getExpectedLength() {
            return 1;
        }
    };

    /**
     * Parses 2 bytes as an unsigned short (0-65535).
     */
    public static final ParameterParser<Integer> UNSIGNED_SHORT = new ParameterParser<>() {
        @Override
        public Integer parse(ByteBuf buffer, int length) {
            return buffer.readUnsignedShort();
        }

        @Override
        public int getExpectedLength() {
            return 2;
        }
    };

    /**
     * Parses 2 bytes as a signed short (-32768 to 32767).
     */
    public static final ParameterParser<Integer> SIGNED_SHORT = new ParameterParser<>() {
        @Override
        public Integer parse(ByteBuf buffer, int length) {
            return (int) buffer.readShort();
        }

        @Override
        public int getExpectedLength() {
            return 2;
        }
    };

    /**
     * Parses 4 bytes as a signed int.
     */
    public static final ParameterParser<Integer> SIGNED_INT = new ParameterParser<>() {
        @Override
        public Integer parse(ByteBuf buffer, int length) {
            return buffer.readInt();
        }

        @Override
        public int getExpectedLength() {
            return 4;
        }
    };

    /**
     * Parses 4 bytes as an unsigned int (returned as Long to handle full range).
     */
    public static final ParameterParser<Long> UNSIGNED_INT = new ParameterParser<>() {
        @Override
        public Long parse(ByteBuf buffer, int length) {
            return buffer.readUnsignedInt();
        }

        @Override
        public int getExpectedLength() {
            return 4;
        }
    };

    /**
     * Parses 8 bytes as a signed long.
     */
    public static final ParameterParser<Long> SIGNED_LONG = new ParameterParser<>() {
        @Override
        public Long parse(ByteBuf buffer, int length) {
            return buffer.readLong();
        }

        @Override
        public int getExpectedLength() {
            return 8;
        }
    };

    // ========== Floating Point Parsers ==========

    /**
     * Creates a parser that reads an unsigned short and applies a multiplier.
     *
     * @param multiplier The multiplier to apply (e.g., 0.001, 0.01, 0.1)
     */
    public static ParameterParser<Double> unsignedShortWithMultiplier(double multiplier) {
        return new ParameterParser<>() {
            @Override
            public Double parse(ByteBuf buffer, int length) {
                return buffer.readUnsignedShort() * multiplier;
            }

            @Override
            public int getExpectedLength() {
                return 2;
            }
        };
    }

    /**
     * Creates a parser that reads a signed short and applies a multiplier.
     */
    public static ParameterParser<Double> signedShortWithMultiplier(double multiplier) {
        return new ParameterParser<>() {
            @Override
            public Double parse(ByteBuf buffer, int length) {
                return buffer.readShort() * multiplier;
            }

            @Override
            public int getExpectedLength() {
                return 2;
            }
        };
    }

    /**
     * Creates a parser that reads an unsigned int and applies a multiplier.
     */
    public static ParameterParser<Double> unsignedIntWithMultiplier(double multiplier) {
        return new ParameterParser<>() {
            @Override
            public Double parse(ByteBuf buffer, int length) {
                return buffer.readUnsignedInt() * multiplier;
            }

            @Override
            public int getExpectedLength() {
                return 4;
            }
        };
    }

    /**
     * Parses 4 bytes as an IEEE 754 float.
     */
    public static final ParameterParser<Float> FLOAT = new ParameterParser<>() {
        @Override
        public Float parse(ByteBuf buffer, int length) {
            return buffer.readFloat();
        }

        @Override
        public int getExpectedLength() {
            return 4;
        }
    };

    /**
     * Parses 8 bytes as an IEEE 754 double.
     */
    public static final ParameterParser<Double> DOUBLE = new ParameterParser<>() {
        @Override
        public Double parse(ByteBuf buffer, int length) {
            return buffer.readDouble();
        }

        @Override
        public int getExpectedLength() {
            return 8;
        }
    };

    // ========== String Parsers ==========

    /**
     * Parses bytes as a UTF-8 string of the given length.
     */
    public static ParameterParser<String> fixedLengthString(int length) {
        return new ParameterParser<>() {
            @Override
            public String parse(ByteBuf buffer, int len) {
                int actualLength = Math.min(len, length);
                byte[] bytes = new byte[actualLength];
                buffer.readBytes(bytes);
                // Trim null bytes
                int end = actualLength;
                while (end > 0 && bytes[end - 1] == 0) {
                    end--;
                }
                return new String(bytes, 0, end, StandardCharsets.UTF_8);
            }

            @Override
            public int getExpectedLength() {
                return length;
            }
        };
    }

    /**
     * Parses variable-length bytes as a UTF-8 string.
     */
    public static final ParameterParser<String> VARIABLE_STRING = (buffer, length) -> {
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    };

    /**
     * Parses 8 bytes as a hex string (e.g., for ICCID, iButton IDs).
     */
    public static final ParameterParser<String> HEX_LONG = new ParameterParser<>() {
        @Override
        public String parse(ByteBuf buffer, int length) {
            return String.format("%016X", buffer.readLong());
        }

        @Override
        public int getExpectedLength() {
            return 8;
        }
    };

    /**
     * Parses bytes as a hex string of variable length.
     */
    public static final ParameterParser<String> HEX_BYTES = (buffer, length) -> {
        return ByteBufUtil.hexDump(buffer, buffer.readerIndex(), length).toUpperCase();
    };

    // ========== Dynamic/Fallback Parsers ==========

    /**
     * Creates a fallback parser that infers the best representation based on byte length.
     * Used for unknown parameters.
     *
     * @param length The byte length
     * @return A parser that returns an appropriate type
     */
    public static ParameterParser<?> inferFromLength(int length) {
        return switch (length) {
            case 1 -> UNSIGNED_BYTE;
            case 2 -> UNSIGNED_SHORT;
            case 4 -> UNSIGNED_INT;
            case 8 -> SIGNED_LONG;
            default -> HEX_BYTES;
        };
    }

    /**
     * Infers the Java type for a given byte length.
     */
    public static Class<?> inferTypeFromLength(int length) {
        return switch (length) {
            case 1, 2 -> Integer.class;
            case 4, 8 -> Long.class;
            default -> String.class;
        };
    }
}
