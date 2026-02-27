package org.openremote.model.telematics.teltonika;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.asset.Asset;
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
        asset.setModel("");
        asset.setProtocol("MQTT");
        asset.setCodec("Codec JSON");
        asset.addOrReplaceAttributes(
                new Attribute<>(Asset.LOCATION, null),
                new Attribute<>(Asset.NOTES, null),
                new Attribute<>(TeltonikaTrackerAsset.GPS_LOCATION, null),
                new Attribute<>(TeltonikaTrackerAsset.CODEC, "Codec JSON"),
                new Attribute<>(TeltonikaTrackerAsset.PROTOCOL, "MQTT")
        );
        return asset;
    }

    @Override
    public String generateAssetId(String deviceId) {
        return UniqueIdentifierGenerator.generateId(deviceId);
    }

    @Override
    public List<Attribute<?>> applyAttributes(TeltonikaTrackerAsset asset, DeviceMessage message) {
        List<Attribute<?>> newlyCreated = new ArrayList<>();
        for (Attribute<?> attribute : filterStorableAttributes(message.getAttributes())) {
            if (asset.getAttribute(attribute.getName()).isEmpty()) {
                newlyCreated.add(attribute);
            }
            asset.addOrReplaceAttributes(attribute);
        }
        return newlyCreated;
    }
}
