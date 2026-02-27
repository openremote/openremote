package org.openremote.model.telematics.asset;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.asset.Asset;
import org.openremote.model.telematics.core.DeviceMessage;

import java.util.List;

/**
 * Maps telematics device messages to OpenRemote assets.
 * <p>
 * Each vendor implementation provides a mapper that:
 * <ul>
 *   <li>Creates the appropriate asset type for a device</li>
 *   <li>Generates deterministic asset IDs from device IDs</li>
 *   <li>Applies attributes to assets (adding new or updating existing)</li>
 * </ul>
 * <p>
 * Note: The actual attribute conversion is handled by {@link org.openremote.model.value.AttributeDescriptor },
 * which outputs standard OpenRemote {@link Attribute} objects. The mapper's job is
 * primarily asset lifecycle management.
 *
 * @param <A> The asset type this mapper produces
 */
public interface TelematicsAssetMapper<A extends Asset<?>> {

    /**
     * The vendor prefix this mapper handles (e.g., "teltonika").
     */
    String getVendorPrefix();

    /**
     * The asset class this mapper produces.
     */
    Class<A> getAssetClass();

    /**
     * Create a new asset instance for a device.
     * <p>
     * Sets up the base asset with manufacturer, protocol, and device ID.
     * The asset is not yet persisted.
     *
     * @param deviceId The device identifier (IMEI)
     * @param realm    The OpenRemote realm
     * @return A new, unpersisted asset
     */
    A createAsset(String deviceId, String realm);

    /**
     * Generate a deterministic asset ID from the device ID.
     * <p>
     * This allows looking up assets by device ID without a database query.
     *
     * @param deviceId The device identifier
     * @return The asset ID
     */
    String generateAssetId(String deviceId);

    /**
     * Apply attributes from a device message to an asset.
     * <p>
     * This handles:
     * <ul>
     *   <li>Adding new attributes that don't exist on the asset</li>
     *   <li>Updating existing attributes with new values</li>
     * </ul>
     *
     * @param asset   The target asset
     * @param message The device message containing attributes
     * @return List of attributes that were new (not previously on the asset)
     */
    List<Attribute<?>> applyAttributes(A asset, DeviceMessage message);

    /**
     * Filter attributes that should be stored on the asset.
     * <p>
     * Some attributes may be transient or internal and shouldn't be persisted.
     * Default implementation keeps all attributes.
     *
     * @param attributes The attributes to filter
     * @return Filtered list of attributes to store
     */
    default List<Attribute<?>> filterStorableAttributes(List<Attribute<?>> attributes) {
        return attributes;
    }
}
