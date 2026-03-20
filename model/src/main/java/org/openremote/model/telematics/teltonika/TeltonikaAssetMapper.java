package org.openremote.model.telematics.teltonika;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.asset.Asset;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.telematics.asset.TelematicsAssetMapper;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.util.UniqueIdentifierGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Asset mapper for Teltonika tracker devices.
 */
public class TeltonikaAssetMapper implements TelematicsAssetMapper<TeltonikaTrackerAsset> {

    @Override
    public String getVendorPrefix() {
        return "teltonika";
    }

    @Override
    public Class<TeltonikaTrackerAsset> getAssetClass() {
        return TeltonikaTrackerAsset.class;
    }

    @Override
    public TeltonikaTrackerAsset createAsset(String imei, String realm) {
        TeltonikaTrackerAsset asset = new TeltonikaTrackerAsset();
        asset.setRealm(realm);
        asset.setId(generateAssetId(imei));
        asset.setName("Teltonika Device " + imei);
        asset.setImei(imei);
        asset.setManufacturer("Teltonika");
        asset.addOrReplaceAttributes(
                new Attribute<>(TeltonikaTrackerAsset.PROTOCOL),
                new Attribute<>(TeltonikaTrackerAsset.MODEL),
                new Attribute<>(TeltonikaTrackerAsset.NOTES),
                new Attribute<>(Asset.LOCATION)
        );
        return asset;
    }

    @Override
    public String generateAssetId(String deviceId) {
        return UniqueIdentifierGenerator.generateId(deviceId);
    }

    @Override
    public List<Attribute<?>> applyAttributes(TeltonikaTrackerAsset asset, DeviceMessage message) {
        List<Attribute<?>> preProcessedAttributes = preProcessAttributes(asset, message);
        List<Attribute<?>> newlyCreated = new ArrayList<>();
        for (Attribute<?> attribute : filterStorableAttributes(preProcessedAttributes)) {
            if (asset.getAttribute(attribute.getName()).isEmpty()) {
                newlyCreated.add(attribute);
            }
            asset.addOrReplaceAttributes(attribute);
        }
        return newlyCreated;
    }

    /*
    Just like all other Telematics devices, Teltonika devices produce some commonly known parameters, that should just
    use their OpenRemote-specific equivalent attribute and value descriptors. Think location, timestamp, etc. This way, the asset is more
    compliant with the underlying OpenRemote system.

    Those attributes are added in their correct form here, and they're removed from the main message after they're added.
     */
    public List<Attribute<?>> preProcessAttributes(TeltonikaTrackerAsset asset, DeviceMessage message) {
        List<Attribute<?>> processedAttributes = new ArrayList<>();

        for (Attribute<?> attribute : message.getAttributes()) {
            if (TeltonikaTrackerAsset.GPS_LOCATION.getName().equals(attribute.getName())) {
                attribute = new Attribute<>(Asset.LOCATION, (GeoJSONPoint) attribute.getValue().get(), attribute.getTimestamp().get());
            }
            if(TeltonikaParameters.TIMESTAMP.getName().equals(attribute.getName())) {
                attribute = new Attribute<>(TeltonikaTrackerAsset.TIMESTAMP, (Long) attribute.getValue().get(),  attribute.getTimestamp().get());
            }



            processedAttributes.add(attribute);
        }

        return processedAttributes;
    }

}
