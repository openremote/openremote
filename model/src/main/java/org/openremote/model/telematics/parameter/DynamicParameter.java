package org.openremote.model.telematics.parameter;

/**
 * A dynamically created ParseableValueDescriptor for unknown parameter IDs.
 * <p>
 * When a device sends a parameter that isn't in the registry, we still want to
 * capture and store the data. DynamicParameter infers the type from byte length.
 *
 * @param <T> The inferred type of the parameter value
 */
public class DynamicParameter<T> extends ParseableValueDescriptor<T> {

    private final boolean known;

    public DynamicParameter(String name, Class<T> type, int byteLength, ParameterParser<T> parser) {
        super(name, type, byteLength, parser);
        this.known = false;
    }

    /**
     * Creates a dynamic parameter with inferred type based on byte length.
     *
     * @param vendorPrefix The vendor prefix (e.g., "teltonika")
     * @param id           The parameter ID
     * @param byteLength   The byte length (used to infer type)
     */
    @SuppressWarnings("unchecked")
    public static DynamicParameter<?> fromByteLength(String vendorPrefix, String id, int byteLength) {
        String name = vendorPrefix + "_" + id;
        Class<?> inferredType = StandardParsers.inferTypeFromLength(byteLength);
        ParameterParser<?> inferredParser = StandardParsers.inferFromLength(byteLength);
        return new DynamicParameter<>(name, (Class<Object>) inferredType, byteLength, (ParameterParser<Object>) inferredParser);
    }

    /**
     * Returns false - dynamic parameters are not known/registered.
     */
    public boolean isKnown() {
        return known;
    }

    @Override
    public String toString() {
        return "DynamicParameter{" +
                "name='" + getName() + '\'' +
                ", type=" + getType().getSimpleName() +
                ", byteLength=" + getByteLength() +
                '}';
    }
}
