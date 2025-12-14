package org.openremote.model.telematics;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.telematics.teltonika.TeltonikaValueDescriptors;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class TrackerAsset extends Asset<TrackerAsset> {
    // Basic Asset Attributes
    public static final AttributeDescriptor<String> IMEI = new AttributeDescriptor<>("imei", ValueType.TEXT);
    public static final AttributeDescriptor<String> MANUFACTURER = Asset.MANUFACTURER.withOptional(false);
    public static final AttributeDescriptor<String> MODEL = Asset.MODEL.withOptional(false);
    public static final AttributeDescriptor<String> PROTOCOL = new AttributeDescriptor<>("protocol", ValueType.TEXT);
    public static final AttributeDescriptor<String> CODEC = new AttributeDescriptor<>("codec", ValueType.TEXT).withOptional(false);

    // Well-known Teltonika descriptors (not standard AVL IDs)
    public static final AttributeDescriptor<Integer> PRIORITY = new AttributeDescriptor<>("priority", TeltonikaValueDescriptors.priority).withOptional(true);
    public static final AttributeDescriptor<Integer> ALTITUDE = new AttributeDescriptor<>("altitude", TeltonikaValueDescriptors.altitude).withOptional(true);
    public static final AttributeDescriptor<Integer> DIRECTION = new AttributeDescriptor<>("direction", TeltonikaValueDescriptors.direction).withOptional(true);
    public static final AttributeDescriptor<Integer> SATELLITES = new AttributeDescriptor<>("satellites", TeltonikaValueDescriptors.satellites).withOptional(true);
    public static final AttributeDescriptor<Integer> SPEED_SATELLITE = new AttributeDescriptor<>("speedSatellite", TeltonikaValueDescriptors.speedSatellite).withOptional(true);
    public static final AttributeDescriptor<Integer> EVENT_TRIGGERED = new AttributeDescriptor<>("eventTriggered", TeltonikaValueDescriptors.eventTriggered).withOptional(true);
    public static final AttributeDescriptor<Long> TIMESTAMP = new AttributeDescriptor<>("timestamp", TeltonikaValueDescriptors.timestamp).withOptional(true);
    public static final AttributeDescriptor<GeoJSONPoint> GPS_LOCATION = new AttributeDescriptor<>("gpsLocation", TeltonikaValueDescriptors.location).withOptional(true);

    // Standard AVL ID descriptors
    public static final AttributeDescriptor<Boolean> DIGITAL_INPUT_1 = new AttributeDescriptor<>("digitalInput1", TeltonikaValueDescriptors.digitalInput1).withOptional(true);
    public static final AttributeDescriptor<Double> ANALOG_INPUT_1 = new AttributeDescriptor<>("analogInput1", TeltonikaValueDescriptors.analogInput1).withOptional(true);
    public static final AttributeDescriptor<String> ICCID_1 = new AttributeDescriptor<>("iccid1", TeltonikaValueDescriptors.iccid1).withOptional(true);
    public static final AttributeDescriptor<String> ICCID_2 = new AttributeDescriptor<>("iccid2", TeltonikaValueDescriptors.iccid2).withOptional(true);
    public static final AttributeDescriptor<Double> FUEL_USED_GPS = new AttributeDescriptor<>("fuelUsedGps", TeltonikaValueDescriptors.fuelUsedGps).withOptional(true);
    public static final AttributeDescriptor<Double> FUEL_RATE_GPS = new AttributeDescriptor<>("fuelRateGps", TeltonikaValueDescriptors.fuelRateGps).withOptional(true);
    public static final AttributeDescriptor<Double> ECO_SCORE = new AttributeDescriptor<>("ecoScore", TeltonikaValueDescriptors.ecoScore).withOptional(true);
    public static final AttributeDescriptor<Integer> TOTAL_ODOMETER = new AttributeDescriptor<>("totalOdometer", TeltonikaValueDescriptors.totalOdometer).withOptional(true);
    public static final AttributeDescriptor<Integer> AXIS_X = new AttributeDescriptor<>("axisX", TeltonikaValueDescriptors.axisX).withOptional(true);
    public static final AttributeDescriptor<Integer> AXIS_Y = new AttributeDescriptor<>("axisY", TeltonikaValueDescriptors.axisY).withOptional(true);
    public static final AttributeDescriptor<Integer> AXIS_Z = new AttributeDescriptor<>("axisZ", TeltonikaValueDescriptors.axisZ).withOptional(true);
    public static final AttributeDescriptor<Boolean> DIGITAL_OUTPUT_1 = new AttributeDescriptor<>("digitalOutput1", TeltonikaValueDescriptors.digitalOutput1).withOptional(true);
    public static final AttributeDescriptor<Integer> TRIP_ODOMETER = new AttributeDescriptor<>("tripOdometer", TeltonikaValueDescriptors.tripOdometer).withOptional(true);
    public static final AttributeDescriptor<Integer> SLEEP_MODE = new AttributeDescriptor<>("sleepMode", TeltonikaValueDescriptors.sleepMode).withOptional(true);
    public static final AttributeDescriptor<Integer> GSM_AREA_CODE = new AttributeDescriptor<>("gsmAreaCode", TeltonikaValueDescriptors.gsmAreaCode).withOptional(true);
    public static final AttributeDescriptor<Integer> NETWORK_TYPE = new AttributeDescriptor<>("networkType", TeltonikaValueDescriptors.networkType).withOptional(true);
    public static final AttributeDescriptor<String> USER_ID = new AttributeDescriptor<>("userId", TeltonikaValueDescriptors.userId).withOptional(true);
    public static final AttributeDescriptor<Boolean> IGNITION = new AttributeDescriptor<>("ignition", TeltonikaValueDescriptors.ignition).withOptional(true);
    public static final AttributeDescriptor<Boolean> MOVEMENT = new AttributeDescriptor<>("movement", TeltonikaValueDescriptors.movement).withOptional(true);
    public static final AttributeDescriptor<Long> ACTIVE_GSM_OPERATOR = new AttributeDescriptor<>("activeGsmOperator", TeltonikaValueDescriptors.activeGsmOperator).withOptional(true);
    public static final AttributeDescriptor<Integer> TRIP = new AttributeDescriptor<>("trip", TeltonikaValueDescriptors.trip).withOptional(true);
    public static final AttributeDescriptor<Integer> BT_STATUS = new AttributeDescriptor<>("btStatus", TeltonikaValueDescriptors.btStatus).withOptional(true);
    public static final AttributeDescriptor<Integer> DATA_MODE = new AttributeDescriptor<>("dataMode", TeltonikaValueDescriptors.dataMode).withOptional(true);
    public static final AttributeDescriptor<Boolean> INSTANT_MOVEMENT = new AttributeDescriptor<>("instantMovement", TeltonikaValueDescriptors.instantMovement).withOptional(true);
    public static final AttributeDescriptor<GeoJSONPoint> ISO6709_COORDINATES = new AttributeDescriptor<>("iso6709Coordinates", TeltonikaValueDescriptors.iso6709Coordinates).withOptional(true);
    public static final AttributeDescriptor<Long> IGNITION_ON_COUNTER = new AttributeDescriptor<>("ignitionOnCounter", TeltonikaValueDescriptors.ignitionOnCounter).withOptional(true);
    public static final AttributeDescriptor<Long> UMTS_LTE_CELL_ID = new AttributeDescriptor<>("umtsLteCellId", TeltonikaValueDescriptors.umtsLteCellId).withOptional(true);
    public static final AttributeDescriptor<Integer> SPEED = new AttributeDescriptor<>("speed", TeltonikaValueDescriptors.speed).withOptional(true);
    public static final AttributeDescriptor<Double> EXTERNAL_VOLTAGE = new AttributeDescriptor<>("externalVoltage", TeltonikaValueDescriptors.externalVoltage).withOptional(true);
    public static final AttributeDescriptor<Double> BATTERY_VOLTAGE = new AttributeDescriptor<>("batteryVoltage", TeltonikaValueDescriptors.batteryVoltage).withOptional(true);
    public static final AttributeDescriptor<Double> BATTERY_CURRENT = new AttributeDescriptor<>("batteryCurrent", TeltonikaValueDescriptors.batteryCurrent).withOptional(true);
    public static final AttributeDescriptor<Integer> BATTERY_LEVEL = new AttributeDescriptor<>("batteryLevel", TeltonikaValueDescriptors.batteryLevel).withOptional(true);
    public static final AttributeDescriptor<Integer> GSM_SIGNAL = new AttributeDescriptor<>("gsmSignal", TeltonikaValueDescriptors.gsmSignal).withOptional(true);
    public static final AttributeDescriptor<Integer> GNSS_STATUS = new AttributeDescriptor<>("gnssStatus", TeltonikaValueDescriptors.gnssStatus).withOptional(true);
    public static final AttributeDescriptor<Double> GNSS_PDOP = new AttributeDescriptor<>("gnssPdop", TeltonikaValueDescriptors.gnssPdop).withOptional(true);
    public static final AttributeDescriptor<Double> GNSS_HDOP = new AttributeDescriptor<>("gnssHdop", TeltonikaValueDescriptors.gnssHdop).withOptional(true);
    public static final AttributeDescriptor<String> IBUTTON = new AttributeDescriptor<>("iButton", TeltonikaValueDescriptors.iButton).withOptional(true);

    public static final AttributeDescriptor<ValueDescriptor[]> VALUE_DESCRIPTORS = new AttributeDescriptor<>("attributeDescriptors", ValueType.VALUE_DESCRIPTOR_VALUE_DESCRIPTOR.asArray()).withOptional(true);

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
