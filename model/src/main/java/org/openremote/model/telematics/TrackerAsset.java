package org.openremote.model.telematics;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

/**
 * Base asset type for GPS/telematics trackers.
 * Contains common attributes shared across all tracker vendors.
 * <p>
 * Vendor-specific implementations (e.g., TeltonikaTrackerAsset) extend this class
 * to add device-specific attributes.
 */
@Entity
public class TrackerAsset extends Asset<TrackerAsset> {

    // Device identification
    public static final AttributeDescriptor<String> IMEI = new AttributeDescriptor<>("imei", ValueType.TEXT);
    public static final AttributeDescriptor<String> MANUFACTURER = Asset.MANUFACTURER.withOptional(false);
    public static final AttributeDescriptor<String> MODEL = Asset.MODEL.withOptional(false);
    public static final AttributeDescriptor<String> PROTOCOL = new AttributeDescriptor<>("protocol", ValueType.TEXT);
    public static final AttributeDescriptor<String> CODEC = new AttributeDescriptor<>("codec", ValueType.TEXT).withOptional(true);

    // Common GPS/location attributes
    public static final AttributeDescriptor<GeoJSONPoint> GPS_LOCATION = new AttributeDescriptor<>("gpsLocation", ValueType.GEO_JSON_POINT).withOptional(true);
    public static final AttributeDescriptor<Integer> SPEED = new AttributeDescriptor<>("speed", ValueType.POSITIVE_INTEGER).withOptional(true);
    public static final AttributeDescriptor<Integer> DIRECTION = new AttributeDescriptor<>("direction", ValueType.DIRECTION).withOptional(true);
    public static final AttributeDescriptor<Integer> ALTITUDE = new AttributeDescriptor<>("altitude", ValueType.INTEGER).withOptional(true);
    public static final AttributeDescriptor<Integer> SATELLITES = new AttributeDescriptor<>("satellites", ValueType.POSITIVE_INTEGER).withOptional(true);
    public static final AttributeDescriptor<Long> TIMESTAMP = new AttributeDescriptor<>("timestamp", ValueType.TIMESTAMP).withOptional(true);

    // Common vehicle/device status attributes
    public static final AttributeDescriptor<Boolean> IGNITION = new AttributeDescriptor<>("ignition", ValueType.BOOLEAN).withOptional(true);
    public static final AttributeDescriptor<Boolean> MOVEMENT = new AttributeDescriptor<>("movement", ValueType.BOOLEAN).withOptional(true);
    public static final AttributeDescriptor<Integer> BATTERY_LEVEL = new AttributeDescriptor<>("batteryLevel", ValueType.POSITIVE_INTEGER).withOptional(true);
    public static final AttributeDescriptor<Double> BATTERY_VOLTAGE = new AttributeDescriptor<>("batteryVoltage", ValueType.POSITIVE_NUMBER).withOptional(true);
    public static final AttributeDescriptor<Double> EXTERNAL_VOLTAGE = new AttributeDescriptor<>("externalVoltage", ValueType.POSITIVE_NUMBER).withOptional(true);
    public static final AttributeDescriptor<Integer> GSM_SIGNAL = new AttributeDescriptor<>("gsmSignal", ValueType.POSITIVE_INTEGER).withOptional(true);
    public static final AttributeDescriptor<Integer> TOTAL_ODOMETER = new AttributeDescriptor<>("totalOdometer", ValueType.POSITIVE_INTEGER).withOptional(true);

    public static final AssetDescriptor<TrackerAsset> DESCRIPTOR = new AssetDescriptor<>("crosshairs-gps", "005aff", TrackerAsset.class);

    /**
     * Default constructor for JPA.
     */
    protected TrackerAsset() {
    }

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

    public Optional<String> getManufacturer() {
        return getAttributes().getValue(MANUFACTURER);
    }

    public TrackerAsset setManufacturer(String manufacturer) {
        getAttributes().getOrCreate(MANUFACTURER).setValue(manufacturer);
        return this;
    }

    public Optional<String> getModel() {
        return getAttributes().getValue(MODEL);
    }

    public TrackerAsset setModel(String model) {
        getAttributes().getOrCreate(MODEL).setValue(model);
        return this;
    }
}
