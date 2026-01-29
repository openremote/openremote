package org.openremote.model.telematics.teltonika;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.openremote.model.asset.Asset;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.telematics.ParsingValueDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TeltonikaValueDescriptors {


    /**
     * Auto-generated Teltonika AVL parameter descriptors.
     * Generated from Teltonika documentation.
     */

    // Helper methods for reading specific byte sizes
    private static final Pattern ISO6709_PATTERN = Pattern.compile("^([+-]\\d{2}\\d{2}\\d{2}\\.\\d+)([+-]\\d{3}\\d{2}\\d{2}\\.\\d+)([+-]\\d{1,5}(?:\\.\\d+)?)?/?$");

    // Permanent I/O: 1-byte booleans
    private static final String PREFIX = TeltonikaValueDescriptor.VENDOR_PREFIX;

//    public static final TeltonikaValueDescriptor<Integer> IGNITION = new TeltonikaValueDescriptor<>(Integer.class, 1, buf -> (int) buf.readUnsignedByte(), 239, "Ignition", "Unsigned", 0L, 1L, null, null, "0 – Ignition Off\n\n1 – Ignition On", "FMBXXX", "Permanent I/O Elements");
    public static final TeltonikaValueDescriptor<Boolean> digitalInput1 = new TeltonikaValueDescriptor<>("1", Boolean.class, 1, ByteBuf::readBoolean);
    public static final TeltonikaValueDescriptor<Boolean> digitalOutput1 = new TeltonikaValueDescriptor<>("179", Boolean.class, 1, ByteBuf::readBoolean);
    public static final TeltonikaValueDescriptor<Boolean> ignition = new TeltonikaValueDescriptor<>("239", Boolean.class, 1, ByteBuf::readBoolean);
    public static final TeltonikaValueDescriptor<Boolean> movement = new TeltonikaValueDescriptor<>("240", Boolean.class, 1, ByteBuf::readBoolean);
    public static final TeltonikaValueDescriptor<Boolean> instantMovement = new TeltonikaValueDescriptor<>("303", Boolean.class, 1, ByteBuf::readBoolean);

    // Permanent I/O: 1-byte unsigned integers
    public static final TeltonikaValueDescriptor<Integer> gsmSignal = new TeltonikaValueDescriptor<>("21", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Integer> gnssStatus = new TeltonikaValueDescriptor<>("69", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Integer> dataMode = new TeltonikaValueDescriptor<>("80", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Integer> batteryLevel = new TeltonikaValueDescriptor<>("113", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Integer> sleepMode = new TeltonikaValueDescriptor<>("200", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Integer> networkType = new TeltonikaValueDescriptor<>("237", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Integer> btStatus = new TeltonikaValueDescriptor<>("263", Integer.class, 1, buf -> (int) buf.readUnsignedByte());

    // 2-byte unsigned integer values
    public static final TeltonikaValueDescriptor<Integer> speed = new TeltonikaValueDescriptor<>("24", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final TeltonikaValueDescriptor<Integer> gsmAreaCode = new TeltonikaValueDescriptor<>("206", Integer.class, 2, ByteBuf::readUnsignedShort);

    // 2-byte unsigned with multiplier (0.001)
    public static final TeltonikaValueDescriptor<Double> analogInput1 = new TeltonikaValueDescriptor<>("9", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final TeltonikaValueDescriptor<Double> externalVoltage = new TeltonikaValueDescriptor<>("66", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final TeltonikaValueDescriptor<Double> batteryVoltage = new TeltonikaValueDescriptor<>("67", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final TeltonikaValueDescriptor<Double> batteryCurrent = new TeltonikaValueDescriptor<>("68", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);

    // 2-byte unsigned with multiplier (0.1)
    public static final TeltonikaValueDescriptor<Double> gnssPdop = new TeltonikaValueDescriptor<>("181", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final TeltonikaValueDescriptor<Double> gnssHdop = new TeltonikaValueDescriptor<>("182", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);

    // 2-byte unsigned with multiplier (0.01)
    public static final TeltonikaValueDescriptor<Double> fuelRateGps = new TeltonikaValueDescriptor<>("13", Double.class, 2, buf -> buf.readUnsignedShort() * 0.01);
    public static final TeltonikaValueDescriptor<Double> ecoScore = new TeltonikaValueDescriptor<>("15", Double.class, 2, buf -> buf.readUnsignedShort() * 0.01);

    // 2-byte signed integer values
    public static final TeltonikaValueDescriptor<Integer> axisX = new TeltonikaValueDescriptor<>("17", Integer.class, 2, buf -> (int) buf.readShort());
    public static final TeltonikaValueDescriptor<Integer> axisY = new TeltonikaValueDescriptor<>("18", Integer.class, 2, buf -> (int) buf.readShort());
    public static final TeltonikaValueDescriptor<Integer> axisZ = new TeltonikaValueDescriptor<>("19", Integer.class, 2, buf -> (int) buf.readShort());

    public static final TeltonikaValueDescriptor<Integer> totalOdometer = new TeltonikaValueDescriptor<>("16", Integer.class, 4, ByteBuf::readInt);
    public static final TeltonikaValueDescriptor<Integer> tripOdometer = new TeltonikaValueDescriptor<>("199", Integer.class, 4, ByteBuf::readInt);
    public static final TeltonikaValueDescriptor<Long> activeGsmOperator = new TeltonikaValueDescriptor<>("241", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final TeltonikaValueDescriptor<Long> umtsLteCellId = new TeltonikaValueDescriptor<>("636", Long.class, 4, ByteBuf::readUnsignedInt);

    // 4-byte unsigned with multiplier (0.001)
    public static final TeltonikaValueDescriptor<Double> fuelUsedGps = new TeltonikaValueDescriptor<>("12", Double.class, 4, buf -> buf.readUnsignedInt() * 0.001);

    // 8-byte values (strings/IDs) - use String.format to preserve leading zeros
    public static final TeltonikaValueDescriptor<String> iccid1 = new TeltonikaValueDescriptor<>("11", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final TeltonikaValueDescriptor<String> iccid2 = new TeltonikaValueDescriptor<>("14", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final TeltonikaValueDescriptor<String> iButton = new TeltonikaValueDescriptor<>("78", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final TeltonikaValueDescriptor<String> userId = new TeltonikaValueDescriptor<>("238", String.class, 8, buf -> String.format("%016X", buf.readLong()));

    // Eventual I/O elements
    public static final TeltonikaValueDescriptor<Integer> trip = new TeltonikaValueDescriptor<>("250", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final TeltonikaValueDescriptor<Long> ignitionOnCounter = new TeltonikaValueDescriptor<>("449", Long.class, 4, ByteBuf::readUnsignedInt);

    public static final TeltonikaValueDescriptor<GeoJSONPoint> iso6709Coordinates = new TeltonikaValueDescriptor<>("387", GeoJSONPoint.class, 34, TeltonikaValueDescriptors::parseIso6709);

    public static final TeltonikaValueDescriptor<Integer> priority = new TeltonikaValueDescriptor<>("pr", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final TeltonikaValueDescriptor<Integer> altitude = new TeltonikaValueDescriptor<>("alt", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final TeltonikaValueDescriptor<Integer> direction = new TeltonikaValueDescriptor<>("ang", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final TeltonikaValueDescriptor<Integer> satellites = new TeltonikaValueDescriptor<>("sat", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final TeltonikaValueDescriptor<Integer> speedSatellite = new TeltonikaValueDescriptor<>("sp", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final TeltonikaValueDescriptor<Integer> eventTriggered = new TeltonikaValueDescriptor<>("evt", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final TeltonikaValueDescriptor<Long> timestamp = new TeltonikaValueDescriptor<>("ts", Long.class, TeltonikaValueDescriptors::parseLongFromUtf8);
    public static final TeltonikaValueDescriptor<GeoJSONPoint> location = new TeltonikaValueDescriptor<>("latlng", GeoJSONPoint.class, TeltonikaValueDescriptors::parseLatLng);

    private static final Map<String, TeltonikaValueDescriptor<?>> DESCRIPTORS_BY_ID;
    private static final Map<String, TeltonikaValueDescriptor<?>> DESCRIPTORS_BY_NAME;

    static {
        var descriptors = Arrays.stream(TeltonikaValueDescriptors.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) &&
                        TeltonikaValueDescriptor.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    try {
                        return (TeltonikaValueDescriptor<?>) field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(desc -> desc.getName() != null)
                .toList();

        DESCRIPTORS_BY_ID = descriptors.stream().collect(Collectors.toMap(TeltonikaValueDescriptor::getName, Function.identity()));
        DESCRIPTORS_BY_NAME = descriptors.stream().collect(Collectors.toMap(TeltonikaValueDescriptor::getName, Function.identity()));
    }

    public static Optional<TeltonikaValueDescriptor<?>> getByName(String name) {
        return Optional.ofNullable(DESCRIPTORS_BY_NAME.get(name));
    }

    /**
     * Scans all AttributeDescriptor fields in the given Asset class and returns the one
     * that matches the input ParsingValueDescriptor by name.
     *
     * @param assetClass The Asset class to scan for AttributeDescriptor fields
     * @param valueDescriptor The ParsingValueDescriptor to match against
     * @param <T> The type of the value held by the descriptor
     * @return An Optional containing the matching AttributeDescriptor, or empty if no match is found
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<AttributeDescriptor<T>> findMatchingAttributeDescriptor(
            Class<? extends Asset<?>> assetClass,
            ValueDescriptor<T> valueDescriptor) {

        return Arrays.stream(assetClass.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) &&
                        AttributeDescriptor.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    try {
                        return (AttributeDescriptor<?>) field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(attrDesc -> attrDesc.getType().getName().equals(valueDescriptor.getName()))
                .findFirst()
                .map(attrDesc -> (AttributeDescriptor<T>) attrDesc);
    }

    // Static map for fast descriptor lookup by ID
    private static final Map<String, TeltonikaValueDescriptor<?>> DESCRIPTOR_MAP = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(TeltonikaValueDescriptors.class.getName());

    static {
        // Populate descriptor map using reflection
        for (Field field : TeltonikaValueDescriptors.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                TeltonikaValueDescriptor.class.isAssignableFrom(field.getType())) {
                try {
                    TeltonikaValueDescriptor<?> descriptor = (TeltonikaValueDescriptor<?>) field.get(null);
                    if (descriptor == null){
                        throw new IllegalStateException("Descriptor field " + field.getName() + " is null");
                    }
                    DESCRIPTOR_MAP.put(descriptor.getName(), descriptor);
                } catch (IllegalAccessException e) {
                    // Skip fields that can't be accessed
                }
            }
        }
        LOG.info("Added "+DESCRIPTOR_MAP.size()+" descriptors as Teltonika Value Descriptors");
    }

    /**
     * Get a TeltonikaValueDescriptor by its AVL ID.
     *
     * @param id The AVL ID as a string (e.g., "11", "239", "sp")
     * @return The corresponding TeltonikaValueDescriptor, or null if not found
     */
    public static TeltonikaValueDescriptor<?> getDescriptorById(String id) {
        return DESCRIPTOR_MAP.get(PREFIX + "_" + id);
    }

    // Custom non-AVL parameters (well-known Teltonika keys)
    private static int parseIntFromUtf8(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return Integer.parseInt(new String(bytes, StandardCharsets.UTF_8).trim());
    }

    private static long parseLongFromUtf8(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return Long.parseLong(new String(bytes, StandardCharsets.UTF_8).trim());
    }

    private static GeoJSONPoint parseLatLng(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8).trim();
        String[] parts = value.split(",");
        double lat = parts.length > 0 ? Double.parseDouble(parts[0]) : 0.0;
        double lon = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.0;
        return new GeoJSONPoint(lon, lat);
    }

    private static GeoJSONPoint parseIso6709(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8).trim();
        var matcher = ISO6709_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        double lat = dmsToDecimal(matcher.group(1));
        double lon = dmsToDecimal(matcher.group(2));
        return new GeoJSONPoint(lon, lat);
    }

    private static double dmsToDecimal(String dms) {
        if (dms == null || dms.length() < 7) {
            return 0.0;
        }
        boolean positive = dms.charAt(0) == '+';
        int degDigits = (dms.length() - 1 == 12) ? 3 : 2;
        int offset = 1;
        int degrees = Integer.parseInt(dms.substring(offset, offset + degDigits));
        offset += degDigits;
        int minutes = Integer.parseInt(dms.substring(offset, offset + 2));
        offset += 2;
        double seconds = Double.parseDouble(dms.substring(offset));
        double decimal = degrees + minutes / 60.0 + seconds / 3600.0;
        return positive ? decimal : -decimal;
    }
}
