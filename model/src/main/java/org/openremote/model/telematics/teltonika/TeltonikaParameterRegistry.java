package org.openremote.model.telematics.teltonika;

import org.openremote.model.asset.Asset;
import org.openremote.model.telematics.ParameterRegistry;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * Parameter registry implementation for Teltonika devices.
 * Provides access to all registered Teltonika AVL parameter descriptors.
 * <p>
 * This is a singleton that wraps the static descriptors defined in
 * {@link TeltonikaValueDescriptors}.
 */
public class TeltonikaParameterRegistry implements ParameterRegistry<TeltonikaValueDescriptor<?>> {

    private static final TeltonikaParameterRegistry INSTANCE = new TeltonikaParameterRegistry();

    private final Map<String, TeltonikaValueDescriptor<?>> descriptorsByName;
    private final Map<String, TeltonikaValueDescriptor<?>> descriptorsById;

    private TeltonikaParameterRegistry() {
        Map<String, TeltonikaValueDescriptor<?>> byName = new HashMap<>();
        Map<String, TeltonikaValueDescriptor<?>> byId = new HashMap<>();

        // Collect all TeltonikaValueDescriptor fields from TeltonikaValueDescriptors
        for (Field field : TeltonikaValueDescriptors.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                TeltonikaValueDescriptor.class.isAssignableFrom(field.getType())) {
                try {
                    TeltonikaValueDescriptor<?> descriptor = (TeltonikaValueDescriptor<?>) field.get(null);
                    if (descriptor != null && descriptor.getName() != null) {
                        byName.put(descriptor.getName(), descriptor);
                        // Also map by the raw ID (without prefix)
                        String rawId = descriptor.getName().replace(TeltonikaValueDescriptor.VENDOR_PREFIX + "_", "");
                        byId.put(rawId, descriptor);
                    }
                } catch (IllegalAccessException e) {
                    // Skip inaccessible fields
                }
            }
        }

        this.descriptorsByName = Collections.unmodifiableMap(byName);
        this.descriptorsById = Collections.unmodifiableMap(byId);
    }

    /**
     * Get the singleton instance.
     *
     * @return The registry instance
     */
    public static TeltonikaParameterRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public String getVendorPrefix() {
        return TeltonikaValueDescriptor.VENDOR_PREFIX;
    }

    @Override
    public Optional<TeltonikaValueDescriptor<?>> getById(String id) {
        return Optional.ofNullable(descriptorsById.get(id));
    }

    @Override
    public Optional<TeltonikaValueDescriptor<?>> getByName(String name) {
        return Optional.ofNullable(descriptorsByName.get(name));
    }

    @Override
    public Stream<TeltonikaValueDescriptor<?>> all() {
        return descriptorsByName.values().stream();
    }

    @Override
    public int size() {
        return descriptorsByName.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<AttributeDescriptor<T>> findMatchingAttributeDescriptor(
            Class<? extends Asset<?>> assetClass,
            ValueDescriptor<T> valueDescriptor) {

        return Arrays.stream(assetClass.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) &&
                        AttributeDescriptor.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    try {
                        return (AttributeDescriptor<?>) field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(attrDesc -> attrDesc.getType().getName().equals(valueDescriptor.getName()))
                .findFirst()
                .map(attrDesc -> (AttributeDescriptor<T>) attrDesc);
    }
}
