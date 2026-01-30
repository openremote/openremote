package org.openremote.model.telematics.teltonika;

import io.netty.buffer.ByteBuf;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.telematics.parameter.StandardParsers;

import java.nio.charset.StandardCharsets;

/**
 * Teltonika AVL parameter definitions.
 * <p>
 * All known Teltonika parameters defined as static constants.
 * Each parameter extends {@link org.openremote.model.value.ValueDescriptor}
 * with parsing capability.
 */
public final class TeltonikaParameters {

    private TeltonikaParameters() {}

    // ========== Permanent I/O: Boolean (1-byte) ==========

    public static final TeltonikaParameter<Boolean> DIGITAL_INPUT_1 = TeltonikaParameter.<Boolean>builder()
            .id(1).type(Boolean.class).byteLength(1).parser(StandardParsers.BOOLEAN)
            .displayName("Digital Input 1").parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<Boolean> DIGITAL_OUTPUT_1 = TeltonikaParameter.<Boolean>builder()
            .id(179).type(Boolean.class).byteLength(1).parser(StandardParsers.BOOLEAN)
            .displayName("Digital Output 1").parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<Boolean> IGNITION = TeltonikaParameter.<Boolean>builder()
            .id(239).type(Boolean.class).byteLength(1).parser(StandardParsers.BOOLEAN)
            .displayName("Ignition").parameterGroup("Permanent I/O Elements")
            .description("0 – Ignition Off, 1 – Ignition On")
            .build();

    public static final TeltonikaParameter<Boolean> MOVEMENT = TeltonikaParameter.<Boolean>builder()
            .id(240).type(Boolean.class).byteLength(1).parser(StandardParsers.BOOLEAN)
            .displayName("Movement").parameterGroup("Permanent I/O Elements")
            .build();

    // ========== Permanent I/O: Unsigned Byte (1-byte) ==========

    public static final TeltonikaParameter<Integer> GSM_SIGNAL = TeltonikaParameter.<Integer>builder()
            .id(21).type(Integer.class).byteLength(1).parser(StandardParsers.UNSIGNED_BYTE)
            .displayName("GSM Signal").parameterGroup("Permanent I/O Elements")
            .minValue(0L).maxValue(5L)
            .build();

    public static final TeltonikaParameter<Integer> BATTERY_LEVEL = TeltonikaParameter.<Integer>builder()
            .id(113).type(Integer.class).byteLength(1).parser(StandardParsers.UNSIGNED_BYTE)
            .displayName("Battery Level").units("%").parameterGroup("Permanent I/O Elements")
            .minValue(0L).maxValue(100L)
            .build();

    public static final TeltonikaParameter<Integer> SLEEP_MODE = TeltonikaParameter.<Integer>builder()
            .id(200).type(Integer.class).byteLength(1).parser(StandardParsers.UNSIGNED_BYTE)
            .displayName("Sleep Mode").parameterGroup("Permanent I/O Elements")
            .build();

    // ========== 2-byte Unsigned Integer ==========

    public static final TeltonikaParameter<Integer> SPEED = TeltonikaParameter.<Integer>builder()
            .id(24).type(Integer.class).byteLength(2).parser(StandardParsers.UNSIGNED_SHORT)
            .displayName("Speed").units("km/h").parameterGroup("Permanent I/O Elements")
            .build();

    // ========== 2-byte with Multiplier ==========

    public static final TeltonikaParameter<Double> EXTERNAL_VOLTAGE = TeltonikaParameter.<Double>builder()
            .id(66).type(Double.class).byteLength(2).parser(StandardParsers.unsignedShortWithMultiplier(0.001))
            .displayName("External Voltage").units("V").multiplier(0.001)
            .parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<Double> BATTERY_VOLTAGE = TeltonikaParameter.<Double>builder()
            .id(67).type(Double.class).byteLength(2).parser(StandardParsers.unsignedShortWithMultiplier(0.001))
            .displayName("Battery Voltage").units("V").multiplier(0.001)
            .parameterGroup("Permanent I/O Elements")
            .build();

    // ========== 2-byte Signed ==========

    public static final TeltonikaParameter<Integer> AXIS_X = TeltonikaParameter.<Integer>builder()
            .id(17).type(Integer.class).byteLength(2).parser(StandardParsers.SIGNED_SHORT)
            .displayName("Axis X").units("mG").parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<Integer> AXIS_Y = TeltonikaParameter.<Integer>builder()
            .id(18).type(Integer.class).byteLength(2).parser(StandardParsers.SIGNED_SHORT)
            .displayName("Axis Y").units("mG").parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<Integer> AXIS_Z = TeltonikaParameter.<Integer>builder()
            .id(19).type(Integer.class).byteLength(2).parser(StandardParsers.SIGNED_SHORT)
            .displayName("Axis Z").units("mG").parameterGroup("Permanent I/O Elements")
            .build();

    // ========== 4-byte Values ==========

    public static final TeltonikaParameter<Integer> TOTAL_ODOMETER = TeltonikaParameter.<Integer>builder()
            .id(16).type(Integer.class).byteLength(4).parser(StandardParsers.SIGNED_INT)
            .displayName("Total Odometer").units("m").parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<Long> ACTIVE_GSM_OPERATOR = TeltonikaParameter.<Long>builder()
            .id(241).type(Long.class).byteLength(4).parser(StandardParsers.UNSIGNED_INT)
            .displayName("Active GSM Operator").parameterGroup("Permanent I/O Elements")
            .build();

    // ========== 8-byte Values (Hex Strings) ==========

    public static final TeltonikaParameter<String> ICCID_1 = TeltonikaParameter.<String>builder()
            .id(11).type(String.class).byteLength(8).parser(StandardParsers.HEX_LONG)
            .displayName("ICCID 1").parameterGroup("Permanent I/O Elements")
            .build();

    public static final TeltonikaParameter<String> IBUTTON = TeltonikaParameter.<String>builder()
            .id(78).type(String.class).byteLength(8).parser(StandardParsers.HEX_LONG)
            .displayName("iButton").parameterGroup("Permanent I/O Elements")
            .build();

    // ========== GPS Frame Elements (variable length, from JSON) ==========

    public static final TeltonikaParameter<Integer> PRIORITY = TeltonikaParameter.<Integer>builder()
            .id("pr").type(Integer.class).parser((buf, len) -> parseIntFromUtf8(buf, len))
            .displayName("Priority")
            .build();

    public static final TeltonikaParameter<Integer> ALTITUDE = TeltonikaParameter.<Integer>builder()
            .id("alt").type(Integer.class).parser((buf, len) -> parseIntFromUtf8(buf, len))
            .displayName("Altitude").units("m")
            .build();

    public static final TeltonikaParameter<Integer> DIRECTION = TeltonikaParameter.<Integer>builder()
            .id("ang").type(Integer.class).parser((buf, len) -> parseIntFromUtf8(buf, len))
            .displayName("Direction").units("°")
            .build();

    public static final TeltonikaParameter<Integer> SATELLITES = TeltonikaParameter.<Integer>builder()
            .id("sat").type(Integer.class).parser((buf, len) -> parseIntFromUtf8(buf, len))
            .displayName("Satellites")
            .build();

    public static final TeltonikaParameter<Integer> SPEED_SATELLITE = TeltonikaParameter.<Integer>builder()
            .id("sp").type(Integer.class).parser((buf, len) -> parseIntFromUtf8(buf, len))
            .displayName("Speed (Satellite)").units("km/h")
            .build();

    public static final TeltonikaParameter<Long> TIMESTAMP = TeltonikaParameter.<Long>builder()
            .id("ts").type(Long.class).parser((buf, len) -> parseLongFromUtf8(buf, len))
            .displayName("Timestamp")
            .build();

    public static final TeltonikaParameter<GeoJSONPoint> LOCATION = TeltonikaParameter.<GeoJSONPoint>builder()
            .id("latlng").type(GeoJSONPoint.class).parser((buf, len) -> parseLatLng(buf, len))
            .displayName("Location")
            .build();

    // ========== Helper Parsing Methods ==========

    private static int parseIntFromUtf8(ByteBuf buf, int len) {
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return Integer.parseInt(new String(bytes, StandardCharsets.UTF_8).trim());
    }

    private static long parseLongFromUtf8(ByteBuf buf, int len) {
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return Long.parseLong(new String(bytes, StandardCharsets.UTF_8).trim());
    }

    private static GeoJSONPoint parseLatLng(ByteBuf buf, int len) {
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8).trim();
        String[] parts = value.split(",");
        double lat = parts.length > 0 ? Double.parseDouble(parts[0]) : 0.0;
        double lon = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.0;
        return new GeoJSONPoint(lon, lat);
    }
}
