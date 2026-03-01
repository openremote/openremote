package org.openremote.model.telematics.teltonika;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

/**
 * Teltonika-specific tracker asset with AVL protocol attributes.
 * Extends the base TrackerAsset with Teltonika-specific I/O elements.
 */
@Entity
public class TeltonikaTrackerAsset extends Asset<TeltonikaTrackerAsset> {

    // Override base class descriptors with Teltonika-specific parameters for parsing
    public static final AttributeDescriptor<Integer> PRIORITY = new AttributeDescriptor<>("priority", TeltonikaParameters.PRIORITY).withOptional(true);
    public static final AttributeDescriptor<Integer> ALTITUDE = new AttributeDescriptor<>("altitude", TeltonikaParameters.ALTITUDE).withOptional(true);
    public static final AttributeDescriptor<Integer> DIRECTION = new AttributeDescriptor<>("direction", TeltonikaParameters.DIRECTION).withOptional(true);
    public static final AttributeDescriptor<Integer> SATELLITES = new AttributeDescriptor<>("satellites", TeltonikaParameters.SATELLITES).withOptional(true);
    public static final AttributeDescriptor<Integer> SPEED_SATELLITE = new AttributeDescriptor<>("speedSatellite", TeltonikaParameters.SPEED_SATELLITE).withOptional(true);
    public static final AttributeDescriptor<Integer> EVENT_TRIGGERED = new AttributeDescriptor<>("eventTriggered", TeltonikaParameters.EVENT_TRIGGERED).withOptional(true);
    public static final AttributeDescriptor<Long> TIMESTAMP = new AttributeDescriptor<>("timestamp", ValueType.TIMESTAMP).withOptional(true);
    public static final AttributeDescriptor<GeoJSONPoint> GPS_LOCATION = new AttributeDescriptor<>("gpsLocation", TeltonikaParameters.LOCATION).withOptional(true);
    public static final AttributeDescriptor<Integer> SPEED = new AttributeDescriptor<>("speed", TeltonikaParameters.SPEED).withOptional(true);

    // Digital I/O
    public static final AttributeDescriptor<Boolean> DIGITAL_INPUT_1 = new AttributeDescriptor<>("digitalInput1", TeltonikaParameters.DIGITAL_INPUT_1).withOptional(true);
    public static final AttributeDescriptor<Boolean> DIGITAL_OUTPUT_1 = new AttributeDescriptor<>("digitalOutput1", TeltonikaParameters.DIGITAL_OUTPUT_1).withOptional(true);
    public static final AttributeDescriptor<Boolean> IGNITION = new AttributeDescriptor<>("ignition", TeltonikaParameters.IGNITION).withOptional(true);
    public static final AttributeDescriptor<Boolean> MOVEMENT = new AttributeDescriptor<>("movement", TeltonikaParameters.MOVEMENT).withOptional(true);
    public static final AttributeDescriptor<Boolean> INSTANT_MOVEMENT = new AttributeDescriptor<>("instantMovement", TeltonikaParameters.INSTANT_MOVEMENT).withOptional(true);

    // Analog I/O
    public static final AttributeDescriptor<Double> ANALOG_INPUT_1 = new AttributeDescriptor<>("analogInput1", TeltonikaParameters.ANALOG_INPUT_1).withOptional(true);
    public static final AttributeDescriptor<Double> EXTERNAL_VOLTAGE = new AttributeDescriptor<>("externalVoltage", TeltonikaParameters.EXTERNAL_VOLTAGE).withOptional(true);
    public static final AttributeDescriptor<Double> BATTERY_VOLTAGE = new AttributeDescriptor<>("batteryVoltage", TeltonikaParameters.BATTERY_VOLTAGE).withOptional(true);
    public static final AttributeDescriptor<Double> BATTERY_CURRENT = new AttributeDescriptor<>("batteryCurrent", TeltonikaParameters.BATTERY_CURRENT).withOptional(true);

    // SIM/Network
    public static final AttributeDescriptor<String> ICCID_1 = new AttributeDescriptor<>("iccid1", TeltonikaParameters.ICCID1).withOptional(true);
    public static final AttributeDescriptor<String> ICCID_2 = new AttributeDescriptor<>("iccid2", TeltonikaParameters.ICCID2).withOptional(true);
    public static final AttributeDescriptor<Integer> GSM_SIGNAL = new AttributeDescriptor<>("gsmSignal", TeltonikaParameters.GSM_SIGNAL).withOptional(true);
    public static final AttributeDescriptor<Integer> GSM_AREA_CODE = new AttributeDescriptor<>("gsmAreaCode", TeltonikaParameters.GSM_AREA_CODE).withOptional(true);
    public static final AttributeDescriptor<Integer> NETWORK_TYPE = new AttributeDescriptor<>("networkType", TeltonikaParameters.NETWORK_TYPE).withOptional(true);
    public static final AttributeDescriptor<Long> ACTIVE_GSM_OPERATOR = new AttributeDescriptor<>("activeGsmOperator", TeltonikaParameters.ACTIVE_GSM_OPERATOR).withOptional(true);
    public static final AttributeDescriptor<Long> UMTS_LTE_CELL_ID = new AttributeDescriptor<>("umtsLteCellId", TeltonikaParameters.UMTS_LTE_CELL_ID).withOptional(true);

    // Fuel/Eco
    public static final AttributeDescriptor<Double> FUEL_USED_GPS = new AttributeDescriptor<>("fuelUsedGps", TeltonikaParameters.FUEL_USED_GPS).withOptional(true);
    public static final AttributeDescriptor<Double> FUEL_RATE_GPS = new AttributeDescriptor<>("fuelRateGps", TeltonikaParameters.FUEL_RATE_GPS).withOptional(true);
    public static final AttributeDescriptor<Double> ECO_SCORE = new AttributeDescriptor<>("ecoScore", TeltonikaParameters.ECO_SCORE).withOptional(true);

    // Odometer/Trip
    public static final AttributeDescriptor<Long> TOTAL_ODOMETER = new AttributeDescriptor<>("totalOdometer", TeltonikaParameters.TOTAL_ODOMETER).withOptional(true);
    public static final AttributeDescriptor<Long> TRIP_ODOMETER = new AttributeDescriptor<>("tripOdometer", TeltonikaParameters.TRIP_ODOMETER).withOptional(true);
    public static final AttributeDescriptor<Boolean> TRIP = new AttributeDescriptor<>("trip", TeltonikaParameters.TRIP).withOptional(true);

    // Accelerometer
    public static final AttributeDescriptor<Integer> AXIS_X = new AttributeDescriptor<>("axisX", TeltonikaParameters.AXIS_X).withOptional(true);
    public static final AttributeDescriptor<Integer> AXIS_Y = new AttributeDescriptor<>("axisY", TeltonikaParameters.AXIS_Y).withOptional(true);
    public static final AttributeDescriptor<Integer> AXIS_Z = new AttributeDescriptor<>("axisZ", TeltonikaParameters.AXIS_Z).withOptional(true);

    // Device status
    public static final AttributeDescriptor<Integer> BATTERY_LEVEL = new AttributeDescriptor<>("batteryLevel", TeltonikaParameters.BATTERY_LEVEL).withOptional(true);
    public static final AttributeDescriptor<Integer> SLEEP_MODE = new AttributeDescriptor<>("sleepMode", TeltonikaParameters.SLEEP_MODE).withOptional(true);
    public static final AttributeDescriptor<Integer> DATA_MODE = new AttributeDescriptor<>("dataMode", TeltonikaParameters.DATA_MODE).withOptional(true);
    public static final AttributeDescriptor<Integer> BT_STATUS = new AttributeDescriptor<>("btStatus", TeltonikaParameters.BT_STATUS).withOptional(true);
    public static final AttributeDescriptor<Integer> GNSS_STATUS = new AttributeDescriptor<>("gnssStatus", TeltonikaParameters.GNSS_STATUS).withOptional(true);
    public static final AttributeDescriptor<Double> GNSS_PDOP = new AttributeDescriptor<>("gnssPdop", TeltonikaParameters.GNSS_PDOP).withOptional(true);
    public static final AttributeDescriptor<Double> GNSS_HDOP = new AttributeDescriptor<>("gnssHdop", TeltonikaParameters.GNSS_HDOP).withOptional(true);

    // Authentication/ID
    public static final AttributeDescriptor<String> USER_ID = new AttributeDescriptor<>("userId", TeltonikaParameters.USER_ID).withOptional(true);
    public static final AttributeDescriptor<String> IBUTTON = new AttributeDescriptor<>("iButton", TeltonikaParameters.IBUTTON).withOptional(true);

    // Counters
    public static final AttributeDescriptor<Long> IGNITION_ON_COUNTER = new AttributeDescriptor<>("ignitionOnCounter", TeltonikaParameters.IGNITION_ON_COUNTER).withOptional(true);

    // Coordinates
    public static final AttributeDescriptor<GeoJSONPoint> ISO6709_COORDINATES = new AttributeDescriptor<>("iso6709Coordinates", TeltonikaParameters.ISO6709_COORDINATES).withOptional(true);

    // Device identification
    public static final AttributeDescriptor<String> IMEI = new AttributeDescriptor<>("imei", ValueType.TEXT);
    public static final AttributeDescriptor<String> MANUFACTURER = Asset.MANUFACTURER.withOptional(false);
    public static final AttributeDescriptor<String> MODEL = Asset.MODEL.withOptional(false);
    public static final AttributeDescriptor<String> PROTOCOL = new AttributeDescriptor<>("protocol", ValueType.TEXT);
    public static final AttributeDescriptor<String> CODEC = new AttributeDescriptor<>("codec", ValueType.TEXT).withOptional(true);

    // Dynamic parameter descriptor storage
    public static final AttributeDescriptor<ValueDescriptor[]> VALUE_DESCRIPTORS = new AttributeDescriptor<>("attributeDescriptors", org.openremote.model.value.ValueType.VALUE_DESCRIPTOR_VALUE_DESCRIPTOR.asArray()).withOptional(true);

    public static final AssetDescriptor<TeltonikaTrackerAsset> DESCRIPTOR = new AssetDescriptor<>("crosshairs-gps", "00a0ff", TeltonikaTrackerAsset.class);

    /**
     * Default constructor for JPA.
     */
    public TeltonikaTrackerAsset() {
        super();
    }

    public TeltonikaTrackerAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }

    public TeltonikaTrackerAsset setModel(String model) {
        super.setModel(model);
        return this;
    }

    public void setImei(String imei) {
        getAttributes().getOrCreate(IMEI).setValue(imei);
    }

    public void setCodec(String codec) {
        getAttributes().getOrCreate(CODEC).setValue(codec);
    }

    public void setProtocol(String protocol) {
        getAttributes().getOrCreate(PROTOCOL).setValue(protocol);
    }
}
