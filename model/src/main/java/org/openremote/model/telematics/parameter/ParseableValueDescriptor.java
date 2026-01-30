package org.openremote.model.telematics.parameter;

import io.netty.buffer.ByteBuf;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueDescriptor;

/**
 * A ValueDescriptor with binary parsing capability.
 * <p>
 * Extends OpenRemote's {@link ValueDescriptor} to add:
 * <ul>
 *   <li>Binary parsing via {@link ParameterParser}</li>
 *   <li>Expected byte length for protocol parsing</li>
 *   <li>Direct conversion to {@link Attribute}</li>
 * </ul>
 * <p>
 * This integrates telematics parsing with OpenRemote's value type system.
 * The output is always a standard {@link Attribute}.
 *
 * @param <T> The type of the parsed value
 */
public class ParseableValueDescriptor<T> extends ValueDescriptor<T> {

    private final ParameterParser<T> parser;
    private final int byteLength;

    /**
     * Creates a ParseableValueDescriptor with a fixed byte length.
     *
     * @param name        The unique name (e.g., "teltonika_239")
     * @param type        The Java type
     * @param byteLength  Expected byte length (-1 for variable)
     * @param parser      The parser function
     * @param constraints Optional value constraints
     */
    public ParseableValueDescriptor(String name, Class<T> type, int byteLength, ParameterParser<T> parser, ValueConstraint... constraints) {
        super(name, type, constraints);
        this.parser = parser;
        this.byteLength = byteLength;
    }

    /**
     * Creates a ParseableValueDescriptor with variable length.
     */
    public ParseableValueDescriptor(String name, Class<T> type, ParameterParser<T> parser, ValueConstraint... constraints) {
        this(name, type, -1, parser, constraints);
    }

    /**
     * Get the parser for this descriptor.
     */
    public ParameterParser<T> getParser() {
        return parser;
    }

    /**
     * Get the expected byte length, or -1 for variable length.
     */
    public int getByteLength() {
        return byteLength;
    }

    /**
     * Whether this descriptor has a fixed byte length.
     */
    public boolean hasFixedLength() {
        return byteLength > 0;
    }

    /**
     * Parse a value from a byte buffer.
     *
     * @param buffer The buffer positioned at the data
     * @param length The number of bytes to read
     * @return The parsed value
     * @throws ParameterParseException If parsing fails
     */
    public T parse(ByteBuf buffer, int length) throws ParameterParseException {
        try {
            return parser.parse(buffer, length);
        } catch (ParameterParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParameterParseException(getName(), e.getMessage(), e);
        }
    }

    /**
     * Parse using the expected byte length.
     */
    public T parse(ByteBuf buffer) throws ParameterParseException {
        if (byteLength < 0) {
            throw new ParameterParseException(getName(), "Variable-length descriptor requires explicit length");
        }
        return parse(buffer, byteLength);
    }

    /**
     * Parse and create an Attribute with the given timestamp.
     *
     * @param buffer    The buffer positioned at the data
     * @param length    The number of bytes to read
     * @param timestamp The device timestamp
     * @return An Attribute containing the parsed value
     */
    public Attribute<T> parseToAttribute(ByteBuf buffer, int length, long timestamp) throws ParameterParseException {
        T value = parse(buffer, length);
        return new Attribute<>(getName(), this, value, timestamp);
    }

    /**
     * Create an Attribute from an already-parsed value.
     *
     * @param value     The value
     * @param timestamp The device timestamp
     * @return An Attribute containing the value
     */
    public Attribute<T> toAttribute(T value, long timestamp) {
        return new Attribute<>(getName(), this, value, timestamp);
    }

    /**
     * Create an AttributeDescriptor from this value descriptor.
     */
    public AttributeDescriptor<T> toAttributeDescriptor() {
        return new AttributeDescriptor<>(getName(), this);
    }

    @Override
    public String toString() {
        return "ParseableValueDescriptor{" +
                "name='" + getName() + '\'' +
                ", type=" + getType().getSimpleName() +
                ", byteLength=" + byteLength +
                '}';
    }
}
