package org.openremote.model.telematics.parameter;

import io.netty.buffer.ByteBuf;

/**
 * Strategy interface for parsing parameter values from binary data.
 * <p>
 * This interface handles HOW to extract a value from raw bytes, separate from
 * the {@link org.openremote.model.value.ValueDescriptor} which describes WHAT the parameter means.
 * <p>
 * Common parsing strategies:
 * <ul>
 *   <li>Unsigned byte → Integer</li>
 *   <li>Signed short → Integer</li>
 *   <li>Unsigned int with multiplier → Double</li>
 *   <li>Long as hex string → String</li>
 *   <li>Fixed-length UTF-8 → String</li>
 * </ul>
 *
 * @param <T> The type of the parsed value
 */
@FunctionalInterface
public interface ParameterParser<T> {

    /**
     * Parse a value from the given byte buffer.
     * <p>
     * The buffer's reader index will be at the start of the parameter data.
     * The implementation should read exactly the number of bytes it expects.
     *
     * @param buffer The byte buffer positioned at the parameter data
     * @param length The number of bytes available for this parameter
     * @return The parsed value
     * @throws ParameterParseException If parsing fails
     */
    T parse(ByteBuf buffer, int length) throws ParameterParseException;

    /**
     * Returns the expected byte length for this parser, or -1 for variable length.
     * <p>
     * Fixed-length parsers should return a positive value. Variable-length parsers
     * should return -1 and handle the length parameter in {@link #parse}.
     */
    default int getExpectedLength() {
        return -1;
    }

    /**
     * Convenience method for fixed-length parsing.
     */
    default T parse(ByteBuf buffer) throws ParameterParseException {
        int length = getExpectedLength();
        if (length < 0) {
            throw new ParameterParseException("Variable-length parser requires explicit length");
        }
        return parse(buffer, length);
    }
}
