package org.openremote.model.telematics;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class TrackerAsset extends Asset<TrackerAsset> {
    public static final AttributeDescriptor<String> IMEI = new AttributeDescriptor<>("imei", ValueType.TEXT);
    public static final AttributeDescriptor<String> MANUFACTURER = Asset.MANUFACTURER.withOptional(false);
    public static final AttributeDescriptor<String> MODEL = Asset.MODEL.withOptional(false);
    public static final AttributeDescriptor<String> PROTOCOL = new AttributeDescriptor<>("protocol", ValueType.TEXT);
    public static final AttributeDescriptor<String> CODEC = new AttributeDescriptor<>("codec", ValueType.TEXT).withOptional(false);
//    public static final AttributeDescriptor<ConnectionStatus> NAME = new AttributeDescriptor<>("connected", ValueType.CONNECTION_STATUS).withMeta(MetaItemType.USER_CONNECTED, true);

    public static final AssetDescriptor<TrackerAsset> DESCRIPTOR = new AssetDescriptor<>("crosshairs-gps", "005aff", TrackerAsset.class);

    public Optional<String> getProtocol() {
        return getAttributes().getValue(PROTOCOL);
    }

    public TrackerAsset setProtocol(String protocol) {
        getAttributes().getOrCreate(PROTOCOL).setValue(protocol);
        return this;
    }
    public Optional<String> getCodec() {
        return getAttributes().getValue(CODEC);
    }

    public TrackerAsset setCodec(String codec) {
        getAttributes().getOrCreate(CODEC).setValue(codec);
        return this;
    }

    public Optional<String> getImei() {
        return getAttributes().getValue(IMEI);
    }

    public TrackerAsset setImei(String imei) {
        getAttributes().getOrCreate(IMEI).setValue(imei);
        return this;
    }
}
