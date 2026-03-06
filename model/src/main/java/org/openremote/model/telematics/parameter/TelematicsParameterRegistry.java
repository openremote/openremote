package org.openremote.model.telematics.parameter;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Registry for telematics parameter descriptors.
 * <p>
 * Each vendor implementation provides its own registry with known parameter
 * definitions. The registry also supports creating dynamic parameters for
 * unknown IDs - ensuring no data is lost.
 *
 * @param <P> The type of ParseableValueDescriptor this registry manages
 */
public interface TelematicsParameterRegistry<P extends ParseableValueDescriptor<?>> {

    /**
     * Get the vendor prefix for this registry (e.g., "teltonika").
     */
    String getVendorPrefix();

    /**
     * Get a known parameter by its protocol-specific ID.
     *
     * @param id The parameter ID (e.g., "239" for Teltonika ignition)
     */
    Optional<P> getById(String id);

    /**
     * Get a parameter by its full name (including vendor prefix).
     *
     * @param fullName The full attribute name (e.g., "teltonika_239")
     */
    Optional<P> getByFullName(String fullName);

    /**
     * Get a known parameter or create a dynamic one for unknown IDs.
     * <p>
     * This ensures we never lose data even for unknown parameters.
     *
     * @param id         The parameter ID
     * @param byteLength The byte length of the data (used for dynamic creation)
     * @return A descriptor that can parse the data
     */
    ParseableValueDescriptor<?> getOrCreateDynamic(String id, int byteLength);

    /**
     * Stream all known parameters in this registry.
     */
    Stream<P> all();

    /**
     * Get the count of known parameters.
     */
    int size();

    /**
     * Check if a parameter ID is known (registered).
     */
    default boolean isKnown(String id) {
        return getById(id).isPresent();
    }
}
