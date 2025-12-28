package org.openremote.model.telematics;

import io.netty.buffer.ByteBuf;
import jakarta.persistence.Transient;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.TsIgnoreTypeParams;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueDescriptor;

import java.util.function.Function;

@TsIgnore
@TsIgnoreTypeParams
public class ParsingValueDescriptor<T> extends ValueDescriptor<T> {

    @Transient
    private final transient Function<ByteBuf, T> parser;

    @Transient
    private final transient int length; // byte length from AVL specification (-1 for variable length)

    @Transient
    private final transient String vendorPrefix;

    public ParsingValueDescriptor(String vendorPrefix, String name, Class<T> type, int length, Function<ByteBuf, T> parser, ValueConstraint... constraints) {
        super(vendorPrefix != null ? vendorPrefix + "_" + name : name, type, constraints);
        if(vendorPrefix == null) throw new RuntimeException("vendor prefix cannot be null");
        this.vendorPrefix = vendorPrefix;
        this.parser = parser;
        this.length = length;
    }

    public T parse(ByteBuf value) {
        return parser.apply(value);
    }

    public int getLength() {
        return length;
    }

    public boolean hasFixedLength() {
        return length > 0;
    }

    public String getVendorPrefix() {
        return vendorPrefix;
    }

    /**
     * Creates a ParsingValueDescriptor from an existing ValueDescriptor (like ValueType.TEXT) with a custom parser.
     * Useful for extending standard ValueDescriptors with binary parsing logic while preserving their type and constraints.
     *
     * Example usage:
     * <pre>
     * // Extend ValueType.TEXT with binary parsing
     * ParsingValueDescriptor&lt;String&gt; hexString = ParsingValueDescriptor.fromValueDescriptor(
     *     "teltonika", "11", ValueType.TEXT, 8,
     *     buf -> String.format("%016X", buf.getLong())
     * );
     * </pre>
     *
     * @param vendorPrefix The vendor prefix for the attribute name (e.g., "teltonika")
     * @param name The name for the attribute
     * @param valueDescriptor The ValueDescriptor to extend (e.g., ValueType.TEXT, ValueType.NUMBER)
     * @param length The byte length from specification (-1 for variable length)
     * @param parser The function to parse binary data from ByteBuffer
     * @param <T> The type of the parsed value
     * @return A new ParsingValueDescriptor with the specified parser
     */
    public static <T> ParsingValueDescriptor<T> fromValueDescriptor(String vendorPrefix, String name, ValueDescriptor<T> valueDescriptor, int length, Function<ByteBuf, T> parser) {
        return new ParsingValueDescriptor<>(vendorPrefix, name, valueDescriptor.getType(), length, parser, valueDescriptor.getConstraints());
    }

    /**
     * Creates a ParsingValueDescriptor from an existing ValueDescriptor (like ValueType.TEXT) with a custom parser (variable length).
     * Useful for extending standard ValueDescriptors with binary parsing logic while preserving their type and constraints.
     *
     * @param vendorPrefix The vendor prefix for the attribute name (e.g., "teltonika")
     * @param name The name for the attribute
     * @param valueDescriptor The ValueDescriptor to extend (e.g., ValueType.TEXT, ValueType.NUMBER)
     * @param parser The function to parse binary data from ByteBuffer
     * @param <T> The type of the parsed value
     * @return A new ParsingValueDescriptor with the specified parser
     */
    public static <T> ParsingValueDescriptor<T> fromValueDescriptor(String vendorPrefix, String name, ValueDescriptor<T> valueDescriptor, Function<ByteBuf, T> parser) {
        return fromValueDescriptor(vendorPrefix, name, valueDescriptor, -1, parser);
    }
}
