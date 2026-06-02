package org.openremote.manager.telematics;

import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.telematics.session.DeviceConnection;
import org.openremote.model.telematics.teltonika.TeltonikaAssetMapper;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.core.TelematicsMessageEnvelope;
import org.openremote.model.telematics.core.TelematicsMessageHandler;
import org.openremote.model.telematics.teltonika.TeltonikaTrackerAsset;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class TeltonikaMessageHandler implements TelematicsMessageHandler {

    private final Logger logger;
    private final AssetStorageService assetStorageService;
    private final AssetProcessingService assetProcessingService;
    private final TeltonikaAssetMapper assetMapper;

    public TeltonikaMessageHandler(Logger logger,
                                   AssetStorageService assetStorageService,
                                   AssetProcessingService assetProcessingService,
                                   TeltonikaAssetMapper assetMapper) {
        this.logger = logger;
        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
        this.assetMapper = assetMapper;
    }

    @Override
    public String getVendorId() {
        return TeltonikaVendor.getInstance().getVendorId();
    }

    @Override
    public Optional<String> process(TelematicsMessageEnvelope envelope, DeviceConnection connection) {
        TeltonikaTrackerAsset asset = getOrCreateAsset(envelope.getDeviceId(), envelope.getRealm());
        applyMessage(asset, envelope.getMessage());
        return Optional.of(asset.getId());
    }

    private TeltonikaTrackerAsset getOrCreateAsset(String imei, String realm) {
        String assetId = assetMapper.generateAssetId(imei);
        TeltonikaTrackerAsset asset = assetStorageService.find(assetId, TeltonikaTrackerAsset.class);
        if (asset != null) {
            return asset;
        }

        logger.info("Provisioning Teltonika tracker for IMEI=" + imei + " in realm=" + realm);
        TeltonikaTrackerAsset created = assetMapper.createAsset(imei, realm);
        return assetStorageService.merge(created);
    }

    private void applyMessage(TeltonikaTrackerAsset asset, DeviceMessage message) {
        List<Attribute<?>> newAttributes = assetMapper.applyAttributes(asset, message);
        assetStorageService.merge(asset);

        for (Attribute<?> attribute : message.getAttributes()) {
            boolean isNew = newAttributes.stream().anyMatch(a -> Objects.equals(a.getName(), attribute.getName()));
            if (isNew || attribute.getValue().isEmpty()) {
                continue;
            }
            assetProcessingService.sendAttributeEvent(new AttributeEvent(asset.getId(), attribute.getName(), attribute.getValue().get()));
        }
    }
}
