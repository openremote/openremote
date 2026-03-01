package org.openremote.model.telematics.parameter;

import org.openremote.model.asset.Asset;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Registry interface for telematics device parameters.
 * Implementations provide device-specific parameter descriptors that can be used
 * for parsing binary protocols, mapping to asset attributes, and validating values.
 * <p>
 * Each vendor implementation (e.g., Teltonika, Queclink) should implement this interface
 * to provide their specific parameter definitions.
 *
 * @param <D> The type of ParsingValueDescriptor used by this registry
 */
public interface ParameterRegistry<D extends ParseableValueDescriptor<?>> {

    /**
     * Get the vendor prefix used for attribute naming (e.g., "teltonika", "queclink").
     *
     * @return The vendor prefix string
     */
    String getVendorPrefix();

    /**
     * Get a descriptor by its protocol-specific ID (e.g., AVL ID for Teltonika).
     *
     * @param id The protocol-specific parameter ID
     * @return The descriptor if found, or empty if not registered
     */
    Optional<D> getById(String id);

    /**
     * Get a descriptor by its full name (including vendor prefix).
     *
     * @param name The full attribute name (e.g., "teltonika_239")
     * @return The descriptor if found, or empty if not registered
     */
    Optional<D> getByName(String name);

    /**
     * Stream all registered descriptors.
     *
     * @return A stream of all registered descriptors
     */
    Stream<D> all();

    /**
     * Get the total number of registered descriptors.
     *
     * @return The count of registered descriptors
     */
    int size();

    /**
     * Find a matching AttributeDescriptor in the given Asset class for the specified ValueDescriptor.
     *
     * @param assetClass The Asset class to search for matching AttributeDescriptor fields
     * @param valueDescriptor The ValueDescriptor to match against
     * @param <T> The type of the value
     * @return The matching AttributeDescriptor if found
     */
    <T> Optional<AttributeDescriptor<T>> findMatchingAttributeDescriptor(
            Class<? extends Asset<?>> assetClass,
            ValueDescriptor<T> valueDescriptor);
}
