package org.openremote.model.telematics.teltonika;

import org.openremote.model.asset.Asset;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.telematics.ParsingValueDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class TeltonikaValueDescriptors {

    // Helper methods for reading specific byte sizes
    private static final Pattern ISO6709_PATTERN = Pattern.compile("^([+-]\\d{2}\\d{2}\\d{2}\\.\\d+)([+-]\\d{3}\\d{2}\\d{2}\\.\\d+)([+-]\\d{1,5}(?:\\.\\d+)?)?/?$");
    private static byte readByte(ByteBuf buf) {
        return buf.readByte();
    }

    private static int readInt(ByteBuf buf) {
        return buf.readInt();
    }

    private static long readUnsignedInt(ByteBuf buf) {
        return buf.readUnsignedInt();
    }

    private static long readLong(ByteBuf buf) {
        return buf.readLong();
    }

    private static String readString(ByteBuf buf, int length) {
        int readable = Math.min(length, buf.readableBytes());
        byte[] bytes = new byte[readable];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String bytesToHex(byte[] bytes) {
        return ByteBufUtil.hexDump(bytes).toUpperCase(Locale.ROOT);
    }

    private static String readRemainingAsHex(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytesToHex(bytes);
    }

    private static ParsingValueDescriptor<Integer> geofenceZoneDescriptor(int avlId) {
        return new ParsingValueDescriptor<>(String.valueOf(avlId), Integer.class, 1, bb -> (int) bb.readUnsignedByte());
    }

    private static String readFixedHex(ByteBuf buf, int length) {
        int readable = Math.min(length, buf.readableBytes());
        byte[] bytes = new byte[readable];
        buf.readBytes(bytes);
        return bytesToHex(bytes);
    }

    // Permanent I/O: 1-byte booleans
    public static final ParsingValueDescriptor<Boolean> digitalInput1 = new ParsingValueDescriptor<>("1", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> digitalInput2 = new ParsingValueDescriptor<>("2", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> digitalInput3 = new ParsingValueDescriptor<>("3", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> sdStatus = new ParsingValueDescriptor<>("10", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> digitalOutput1 = new ParsingValueDescriptor<>("179", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> digitalOutput2 = new ParsingValueDescriptor<>("180", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ignition = new ParsingValueDescriptor<>("239", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> movement = new ParsingValueDescriptor<>("240", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> instantMovement = new ParsingValueDescriptor<>("303", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> digitalOutput3 = new ParsingValueDescriptor<>("380", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> groundSense = new ParsingValueDescriptor<>("381", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> wakeReason = new ParsingValueDescriptor<>("637", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> eyeMagnet1 = new ParsingValueDescriptor<>("10808", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> eyeMagnet2 = new ParsingValueDescriptor<>("10809", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> eyeMagnet3 = new ParsingValueDescriptor<>("10810", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> eyeMagnet4 = new ParsingValueDescriptor<>("10811", Boolean.class, 1, ByteBuf::readBoolean);

    // Permanent I/O: 1-byte unsigned integers
    public static final ParsingValueDescriptor<Integer> gsmSignal = new ParsingValueDescriptor<>("21", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> gnssStatus = new ParsingValueDescriptor<>("69", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> dataMode = new ParsingValueDescriptor<>("80", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> batteryLevel = new ParsingValueDescriptor<>("113", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> sleepMode = new ParsingValueDescriptor<>("200", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> networkType = new ParsingValueDescriptor<>("237", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> btStatus = new ParsingValueDescriptor<>("263", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> axlCalibrationStatus = new ParsingValueDescriptor<>("383", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> driverCardLicenseType = new ParsingValueDescriptor<>("404", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> driverGender = new ParsingValueDescriptor<>("405", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> driverCardExpirationDate = new ParsingValueDescriptor<>("407", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> driverStatusEvent = new ParsingValueDescriptor<>("409", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> ul202SensorStatus = new ParsingValueDescriptor<>("483", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> msp500SpeedSensorStatus = new ParsingValueDescriptor<>("502", Integer.class, 1, buf -> (int) buf.readUnsignedByte());

    // Permanent I/O: 1-byte signed integers
    public static final ParsingValueDescriptor<Integer> lls1Temperature = new ParsingValueDescriptor<>("202", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> lls2Temperature = new ParsingValueDescriptor<>("204", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> lls3Temperature = new ParsingValueDescriptor<>("211", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> lls4Temperature = new ParsingValueDescriptor<>("213", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> lls5Temperature = new ParsingValueDescriptor<>("215", Integer.class, 1, buf -> (int) readByte(buf));

    // 1-byte unsigned for EYE sensors
    public static final ParsingValueDescriptor<Integer> eyeHumidity1 = new ParsingValueDescriptor<>("10804", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeHumidity2 = new ParsingValueDescriptor<>("10805", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeHumidity3 = new ParsingValueDescriptor<>("10806", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeHumidity4 = new ParsingValueDescriptor<>("10807", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeMovementState1 = new ParsingValueDescriptor<>("10812", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeMovementState2 = new ParsingValueDescriptor<>("10813", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeMovementState3 = new ParsingValueDescriptor<>("10814", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeMovementState4 = new ParsingValueDescriptor<>("10815", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeLowBatteryState1 = new ParsingValueDescriptor<>("10820", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeLowBatteryState2 = new ParsingValueDescriptor<>("10821", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeLowBatteryState3 = new ParsingValueDescriptor<>("10822", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyeLowBatteryState4 = new ParsingValueDescriptor<>("10823", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyePitch1UnsignedView = new ParsingValueDescriptor<>("10816", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyePitch2UnsignedView = new ParsingValueDescriptor<>("10817", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyePitch3UnsignedView = new ParsingValueDescriptor<>("10818", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> eyePitch4UnsignedView = new ParsingValueDescriptor<>("10819", Integer.class, 1, buf -> (int) buf.readUnsignedByte());

    // 2-byte unsigned integer values
    public static final ParsingValueDescriptor<Integer> speed = new ParsingValueDescriptor<>("24", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> gsmCellId = new ParsingValueDescriptor<>("205", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> gsmAreaCode = new ParsingValueDescriptor<>("206", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> lls1FuelLevel = new ParsingValueDescriptor<>("201", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> lls2FuelLevel = new ParsingValueDescriptor<>("203", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> lls3FuelLevel = new ParsingValueDescriptor<>("210", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> lls4FuelLevel = new ParsingValueDescriptor<>("212", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> lls5FuelLevel = new ParsingValueDescriptor<>("214", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> ainSpeed = new ParsingValueDescriptor<>("329", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeBatteryVoltage1 = new ParsingValueDescriptor<>("10824", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeBatteryVoltage2 = new ParsingValueDescriptor<>("10825", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeBatteryVoltage3 = new ParsingValueDescriptor<>("10826", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeBatteryVoltage4 = new ParsingValueDescriptor<>("10827", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeRoll1 = new ParsingValueDescriptor<>("10832", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> eyeRoll2 = new ParsingValueDescriptor<>("10833", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> eyeRoll3 = new ParsingValueDescriptor<>("10834", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> eyeRoll4 = new ParsingValueDescriptor<>("10835", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> eyeMovementCount1 = new ParsingValueDescriptor<>("10836", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMovementCount2 = new ParsingValueDescriptor<>("10837", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMovementCount3 = new ParsingValueDescriptor<>("10838", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMovementCount4 = new ParsingValueDescriptor<>("10839", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMagnetCount1 = new ParsingValueDescriptor<>("10840", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMagnetCount2 = new ParsingValueDescriptor<>("10841", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMagnetCount3 = new ParsingValueDescriptor<>("10842", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> eyeMagnetCount4 = new ParsingValueDescriptor<>("10843", Integer.class, 2, ByteBuf::readUnsignedShort);

    // 2-byte unsigned with multiplier (0.001)
    public static final ParsingValueDescriptor<Double> analogInput1 = new ParsingValueDescriptor<>("9", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final ParsingValueDescriptor<Double> analogInput2 = new ParsingValueDescriptor<>("6", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final ParsingValueDescriptor<Double> externalVoltage = new ParsingValueDescriptor<>("66", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final ParsingValueDescriptor<Double> batteryVoltage = new ParsingValueDescriptor<>("67", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final ParsingValueDescriptor<Double> batteryCurrent = new ParsingValueDescriptor<>("68", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);

    // 2-byte unsigned with multiplier (0.1)
    public static final ParsingValueDescriptor<Double> gnssPdop = new ParsingValueDescriptor<>("181", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> gnssHdop = new ParsingValueDescriptor<>("182", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> ul202SensorFuelLevel = new ParsingValueDescriptor<>("327", Double.class, 2, buf -> buf.readShort() * 0.1);
    public static final ParsingValueDescriptor<Double> frequencyDin1 = new ParsingValueDescriptor<>("622", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> frequencyDin2 = new ParsingValueDescriptor<>("623", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);

    // 2-byte unsigned with multiplier (0.01)
    public static final ParsingValueDescriptor<Double> fuelRateGps = new ParsingValueDescriptor<>("13", Double.class, 2, buf -> buf.readUnsignedShort() * 0.01);
    public static final ParsingValueDescriptor<Double> ecoScore = new ParsingValueDescriptor<>("15", Double.class, 2, buf -> buf.readUnsignedShort() * 0.01);
    public static final ParsingValueDescriptor<Double> eyeTemperature1 = new ParsingValueDescriptor<>("10800", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> eyeTemperature2 = new ParsingValueDescriptor<>("10801", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> eyeTemperature3 = new ParsingValueDescriptor<>("10802", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> eyeTemperature4 = new ParsingValueDescriptor<>("10803", Double.class, 2, buf -> buf.readShort() * 0.01);

    // 2-byte signed integer values
    public static final ParsingValueDescriptor<Integer> axisX = new ParsingValueDescriptor<>("17", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> axisY = new ParsingValueDescriptor<>("18", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> axisZ = new ParsingValueDescriptor<>("19", Integer.class, 2, buf -> (int) buf.readShort());

    // 4-byte unsigned integer values
    public static final ParsingValueDescriptor<Long> pulseCounterDin1 = new ParsingValueDescriptor<>("4", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> pulseCounterDin2 = new ParsingValueDescriptor<>("5", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> totalOdometer = new ParsingValueDescriptor<>("16", Integer.class, 4, ByteBuf::readInt);
    public static final ParsingValueDescriptor<Integer> tripOdometer = new ParsingValueDescriptor<>("199", Integer.class, 4, ByteBuf::readInt);
    public static final ParsingValueDescriptor<Long> activeGsmOperator = new ParsingValueDescriptor<>("241", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> driverCardId = new ParsingValueDescriptor<>("406", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> driverCardPlaceOfIssue = new ParsingValueDescriptor<>("408", Integer.class, 4, ByteBuf::readInt);
    public static final ParsingValueDescriptor<Long> umtsLteCellId = new ParsingValueDescriptor<>("636", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> connectivityQuality = new ParsingValueDescriptor<>("1148", Long.class, 4, ByteBuf::readUnsignedInt);

    // 4-byte unsigned with multiplier (0.001)
    public static final ParsingValueDescriptor<Double> fuelUsedGps = new ParsingValueDescriptor<>("12", Double.class, 4, buf -> buf.readUnsignedInt() * 0.001);

    // 4-byte signed with multiplier (0.1)
    public static final ParsingValueDescriptor<Double> dallasTemperature1 = new ParsingValueDescriptor<>("72", Double.class, 4, buf -> buf.readInt() * 0.1);
    public static final ParsingValueDescriptor<Double> dallasTemperature2 = new ParsingValueDescriptor<>("73", Double.class, 4, buf -> buf.readInt() * 0.1);
    public static final ParsingValueDescriptor<Double> dallasTemperature3 = new ParsingValueDescriptor<>("74", Double.class, 4, buf -> buf.readInt() * 0.1);
    public static final ParsingValueDescriptor<Double> dallasTemperature4 = new ParsingValueDescriptor<>("75", Double.class, 4, buf -> buf.readInt() * 0.1);

    // 8-byte values (strings/IDs) - use String.format to preserve leading zeros
    public static final ParsingValueDescriptor<String> iccid1 = new ParsingValueDescriptor<>("11", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> iccid2 = new ParsingValueDescriptor<>("14", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> dallasTemperatureId1 = new ParsingValueDescriptor<>("76", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> dallasTemperatureId2 = new ParsingValueDescriptor<>("77", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> dallasTemperatureId3 = new ParsingValueDescriptor<>("79", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> dallasTemperatureId4 = new ParsingValueDescriptor<>("71", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> iButton = new ParsingValueDescriptor<>("78", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> rfid = new ParsingValueDescriptor<>("207", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> userId = new ParsingValueDescriptor<>("238", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> bleRfid1 = new ParsingValueDescriptor<>("451", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> bleRfid2 = new ParsingValueDescriptor<>("452", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> bleRfid3 = new ParsingValueDescriptor<>("453", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> bleRfid4 = new ParsingValueDescriptor<>("454", String.class, 8, buf -> String.format("%016X", buf.readLong()));

    // Eventual I/O elements
    public static final ParsingValueDescriptor<Integer> trip = new ParsingValueDescriptor<>("250", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Long> ignitionOnCounter = new ParsingValueDescriptor<>("449", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Boolean> autoGeofence = new ParsingValueDescriptor<>("175", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> alarmEvent = new ParsingValueDescriptor<>("236", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> towing = new ParsingValueDescriptor<>("246", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> idling = new ParsingValueDescriptor<>("251", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> unplug = new ParsingValueDescriptor<>("252", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Integer> immobilizer = new ParsingValueDescriptor<>("248", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> crashDetection = new ParsingValueDescriptor<>("247", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> bloodAlcoholContent = new ParsingValueDescriptor<>("285", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> greenDrivingType = new ParsingValueDescriptor<>("253", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> greenDrivingValue = new ParsingValueDescriptor<>("254", Double.class, 1, buf -> (int) buf.readUnsignedByte() * 0.01);
    public static final ParsingValueDescriptor<Integer> greenDrivingEventDuration = new ParsingValueDescriptor<>("243", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<String> ecoMaximum = new ParsingValueDescriptor<>("258", String.class, 8, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<String> ecoAverage = new ParsingValueDescriptor<>("259", String.class, 8, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<Integer> ecoDuration = new ParsingValueDescriptor<>("260", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> drivingState = new ParsingValueDescriptor<>("283", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> drivingRecords = new ParsingValueDescriptor<>("284", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> crashEventCounter = new ParsingValueDescriptor<>("317", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> gnssJamming = new ParsingValueDescriptor<>("318", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Boolean> jamming = new ParsingValueDescriptor<>("249", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Integer> overSpeeding = new ParsingValueDescriptor<>("255", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<String> crashTraceData = new ParsingValueDescriptor<>("257", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<Boolean> privateMode = new ParsingValueDescriptor<>("391", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> motorcycleFallDetection = new ParsingValueDescriptor<>("1412", Boolean.class, 1, ByteBuf::readBoolean);
    // Geofence zone states (1 byte unsigned)
    public static final ParsingValueDescriptor<Integer> geofenceZone01 = geofenceZoneDescriptor(155);
    public static final ParsingValueDescriptor<Integer> geofenceZone02 = geofenceZoneDescriptor(156);
    public static final ParsingValueDescriptor<Integer> geofenceZone03 = geofenceZoneDescriptor(157);
    public static final ParsingValueDescriptor<Integer> geofenceZone04 = geofenceZoneDescriptor(158);
    public static final ParsingValueDescriptor<Integer> geofenceZone05 = geofenceZoneDescriptor(159);
    public static final ParsingValueDescriptor<Integer> geofenceZone06 = geofenceZoneDescriptor(61);
    public static final ParsingValueDescriptor<Integer> geofenceZone07 = geofenceZoneDescriptor(62);
    public static final ParsingValueDescriptor<Integer> geofenceZone08 = geofenceZoneDescriptor(63);
    public static final ParsingValueDescriptor<Integer> geofenceZone09 = geofenceZoneDescriptor(64);
    public static final ParsingValueDescriptor<Integer> geofenceZone10 = geofenceZoneDescriptor(65);
    public static final ParsingValueDescriptor<Integer> geofenceZone11 = geofenceZoneDescriptor(70);
    public static final ParsingValueDescriptor<Integer> geofenceZone12 = geofenceZoneDescriptor(88);
    public static final ParsingValueDescriptor<Integer> geofenceZone13 = geofenceZoneDescriptor(91);
    public static final ParsingValueDescriptor<Integer> geofenceZone14 = geofenceZoneDescriptor(92);
    public static final ParsingValueDescriptor<Integer> geofenceZone15 = geofenceZoneDescriptor(93);
    public static final ParsingValueDescriptor<Integer> geofenceZone16 = geofenceZoneDescriptor(94);
    public static final ParsingValueDescriptor<Integer> geofenceZone17 = geofenceZoneDescriptor(95);
    public static final ParsingValueDescriptor<Integer> geofenceZone18 = geofenceZoneDescriptor(96);
    public static final ParsingValueDescriptor<Integer> geofenceZone19 = geofenceZoneDescriptor(97);
    public static final ParsingValueDescriptor<Integer> geofenceZone20 = geofenceZoneDescriptor(98);
    public static final ParsingValueDescriptor<Integer> geofenceZone21 = geofenceZoneDescriptor(99);
    public static final ParsingValueDescriptor<Integer> geofenceZone22 = geofenceZoneDescriptor(153);
    public static final ParsingValueDescriptor<Integer> geofenceZone23 = geofenceZoneDescriptor(154);
    public static final ParsingValueDescriptor<Integer> geofenceZone24 = geofenceZoneDescriptor(190);
    public static final ParsingValueDescriptor<Integer> geofenceZone25 = geofenceZoneDescriptor(191);
    public static final ParsingValueDescriptor<Integer> geofenceZone26 = geofenceZoneDescriptor(192);
    public static final ParsingValueDescriptor<Integer> geofenceZone27 = geofenceZoneDescriptor(193);
    public static final ParsingValueDescriptor<Integer> geofenceZone28 = geofenceZoneDescriptor(194);
    public static final ParsingValueDescriptor<Integer> geofenceZone29 = geofenceZoneDescriptor(195);
    public static final ParsingValueDescriptor<Integer> geofenceZone30 = geofenceZoneDescriptor(196);
    public static final ParsingValueDescriptor<Integer> geofenceZone31 = geofenceZoneDescriptor(197);
    public static final ParsingValueDescriptor<Integer> geofenceZone32 = geofenceZoneDescriptor(198);
    public static final ParsingValueDescriptor<Integer> geofenceZone33 = geofenceZoneDescriptor(208);
    public static final ParsingValueDescriptor<Integer> geofenceZone34 = geofenceZoneDescriptor(209);
    public static final ParsingValueDescriptor<Integer> geofenceZone35 = geofenceZoneDescriptor(216);
    public static final ParsingValueDescriptor<Integer> geofenceZone36 = geofenceZoneDescriptor(217);
    public static final ParsingValueDescriptor<Integer> geofenceZone37 = geofenceZoneDescriptor(218);
    public static final ParsingValueDescriptor<Integer> geofenceZone38 = geofenceZoneDescriptor(219);
    public static final ParsingValueDescriptor<Integer> geofenceZone39 = geofenceZoneDescriptor(220);
    public static final ParsingValueDescriptor<Integer> geofenceZone40 = geofenceZoneDescriptor(221);
    public static final ParsingValueDescriptor<Integer> geofenceZone41 = geofenceZoneDescriptor(222);
    public static final ParsingValueDescriptor<Integer> geofenceZone42 = geofenceZoneDescriptor(223);
    public static final ParsingValueDescriptor<Integer> geofenceZone43 = geofenceZoneDescriptor(224);
    public static final ParsingValueDescriptor<Integer> geofenceZone44 = geofenceZoneDescriptor(225);
    public static final ParsingValueDescriptor<Integer> geofenceZone45 = geofenceZoneDescriptor(226);
    public static final ParsingValueDescriptor<Integer> geofenceZone46 = geofenceZoneDescriptor(227);
    public static final ParsingValueDescriptor<Integer> geofenceZone47 = geofenceZoneDescriptor(228);
    public static final ParsingValueDescriptor<Integer> geofenceZone48 = geofenceZoneDescriptor(229);
    public static final ParsingValueDescriptor<Integer> geofenceZone49 = geofenceZoneDescriptor(230);
    public static final ParsingValueDescriptor<Integer> geofenceZone50 = geofenceZoneDescriptor(231);
    // BLE button states (1 byte)
    public static final ParsingValueDescriptor<Boolean> bleButton1State1 = new ParsingValueDescriptor<>("455", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton1State2 = new ParsingValueDescriptor<>("456", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton1State3 = new ParsingValueDescriptor<>("457", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton1State4 = new ParsingValueDescriptor<>("458", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton2State1 = new ParsingValueDescriptor<>("459", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton2State2 = new ParsingValueDescriptor<>("460", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton2State3 = new ParsingValueDescriptor<>("461", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> bleButton2State4 = new ParsingValueDescriptor<>("462", Boolean.class, 1, ByteBuf::readBoolean);
    // BLE environmental sensors and telemetry
    public static final ParsingValueDescriptor<Double> bleTemperature1 = new ParsingValueDescriptor<>("25", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> bleTemperature2 = new ParsingValueDescriptor<>("26", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> bleTemperature3 = new ParsingValueDescriptor<>("27", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> bleTemperature4 = new ParsingValueDescriptor<>("28", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Integer> bleBattery1 = new ParsingValueDescriptor<>("29", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> bleBattery2 = new ParsingValueDescriptor<>("20", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> bleBattery3 = new ParsingValueDescriptor<>("22", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> bleBattery4 = new ParsingValueDescriptor<>("23", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> bleHumidity1 = new ParsingValueDescriptor<>("86", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> bleHumidity2 = new ParsingValueDescriptor<>("104", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> bleHumidity3 = new ParsingValueDescriptor<>("106", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> bleHumidity4 = new ParsingValueDescriptor<>("108", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Integer> bleFuelLevel1 = new ParsingValueDescriptor<>("270", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> bleFuelLevel2 = new ParsingValueDescriptor<>("273", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> bleFuelLevel3 = new ParsingValueDescriptor<>("276", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> bleFuelLevel4 = new ParsingValueDescriptor<>("279", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> bleFuelFrequency1 = new ParsingValueDescriptor<>("306", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleFuelFrequency2 = new ParsingValueDescriptor<>("307", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleFuelFrequency3 = new ParsingValueDescriptor<>("308", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleFuelFrequency4 = new ParsingValueDescriptor<>("309", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> bleLuminosity1 = new ParsingValueDescriptor<>("335", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> bleLuminosity2 = new ParsingValueDescriptor<>("336", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> bleLuminosity3 = new ParsingValueDescriptor<>("337", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> bleLuminosity4 = new ParsingValueDescriptor<>("338", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<String> bleSensor1Custom1 = new ParsingValueDescriptor<>("331", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<String> bleSensor2Custom1 = new ParsingValueDescriptor<>("332", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<String> bleSensor3Custom1 = new ParsingValueDescriptor<>("333", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<String> bleSensor4Custom1 = new ParsingValueDescriptor<>("334", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<Long> bleSensor1Custom2 = new ParsingValueDescriptor<>("463", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor1Custom3 = new ParsingValueDescriptor<>("464", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor1Custom4 = new ParsingValueDescriptor<>("465", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor1Custom5 = new ParsingValueDescriptor<>("466", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor2Custom2 = new ParsingValueDescriptor<>("467", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor2Custom3 = new ParsingValueDescriptor<>("468", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor2Custom4 = new ParsingValueDescriptor<>("469", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor2Custom5 = new ParsingValueDescriptor<>("470", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor3Custom2 = new ParsingValueDescriptor<>("471", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor3Custom3 = new ParsingValueDescriptor<>("472", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor3Custom4 = new ParsingValueDescriptor<>("473", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor3Custom5 = new ParsingValueDescriptor<>("474", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor4Custom2 = new ParsingValueDescriptor<>("475", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor4Custom3 = new ParsingValueDescriptor<>("476", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor4Custom4 = new ParsingValueDescriptor<>("477", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> bleSensor4Custom5 = new ParsingValueDescriptor<>("478", Long.class, 4, ByteBuf::readUnsignedInt);

    // Beacon / advanced BLE payloads
    public static final ParsingValueDescriptor<String> beaconList = new ParsingValueDescriptor<>("385", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);
    public static final ParsingValueDescriptor<String> advancedBleBeaconData = new ParsingValueDescriptor<>("548", String.class, -1, TeltonikaValueDescriptors::readRemainingAsHex);

    // Variable length strings
    public static final ParsingValueDescriptor<String> barcodeId = new ParsingValueDescriptor<>("264", String.class, -1, buf -> readString(buf, Math.min(32, buf.readableBytes())));
    public static final ParsingValueDescriptor<GeoJSONPoint> iso6709Coordinates = new ParsingValueDescriptor<>("387", GeoJSONPoint.class, 34, TeltonikaValueDescriptors::parseIso6709);
    public static final ParsingValueDescriptor<String> driverName = new ParsingValueDescriptor<>("403", String.class, 35, buf -> readString(buf, 35).trim());
    public static final ParsingValueDescriptor<String> msp500VendorName = new ParsingValueDescriptor<>("500", String.class, 40, buf -> readString(buf, 40).trim());
    public static final ParsingValueDescriptor<String> msp500VehicleNumber = new ParsingValueDescriptor<>("501", String.class, 40, buf -> readString(buf, 40).trim());
    public static final ParsingValueDescriptor<String> vin = new ParsingValueDescriptor<>("256", String.class, 17, buf -> readString(buf, 17).trim());

    // OBD elements (standard PIDs)
    public static final ParsingValueDescriptor<Integer> obdNumberOfDtc = new ParsingValueDescriptor<>("30", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdEngineLoad = new ParsingValueDescriptor<>("31", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdCoolantTemperature = new ParsingValueDescriptor<>("32", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> obdShortFuelTrim = new ParsingValueDescriptor<>("33", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> obdFuelPressure = new ParsingValueDescriptor<>("34", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdIntakeMap = new ParsingValueDescriptor<>("35", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdEngineRpm = new ParsingValueDescriptor<>("36", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdVehicleSpeed = new ParsingValueDescriptor<>("37", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdTimingAdvance = new ParsingValueDescriptor<>("38", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> obdIntakeAirTemperature = new ParsingValueDescriptor<>("39", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Double> obdMaf = new ParsingValueDescriptor<>("40", Double.class, 2, buf -> buf.readUnsignedShort() * 0.01);
    public static final ParsingValueDescriptor<Integer> obdThrottlePosition = new ParsingValueDescriptor<>("41", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdRuntimeSinceStart = new ParsingValueDescriptor<>("42", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdDistanceMilOn = new ParsingValueDescriptor<>("43", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Double> obdRelativeFuelRailPressure = new ParsingValueDescriptor<>("44", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Double> obdDirectFuelRailPressure = new ParsingValueDescriptor<>("45", Double.class, 2, buf -> buf.readUnsignedShort() * 10.0);
    public static final ParsingValueDescriptor<Integer> obdCommandedEgr = new ParsingValueDescriptor<>("46", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdEgrError = new ParsingValueDescriptor<>("47", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> obdFuelLevel = new ParsingValueDescriptor<>("48", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdDistanceSinceCodesCleared = new ParsingValueDescriptor<>("49", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdBarometricPressure = new ParsingValueDescriptor<>("50", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> obdControlModuleVoltage = new ParsingValueDescriptor<>("51", Double.class, 2, buf -> buf.readUnsignedShort() * 0.001);
    public static final ParsingValueDescriptor<Integer> obdAbsoluteLoadValue = new ParsingValueDescriptor<>("52", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdFuelType = new ParsingValueDescriptor<>("759", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdAmbientAirTemperature = new ParsingValueDescriptor<>("53", Integer.class, 1, buf -> (int) readByte(buf));
    public static final ParsingValueDescriptor<Integer> obdTimeRunWithMilOn = new ParsingValueDescriptor<>("54", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdTimeSinceCodesCleared = new ParsingValueDescriptor<>("55", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Double> obdAbsoluteFuelRailPressure = new ParsingValueDescriptor<>("56", Double.class, 2, buf -> buf.readUnsignedShort() * 10.0);
    public static final ParsingValueDescriptor<Integer> obdHybridBatteryPackLife = new ParsingValueDescriptor<>("57", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdEngineOilTemperature = new ParsingValueDescriptor<>("58", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> obdFuelInjectionTiming = new ParsingValueDescriptor<>("59", Double.class, 2, buf -> buf.readShort() * 0.01);
    public static final ParsingValueDescriptor<Double> obdFuelRate = new ParsingValueDescriptor<>("60", Double.class, 2, buf -> buf.readUnsignedShort() * 0.01);
    public static final ParsingValueDescriptor<Integer> obdThrottlePositionGroup = new ParsingValueDescriptor<>("540", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> obdCommandedEquivalenceRatio = new ParsingValueDescriptor<>("541", Double.class, 1, buf -> (int) buf.readUnsignedByte() * 0.01);
    public static final ParsingValueDescriptor<Integer> obdIntakeMapExtended = new ParsingValueDescriptor<>("542", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdHybridSystemVoltage = new ParsingValueDescriptor<>("543", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdHybridSystemCurrent = new ParsingValueDescriptor<>("544", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<String> obdFaultCodes = new ParsingValueDescriptor<>("281", String.class, -1, buf -> {
        byte[] bytes = new byte[Math.min(128, buf.readableBytes())];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    });

    // OBD OEM elements
    public static final ParsingValueDescriptor<Long> obdOemTotalMileage = new ParsingValueDescriptor<>("389", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Double> obdOemFuelLevelLitres = new ParsingValueDescriptor<>("390", Double.class, 4, buf -> buf.readUnsignedInt() * 0.1);
    public static final ParsingValueDescriptor<Long> obdOemDistanceUntilService = new ParsingValueDescriptor<>("402", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> obdOemBatteryChargeLevel = new ParsingValueDescriptor<>("411", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> obdOemRemainingDistance = new ParsingValueDescriptor<>("755", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdOemBatteryStateOfHealth = new ParsingValueDescriptor<>("1151", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> obdOemBatteryTemperature = new ParsingValueDescriptor<>("1152", Integer.class, 2, buf -> (int) buf.readShort());

    // CAN adapter telemetry (LVCAN/ALLCAN/CANCONTROL)
    public static final ParsingValueDescriptor<Integer> canVehicleSpeed = new ParsingValueDescriptor<>("81", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canAcceleratorPedalPosition = new ParsingValueDescriptor<>("82", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> canFuelConsumed = new ParsingValueDescriptor<>("83", Double.class, 4, buf -> buf.readUnsignedInt() * 0.1);
    public static final ParsingValueDescriptor<Double> canFuelLevelLitres = new ParsingValueDescriptor<>("84", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Integer> canEngineRpm = new ParsingValueDescriptor<>("85", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> canTotalMileage = new ParsingValueDescriptor<>("87", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canFuelLevelPercent = new ParsingValueDescriptor<>("89", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canDoorStatus = new ParsingValueDescriptor<>("90", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> canProgramNumber = new ParsingValueDescriptor<>("100", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<String> canModuleId8 = new ParsingValueDescriptor<>("101", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canModuleId17 = new ParsingValueDescriptor<>("388", String.class, 17, buf -> readFixedHex(buf, 17));
    public static final ParsingValueDescriptor<Long> canEngineWorktime = new ParsingValueDescriptor<>("102", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canEngineWorktimeCounted = new ParsingValueDescriptor<>("103", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canTotalMileageCounted = new ParsingValueDescriptor<>("105", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Double> canFuelConsumedCounted = new ParsingValueDescriptor<>("107", Double.class, 4, buf -> buf.readUnsignedInt() * 0.1);
    public static final ParsingValueDescriptor<Double> canFuelRate = new ParsingValueDescriptor<>("110", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Integer> canAdBlueLevelPercent = new ParsingValueDescriptor<>("111", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> canAdBlueLevelLitres = new ParsingValueDescriptor<>("112", Double.class, 2, buf -> buf.readUnsignedShort() * 0.1);
    public static final ParsingValueDescriptor<Integer> canEngineLoadPercent = new ParsingValueDescriptor<>("114", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Double> canEngineTemperature = new ParsingValueDescriptor<>("115", Double.class, 2, buf -> buf.readShort() * 0.1);
    public static final ParsingValueDescriptor<Integer> canAxle1Load = new ParsingValueDescriptor<>("118", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canAxle2Load = new ParsingValueDescriptor<>("119", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canAxle3Load = new ParsingValueDescriptor<>("120", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canAxle4Load = new ParsingValueDescriptor<>("121", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canAxle5Load = new ParsingValueDescriptor<>("122", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> canControlStateFlags = new ParsingValueDescriptor<>("123", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<String> canAgriculturalMachineryFlags = new ParsingValueDescriptor<>("124", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<Long> canHarvestingTime = new ParsingValueDescriptor<>("125", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canAreaOfHarvest = new ParsingValueDescriptor<>("126", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canMowingEfficiency = new ParsingValueDescriptor<>("127", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canGrainMownVolume = new ParsingValueDescriptor<>("128", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canGrainMoisture = new ParsingValueDescriptor<>("129", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canHarvestingDrumRpm = new ParsingValueDescriptor<>("130", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canGapUnderHarvestingDrum = new ParsingValueDescriptor<>("131", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<String> canSecurityStateFlags = new ParsingValueDescriptor<>("132", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<Long> canTachographTotalDistance = new ParsingValueDescriptor<>("133", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canTripDistance = new ParsingValueDescriptor<>("134", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canTachographVehicleSpeed = new ParsingValueDescriptor<>("135", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canTachographDriverCardPresence = new ParsingValueDescriptor<>("136", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canDriver1States = new ParsingValueDescriptor<>("137", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canDriver2States = new ParsingValueDescriptor<>("138", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canDriver1ContinuousDrivingTime = new ParsingValueDescriptor<>("139", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver2ContinuousDrivingTime = new ParsingValueDescriptor<>("140", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver1CumulativeBreakTime = new ParsingValueDescriptor<>("141", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver2CumulativeBreakTime = new ParsingValueDescriptor<>("142", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver1SelectedActivityDuration = new ParsingValueDescriptor<>("143", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver2SelectedActivityDuration = new ParsingValueDescriptor<>("144", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver1CumulativeDrivingTime = new ParsingValueDescriptor<>("145", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDriver2CumulativeDrivingTime = new ParsingValueDescriptor<>("146", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<String> canDriver1IdHigh = new ParsingValueDescriptor<>("147", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canDriver1IdLow = new ParsingValueDescriptor<>("148", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canDriver2IdHigh = new ParsingValueDescriptor<>("149", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canDriver2IdLow = new ParsingValueDescriptor<>("150", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<Double> canBatteryTemperature = new ParsingValueDescriptor<>("151", Double.class, 2, buf -> buf.readShort() * 0.1);
    public static final ParsingValueDescriptor<Integer> canHvBatteryLevelPercent = new ParsingValueDescriptor<>("152", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canDtcFaults = new ParsingValueDescriptor<>("160", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> canSlopeOfArm = new ParsingValueDescriptor<>("161", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> canRotationOfArm = new ParsingValueDescriptor<>("162", Integer.class, 2, buf -> (int) buf.readShort());
    public static final ParsingValueDescriptor<Integer> canEjectOfArm = new ParsingValueDescriptor<>("163", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canHorizontalDistanceArmVehicle = new ParsingValueDescriptor<>("164", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canHeightArmAboveGround = new ParsingValueDescriptor<>("165", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canDrillRpm = new ParsingValueDescriptor<>("166", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canSpreadSaltPerSquareMeter = new ParsingValueDescriptor<>("167", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canBatteryVoltageRaw = new ParsingValueDescriptor<>("168", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> canSpreadFineGrainedSalt = new ParsingValueDescriptor<>("169", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadCoarseGrainedSalt = new ParsingValueDescriptor<>("170", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadDiMix = new ParsingValueDescriptor<>("171", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadCoarseGrainedCalcium = new ParsingValueDescriptor<>("172", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadCalciumChloride = new ParsingValueDescriptor<>("173", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadSodiumChloride = new ParsingValueDescriptor<>("174", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadMagnesiumChloride = new ParsingValueDescriptor<>("176", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadGravel = new ParsingValueDescriptor<>("177", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canSpreadSand = new ParsingValueDescriptor<>("178", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canWidthPouringLeft = new ParsingValueDescriptor<>("183", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canWidthPouringRight = new ParsingValueDescriptor<>("184", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> canSaltSpreaderWorkingHours = new ParsingValueDescriptor<>("185", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canDistanceDuringSalting = new ParsingValueDescriptor<>("186", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canLoadWeight = new ParsingValueDescriptor<>("187", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canRetarderLoad = new ParsingValueDescriptor<>("188", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Long> canCruiseTime = new ParsingValueDescriptor<>("189", Long.class, 4, ByteBuf::readUnsignedInt);

    // CAN adapter fuel / gas / OEM flags
    public static final ParsingValueDescriptor<Boolean> canCngStatus = new ParsingValueDescriptor<>("232", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Long> canCngUsed = new ParsingValueDescriptor<>("233", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canCngLevelPercent = new ParsingValueDescriptor<>("234", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Boolean> canOilLevelIndicator = new ParsingValueDescriptor<>("235", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Long> canVehicleRangeOnBattery = new ParsingValueDescriptor<>("304", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canVehicleRangeOnAdditionalFuel = new ParsingValueDescriptor<>("305", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<String> canVinNumber = new ParsingValueDescriptor<>("325", String.class, 17, buf -> readString(buf, 17).trim());
    public static final ParsingValueDescriptor<String> canFaultCodes = new ParsingValueDescriptor<>("282", String.class, -1, buf -> {
        byte[] bytes = new byte[Math.min(128, buf.readableBytes())];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    });
    public static final ParsingValueDescriptor<String> canSecurityStateFlagsP4 = new ParsingValueDescriptor<>("517", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canControlStateFlagsP4 = new ParsingValueDescriptor<>("518", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canIndicatorStateFlagsP4 = new ParsingValueDescriptor<>("519", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canAgriculturalStateFlagsP4 = new ParsingValueDescriptor<>("520", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canUtilityStateFlagsP4 = new ParsingValueDescriptor<>("521", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<String> canCisternStateFlagsP4 = new ParsingValueDescriptor<>("522", String.class, 8, buf -> String.format("%016X", buf.readLong()));
    public static final ParsingValueDescriptor<Long> canLngUsed = new ParsingValueDescriptor<>("855", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canLngUsedCounted = new ParsingValueDescriptor<>("856", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canLngLevelPercent = new ParsingValueDescriptor<>("857", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canLngLevelKg = new ParsingValueDescriptor<>("858", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> canTotalLpgUsed = new ParsingValueDescriptor<>("1100", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> canTotalLpgUsedCounted = new ParsingValueDescriptor<>("1101", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> canLpgLevelPercent = new ParsingValueDescriptor<>("1102", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> canLpgLevelLitres = new ParsingValueDescriptor<>("1103", Integer.class, 2, ByteBuf::readUnsignedShort);

    // SSF / CSF / ISF / ASF / USF / CiSF flags
    public static final ParsingValueDescriptor<Boolean> ssfIgnition = new ParsingValueDescriptor<>("898", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfKeyInIgnitionLock = new ParsingValueDescriptor<>("652", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfWebasto = new ParsingValueDescriptor<>("899", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfEngineWorking = new ParsingValueDescriptor<>("900", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfStandaloneEngine = new ParsingValueDescriptor<>("901", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfReadyToDrive = new ParsingValueDescriptor<>("902", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfEngineWorkingOnCng = new ParsingValueDescriptor<>("903", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfWorkModeCompany = new ParsingValueDescriptor<>("904", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfOperatorPresent = new ParsingValueDescriptor<>("905", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfInterlock = new ParsingValueDescriptor<>("906", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfEngineLockActive = new ParsingValueDescriptor<>("907", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfRequestToLockEngine = new ParsingValueDescriptor<>("908", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfHandbrakeActive = new ParsingValueDescriptor<>("653", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFootbrakeActive = new ParsingValueDescriptor<>("910", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfClutchPushed = new ParsingValueDescriptor<>("911", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfHazardWarningLights = new ParsingValueDescriptor<>("912", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFrontLeftDoorOpen = new ParsingValueDescriptor<>("654", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFrontRightDoorOpen = new ParsingValueDescriptor<>("655", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfRearLeftDoorOpen = new ParsingValueDescriptor<>("656", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfRearRightDoorOpen = new ParsingValueDescriptor<>("657", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfTrunkDoorOpen = new ParsingValueDescriptor<>("658", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfEngineCoverOpen = new ParsingValueDescriptor<>("913", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfRoofOpen = new ParsingValueDescriptor<>("909", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfChargingWirePlugged = new ParsingValueDescriptor<>("914", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfBatteryCharging = new ParsingValueDescriptor<>("915", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfElectricEngineState = new ParsingValueDescriptor<>("916", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfCarClosedFactoryRemote = new ParsingValueDescriptor<>("917", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfCarClosed = new ParsingValueDescriptor<>("662", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFactoryAlarmActuated = new ParsingValueDescriptor<>("918", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFactoryAlarmEmulated = new ParsingValueDescriptor<>("919", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfSignalCloseFactoryRemote = new ParsingValueDescriptor<>("920", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfSignalOpenFactoryRemote = new ParsingValueDescriptor<>("921", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfRearmingSignal = new ParsingValueDescriptor<>("922", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfTrunkDoorOpenedFactoryRemote = new ParsingValueDescriptor<>("923", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfCanModuleInSleep = new ParsingValueDescriptor<>("924", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFactoryRemoteTriple = new ParsingValueDescriptor<>("925", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfFactoryArmed = new ParsingValueDescriptor<>("926", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfParkingGearActive = new ParsingValueDescriptor<>("660", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfReverseGearActive = new ParsingValueDescriptor<>("661", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfNeutralGearActive = new ParsingValueDescriptor<>("659", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfDriveActive = new ParsingValueDescriptor<>("927", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfEngineWorkingOnDualFuel = new ParsingValueDescriptor<>("1083", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> ssfEngineWorkingOnLpg = new ParsingValueDescriptor<>("1084", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Boolean> csfParkingLights = new ParsingValueDescriptor<>("928", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfDippedHeadlights = new ParsingValueDescriptor<>("929", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfFullBeamHeadlights = new ParsingValueDescriptor<>("930", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfRearFogLights = new ParsingValueDescriptor<>("931", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfFrontFogLights = new ParsingValueDescriptor<>("932", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfAdditionalFrontLights = new ParsingValueDescriptor<>("933", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfAdditionalRearLights = new ParsingValueDescriptor<>("934", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfLightSignal = new ParsingValueDescriptor<>("935", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfAirConditioning = new ParsingValueDescriptor<>("936", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfCruiseControl = new ParsingValueDescriptor<>("937", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfAutomaticRetarder = new ParsingValueDescriptor<>("938", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfManualRetarder = new ParsingValueDescriptor<>("939", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfDriversSeatbeltFastened = new ParsingValueDescriptor<>("940", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfFrontDriversSeatbeltFastened = new ParsingValueDescriptor<>("941", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfLeftDriversSeatbeltFastened = new ParsingValueDescriptor<>("942", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfRightDriversSeatbeltFastened = new ParsingValueDescriptor<>("943", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfCentreDriversSeatbeltFastened = new ParsingValueDescriptor<>("944", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfFrontPassengerPresent = new ParsingValueDescriptor<>("945", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfPto = new ParsingValueDescriptor<>("946", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfFrontDifferentialLocked = new ParsingValueDescriptor<>("947", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfRearDifferentialLocked = new ParsingValueDescriptor<>("948", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfCentralDifferential4HiLocked = new ParsingValueDescriptor<>("949", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfRearDifferential4LoLocked = new ParsingValueDescriptor<>("950", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfTrailerAxle1LiftActive = new ParsingValueDescriptor<>("951", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfTrailerAxle2LiftActive = new ParsingValueDescriptor<>("952", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfTrailerConnected = new ParsingValueDescriptor<>("1085", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> csfStartStopSystemInactive = new ParsingValueDescriptor<>("1086", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Boolean> isfCheckEngineIndicator = new ParsingValueDescriptor<>("953", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfAbsIndicator = new ParsingValueDescriptor<>("954", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfEspIndicator = new ParsingValueDescriptor<>("955", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfEspTurnedOff = new ParsingValueDescriptor<>("956", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfStopIndicator = new ParsingValueDescriptor<>("957", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfOilLevelIndicator = new ParsingValueDescriptor<>("958", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfCoolantLiquidLevel = new ParsingValueDescriptor<>("959", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfBatteryNotCharging = new ParsingValueDescriptor<>("960", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfHandbrakeSystemIndicator = new ParsingValueDescriptor<>("961", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfAirbagIndicator = new ParsingValueDescriptor<>("962", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfEpsIndicator = new ParsingValueDescriptor<>("963", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfWarningIndicator = new ParsingValueDescriptor<>("964", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLightsFailureIndicator = new ParsingValueDescriptor<>("965", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowTyrePressureIndicator = new ParsingValueDescriptor<>("966", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfWearOfBrakePads = new ParsingValueDescriptor<>("967", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowFuelLevelIndicator = new ParsingValueDescriptor<>("968", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfMaintenanceRequired = new ParsingValueDescriptor<>("969", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfGlowPlugIndicator = new ParsingValueDescriptor<>("970", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfFapIndicator = new ParsingValueDescriptor<>("971", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfEpcIndicator = new ParsingValueDescriptor<>("972", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfCloggedEngineOilFilter = new ParsingValueDescriptor<>("973", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowEngineOilPressure = new ParsingValueDescriptor<>("974", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfHighEngineOilTemperature = new ParsingValueDescriptor<>("975", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowCoolantLevel = new ParsingValueDescriptor<>("976", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfCloggedHydraulicOilFilter = new ParsingValueDescriptor<>("977", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfHydraulicSystemLowPressure = new ParsingValueDescriptor<>("978", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfHydraulicOilLowLevel = new ParsingValueDescriptor<>("979", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfHydraulicSystemHighTemperature = new ParsingValueDescriptor<>("980", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfOilOverflowHydraulicChamber = new ParsingValueDescriptor<>("981", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfCloggedAirFilter = new ParsingValueDescriptor<>("982", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfCloggedFuelFilter = new ParsingValueDescriptor<>("983", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfWaterInFuelIndicator = new ParsingValueDescriptor<>("984", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfCloggedBrakeSystemFilter = new ParsingValueDescriptor<>("985", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowWasherFluidLevel = new ParsingValueDescriptor<>("986", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowAdBlueLevel = new ParsingValueDescriptor<>("987", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowTrailerTyrePressure = new ParsingValueDescriptor<>("988", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfWearOfTrailerBrakeLining = new ParsingValueDescriptor<>("989", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfHighTrailerBrakeTemperature = new ParsingValueDescriptor<>("990", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfIncorrectTrailerPneumaticSupply = new ParsingValueDescriptor<>("991", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> isfLowCngLevelIndicator = new ParsingValueDescriptor<>("992", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Boolean> asfRightJoystickMovedRight = new ParsingValueDescriptor<>("993", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfRightJoystickMovedLeft = new ParsingValueDescriptor<>("994", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfRightJoystickMovedForward = new ParsingValueDescriptor<>("995", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfRightJoystickMovedBack = new ParsingValueDescriptor<>("996", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfLeftJoystickMovedRight = new ParsingValueDescriptor<>("997", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfLeftJoystickMovedLeft = new ParsingValueDescriptor<>("998", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfLeftJoystickMovedForward = new ParsingValueDescriptor<>("999", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfLeftJoystickMovedBack = new ParsingValueDescriptor<>("1000", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFirstRearHydraulic = new ParsingValueDescriptor<>("1001", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSecondRearHydraulic = new ParsingValueDescriptor<>("1002", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfThirdRearHydraulic = new ParsingValueDescriptor<>("1003", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFourthRearHydraulic = new ParsingValueDescriptor<>("1004", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFirstFrontHydraulic = new ParsingValueDescriptor<>("1005", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSecondFrontHydraulic = new ParsingValueDescriptor<>("1006", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfThirdFrontHydraulic = new ParsingValueDescriptor<>("1007", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFourthFrontHydraulic = new ParsingValueDescriptor<>("1008", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFrontThreePointHitch = new ParsingValueDescriptor<>("1009", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfRearThreePointHitch = new ParsingValueDescriptor<>("1010", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFrontPowerTakeOff = new ParsingValueDescriptor<>("1011", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfRearPowerTakeOff = new ParsingValueDescriptor<>("1012", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfMowingActive = new ParsingValueDescriptor<>("1013", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfThreshingActive = new ParsingValueDescriptor<>("1014", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfGrainReleaseFromHopper = new ParsingValueDescriptor<>("1015", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfGrainTankFull = new ParsingValueDescriptor<>("1016", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfGrainTankSeventyFull = new ParsingValueDescriptor<>("1017", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfGrainTankOpened = new ParsingValueDescriptor<>("1018", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfUnloaderDrive = new ParsingValueDescriptor<>("1019", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfCleaningFanControlOff = new ParsingValueDescriptor<>("1020", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfThreshingDrumControlOff = new ParsingValueDescriptor<>("1021", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfStrawWalkerClogged = new ParsingValueDescriptor<>("1022", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfExcessiveClearanceUnderDrum = new ParsingValueDescriptor<>("1023", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfLowDriveHydraulicsTemperature = new ParsingValueDescriptor<>("1024", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfHighDriveHydraulicsTemperature = new ParsingValueDescriptor<>("1025", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfEarAugerSpeedBelowNorm = new ParsingValueDescriptor<>("1026", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfGrainAugerSpeedBelowNorm = new ParsingValueDescriptor<>("1027", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfStrawChopperSpeedBelowNorm = new ParsingValueDescriptor<>("1028", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfStrawShakerSpeedBelowNorm = new ParsingValueDescriptor<>("1029", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFeederSpeedBelowNorm = new ParsingValueDescriptor<>("1030", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfStrawChopperOn = new ParsingValueDescriptor<>("1031", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfCornHeaderConnected = new ParsingValueDescriptor<>("1032", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfGrainHeaderConnected = new ParsingValueDescriptor<>("1033", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfFeederReverseOn = new ParsingValueDescriptor<>("1034", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfPressureFilterHydraulicPumpClogged = new ParsingValueDescriptor<>("1035", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfAdapterPressureFilterSensor = new ParsingValueDescriptor<>("1087", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfService2RequiredIndicator = new ParsingValueDescriptor<>("1088", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfDrainFilterCloggedIndicator = new ParsingValueDescriptor<>("1089", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection1Spraying = new ParsingValueDescriptor<>("1090", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection2Spraying = new ParsingValueDescriptor<>("1091", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection3Spraying = new ParsingValueDescriptor<>("1092", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection4Spraying = new ParsingValueDescriptor<>("1093", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection5Spraying = new ParsingValueDescriptor<>("1094", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection6Spraying = new ParsingValueDescriptor<>("1095", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection7Spraying = new ParsingValueDescriptor<>("1096", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection8Spraying = new ParsingValueDescriptor<>("1097", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> asfSection9Spraying = new ParsingValueDescriptor<>("1098", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Boolean> usfSpreading = new ParsingValueDescriptor<>("1036", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfPouringChemicals = new ParsingValueDescriptor<>("1037", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfConveyorBelt = new ParsingValueDescriptor<>("1038", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfDriveWheel = new ParsingValueDescriptor<>("1039", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfBrushes = new ParsingValueDescriptor<>("1040", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfVacuumCleaner = new ParsingValueDescriptor<>("1041", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfWaterSupply = new ParsingValueDescriptor<>("1042", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfSpreadingSecondary = new ParsingValueDescriptor<>("1043", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfLiquidPump = new ParsingValueDescriptor<>("1044", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfUnloadingFromHopper = new ParsingValueDescriptor<>("1045", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfLowSaltLevel = new ParsingValueDescriptor<>("1046", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfLowWaterLevel = new ParsingValueDescriptor<>("1047", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfChemicals = new ParsingValueDescriptor<>("1048", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfCompressor = new ParsingValueDescriptor<>("1049", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfWaterValveOpened = new ParsingValueDescriptor<>("1050", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfCabinMovedUp = new ParsingValueDescriptor<>("1051", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfCabinMovedDown = new ParsingValueDescriptor<>("1052", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> usfHydraulicsWorkNotPermitted = new ParsingValueDescriptor<>("1099", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Boolean> cisfSection1FluidInDownpipe = new ParsingValueDescriptor<>("1053", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection1Filled = new ParsingValueDescriptor<>("1054", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection1Overfilled = new ParsingValueDescriptor<>("1055", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection2FluidInDownpipe = new ParsingValueDescriptor<>("1056", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection2Filled = new ParsingValueDescriptor<>("1057", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection2Overfilled = new ParsingValueDescriptor<>("1058", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection3FluidInDownpipe = new ParsingValueDescriptor<>("1059", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection3Filled = new ParsingValueDescriptor<>("1060", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection3Overfilled = new ParsingValueDescriptor<>("1061", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection4FluidInDownpipe = new ParsingValueDescriptor<>("1062", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection4Filled = new ParsingValueDescriptor<>("1063", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection4Overfilled = new ParsingValueDescriptor<>("1064", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection5FluidInDownpipe = new ParsingValueDescriptor<>("1065", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection5Filled = new ParsingValueDescriptor<>("1066", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection5Overfilled = new ParsingValueDescriptor<>("1067", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection6FluidInDownpipe = new ParsingValueDescriptor<>("1068", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection6Filled = new ParsingValueDescriptor<>("1069", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection6Overfilled = new ParsingValueDescriptor<>("1070", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection7FluidInDownpipe = new ParsingValueDescriptor<>("1071", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection7Filled = new ParsingValueDescriptor<>("1072", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection7Overfilled = new ParsingValueDescriptor<>("1073", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection8FluidInDownpipe = new ParsingValueDescriptor<>("1074", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection8Filled = new ParsingValueDescriptor<>("1075", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> cisfSection8Overfilled = new ParsingValueDescriptor<>("1076", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Long> distanceToNextService = new ParsingValueDescriptor<>("400", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Double> cngLevelKg = new ParsingValueDescriptor<>("450", Double.class, 2, buf -> buf.readUnsignedShort() * 10.0);
    public static final ParsingValueDescriptor<Long> distanceFromNeedOfService = new ParsingValueDescriptor<>("859", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> distanceFromLastService = new ParsingValueDescriptor<>("860", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> timeToNextService = new ParsingValueDescriptor<>("861", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> timeFromNeedOfService = new ParsingValueDescriptor<>("862", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Integer> timeFromLastService = new ParsingValueDescriptor<>("863", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> distanceToNextOilService = new ParsingValueDescriptor<>("864", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> timeToNextOilService = new ParsingValueDescriptor<>("865", Integer.class, 2, ByteBuf::readUnsignedShort);
    public static final ParsingValueDescriptor<Long> lvcanVehicleRange = new ParsingValueDescriptor<>("866", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> lvcanTotalCngCounted = new ParsingValueDescriptor<>("867", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> totalBaleCount = new ParsingValueDescriptor<>("1079", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> baleCount = new ParsingValueDescriptor<>("1080", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> cutBaleCount = new ParsingValueDescriptor<>("1081", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Long> baleSlices = new ParsingValueDescriptor<>("1082", Long.class, 4, ByteBuf::readUnsignedInt);
    public static final ParsingValueDescriptor<Integer> lvcanMaxRoadSpeed = new ParsingValueDescriptor<>("1116", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Integer> lvcanExceededRoadSpeed = new ParsingValueDescriptor<>("1117", Integer.class, 1, buf -> (int) buf.readUnsignedByte());
    public static final ParsingValueDescriptor<Boolean> lvcanSpeedLimitSign = new ParsingValueDescriptor<>("1205", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> lvcanEndOfSpeedLimitSign = new ParsingValueDescriptor<>("1206", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> lvcanSpeedExceededBySign = new ParsingValueDescriptor<>("1207", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> lvcanTimeSpecificSpeedLimitSign = new ParsingValueDescriptor<>("1208", Boolean.class, 1, ByteBuf::readBoolean);
    public static final ParsingValueDescriptor<Boolean> lvcanWeatherSpeedLimitSign = new ParsingValueDescriptor<>("1209", Boolean.class, 1, ByteBuf::readBoolean);

    public static final ParsingValueDescriptor<Integer> priority = new ParsingValueDescriptor<>("pr", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final ParsingValueDescriptor<Integer> altitude = new ParsingValueDescriptor<>("alt", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final ParsingValueDescriptor<Integer> direction = new ParsingValueDescriptor<>("ang", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final ParsingValueDescriptor<Integer> satellites = new ParsingValueDescriptor<>("sat", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final ParsingValueDescriptor<Integer> speedSatellite = new ParsingValueDescriptor<>("sp", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final ParsingValueDescriptor<Integer> eventTriggered = new ParsingValueDescriptor<>("evt", Integer.class, TeltonikaValueDescriptors::parseIntFromUtf8);
    public static final ParsingValueDescriptor<Long> timestamp = new ParsingValueDescriptor<>("ts", Long.class, TeltonikaValueDescriptors::parseLongFromUtf8);
    public static final ParsingValueDescriptor<GeoJSONPoint> location = new ParsingValueDescriptor<>("latlng", GeoJSONPoint.class, TeltonikaValueDescriptors::parseLatLng);

    private static final Map<String, ParsingValueDescriptor<?>> DESCRIPTORS_BY_ID;
    private static final Map<String, ParsingValueDescriptor<?>> DESCRIPTORS_BY_NAME;

    static {
        var descriptors = Arrays.stream(TeltonikaValueDescriptors.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) &&
                        ParsingValueDescriptor.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    try {
                        return (ParsingValueDescriptor<?>) field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(desc -> desc.getName() != null)
                .toList();

        DESCRIPTORS_BY_ID = descriptors.stream().collect(Collectors.toMap(ParsingValueDescriptor::getName, Function.identity()));
        DESCRIPTORS_BY_NAME = descriptors.stream().collect(Collectors.toMap(ParsingValueDescriptor::getName, Function.identity()));
    }

    public static Optional<ParsingValueDescriptor<?>> getById(String id) {
        return Optional.ofNullable(DESCRIPTORS_BY_ID.get(id));
    }

    public static Optional<ParsingValueDescriptor<?>> getByName(String name) {
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
    private static final Map<String, ParsingValueDescriptor<?>> DESCRIPTOR_MAP = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(TeltonikaValueDescriptors.class.getName());

    static {
        // Populate descriptor map using reflection
        for (Field field : TeltonikaValueDescriptors.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                ParsingValueDescriptor.class.isAssignableFrom(field.getType())) {
                try {
                    ParsingValueDescriptor<?> descriptor = (ParsingValueDescriptor<?>) field.get(null);
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
     * Get a ParsingValueDescriptor by its Teltonika AVL ID.
     *
     * @param id The AVL ID as a string (e.g., "11", "239", "sp")
     * @return The corresponding ParsingValueDescriptor, or null if not found
     */
    public static ParsingValueDescriptor<?> getDescriptorById(String id) {
        return DESCRIPTOR_MAP.get("teltonika_"+id);
    }

    /**
     * Check if a descriptor exists for the given AVL ID.
     *
     * @param id The AVL ID as a string
     * @return true if a descriptor exists
     */
    public static boolean hasDescriptor(String id) {
        return DESCRIPTOR_MAP.containsKey(id);
    }

    /**
     * Get all registered descriptor IDs.
     *
     * @return Set of all AVL IDs that have descriptors
     */
    public static Set<String> getAllDescriptorIds() {
        return Collections.unmodifiableSet(DESCRIPTOR_MAP.keySet());
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
