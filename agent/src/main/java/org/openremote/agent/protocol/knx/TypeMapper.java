package org.openremote.agent.protocol.knx;

import org.openremote.model.attribute.AttributeType;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.dptxlator.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TypeMapper {

    private static final Logger LOG = Logger.getLogger(TypeMapper.class.getName());

    /** map dpt to OpenRemote type */
    static private Map<String, AttributeType> dptToTypeMap;

    /** map KNX DPT to OpenRemote type */
    static private Map<AttributeType, String> typeToDptMap;

    static {
        dptToTypeMap = new HashMap<>();

        // Main number 1
        dptToTypeMap.put(DPTXlatorBoolean.DPT_SWITCH.getID(), AttributeType.BOOLEAN);
        dptToTypeMap.put(DPTXlatorBoolean.DPT_BOOL.getID(), AttributeType.BOOLEAN);
        dptToTypeMap.put(DPTXlatorBoolean.DPT_OPENCLOSE.getID(), AttributeType.BOOLEAN);

        // Main number 2
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_SWITCH_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_BOOL_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_ENABLE_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_RAMP_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_ALARM_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_BINARY_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_STEP_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_UPDOWN_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_OPENCLOSE_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_START_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_STATE_CONTROL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_INVERT_CONTROL.getID(), AttributeType.NUMBER);

        // Main number 3
        // dptToTypeMap.put(DPTXlator3BitControlled.DPT_CONTROL_DIMMING.getID(), AttributeType.IncreaseDecrease);

        // Datapoint Types "8-Bit Unsigned Value", Main number 5
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_SCALING.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_ANGLE.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_PERCENT_U8.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_DECIMALFACTOR.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_TARIFF.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_VALUE_1_UCOUNT.getID(), AttributeType.NUMBER);

        // Datapoint Types "8-bit Signed Value", Main number 6
        dptToTypeMap.put(DPTXlator8BitSigned.DPT_PERCENT_V8.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitSigned.DPT_VALUE_1_UCOUNT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitSigned.DPT_STATUS_MODE3.getID(), AttributeType.NUMBER);

        // Datapoint Types "2-Octet Unsigned Value", Main number 7
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_VALUE_2_UCOUNT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_10.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_100.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_SEC.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_MIN.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_HOURS.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_PROP_DATATYPE.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_LENGTH.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_ELECTRICAL_CURRENT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_BRIGHTNESS.getID(), AttributeType.NUMBER);

        // Datapoint Types "2-Octet Float Value", Main number 9
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMPERATURE.getID(), AttributeType.TEMPERATURE_CELCIUS);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMPERATURE_DIFFERENCE.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMPERATURE_GRADIENT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_INTENSITY_OF_LIGHT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_WIND_SPEED.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_AIR_PRESSURE.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_HUMIDITY.getID(), AttributeType.HUMIDITY_PERCENTAGE);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_AIRQUALITY.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE1.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE2.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_VOLTAGE.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_ELECTRICAL_CURRENT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_POWERDENSITY.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_KELVIN_PER_PERCENT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_POWER.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_VOLUME_FLOW.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_RAIN_AMOUNT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMP_F.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_WIND_SPEED_KMH.getID(), AttributeType.NUMBER);

        // Datapoint Types "Time", Main number 10
        dptToTypeMap.put(DPTXlatorTime.DPT_TIMEOFDAY.getID(), AttributeType.DATETIME);

        // Datapoint Types “Date”", Main number 11
        dptToTypeMap.put(DPTXlatorDate.DPT_DATE.getID(), AttributeType.DATETIME);

        // Datapoint Types "4-Octet Unsigned Value", Main number 12
        dptToTypeMap.put(DPTXlator4ByteUnsigned.DPT_VALUE_4_UCOUNT.getID(), AttributeType.NUMBER);

        // Datapoint Types "4-Octet Signed Value", Main number 13
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_COUNT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_FLOWRATE.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_ACTIVE_ENERGY.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_APPARENT_ENERGY.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_REACTIVE_ENERGY.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_ACTIVE_ENERGY_KWH.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_APPARENT_ENERGY_KVAH.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_REACTIVE_ENERGY_KVARH.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_DELTA_TIME.getID(), AttributeType.NUMBER);

        // Datapoint Types "4-Octet Float Value", Main number 14
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ACCELERATION_ANGULAR.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ANGLE_DEG.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ELECTRIC_CURRENT.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ELECTRIC_POTENTIAL.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_FREQUENCY.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_POWER.getID(), AttributeType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_PRESSURE.getID(), AttributeType.NUMBER);

        // Datapoint Types "String", Main number 16
        dptToTypeMap.put(DPTXlatorString.DPT_STRING_8859_1.getID(), AttributeType.STRING);

        // Datapoint Types "Scene Number", Main number 17
        dptToTypeMap.put(DPTXlatorSceneNumber.DPT_SCENE_NUMBER.getID(), AttributeType.NUMBER);

        // Datapoint Types "Scene Control", Main number 18
        dptToTypeMap.put(DPTXlatorSceneControl.DPT_SCENE_CONTROL.getID(), AttributeType.NUMBER);

        // Datapoint Types "DateTime", Main number 19
        dptToTypeMap.put(DPTXlatorDateTime.DPT_DATE_TIME.getID(), AttributeType.DATETIME);

        // Datapoint Types "RGB Color", Main number 232
        dptToTypeMap.put(DPTXlatorRGB.DPT_RGB.getID(), AttributeType.COLOR_RGB);

        typeToDptMap = new HashMap<>();
        typeToDptMap.put(AttributeType.BOOLEAN, DPTXlatorBoolean.DPT_SWITCH.getID());
        typeToDptMap.put(AttributeType.PERCENTAGE, DPTXlator8BitUnsigned.DPT_SCALING.getID());
        typeToDptMap.put(AttributeType.NUMBER, DPTXlator2ByteFloat.DPT_TEMPERATURE.getID());
        typeToDptMap.put(AttributeType.DATETIME, DPTXlatorTime.DPT_TIMEOFDAY.getID());
        typeToDptMap.put(AttributeType.STRING, DPTXlatorString.DPT_STRING_8859_1.getID());
        typeToDptMap.put(AttributeType.COLOR_RGB, DPTXlatorRGB.DPT_RGB.getID());
    }

    @SuppressWarnings("ConstantConditions")
    public static DPTXlator toDPTXlator(Datapoint datapoint, Value value) throws Exception {

        DPTXlator translator = TranslatorTypes.createTranslator(0, datapoint.getDPT());

        if (translator instanceof DPTXlatorBoolean && value != null && value.getType() == ValueType.BOOLEAN) {
            ((DPTXlatorBoolean) translator).setValue(Values.getBoolean(value).get());
        } else if (translator instanceof DPTXlator8BitUnsigned && value != null && value.getType() == ValueType.NUMBER) {
            ((DPTXlator8BitUnsigned) translator).setValue(Values.getIntegerCoerced(value).orElse(0));
        } else if (translator instanceof DPTXlatorRGB &&  value != null && value.getType() == ValueType.ARRAY) {
            ArrayValue arrayValue = Values.getArray(value).get();
            ((DPTXlatorRGB) translator).setValue(arrayValue.getNumber(0).get().intValue(), arrayValue.getNumber(1).get().intValue(), arrayValue.getNumber(2).get().intValue());
        } else {
            // TODO depending on the DPT and the value, a more sophisticated translation is needed
            translator.setValue(value.toString());
        }
        return translator;
    }

    public static Value toORValue(Datapoint datapoint, byte[] data) throws Exception {

        DPTXlator translator = TranslatorTypes.createTranslator(0, datapoint.getDPT());
        translator.setData(data);
        LOG.info("Received KNX data: " + translator.getType().getID() + " (" + translator.getType().getDescription() + ") is " + translator.getValue()
                        + " (" + translator.getNumericValue() + ") - " + datapoint.getName());
        Value value;

        if (translator instanceof DPTXlatorBoolean) {
            value = Values.create(((DPTXlatorBoolean) translator).getValueBoolean());
        } else if (translator instanceof DPTXlator2ByteFloat) {
            value = Values.create(new BigDecimal(translator.getNumericValue()).setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else if (translator instanceof DPTXlator8BitUnsigned) {
            value = Values.create(translator.getNumericValue());
        } else {
            // TODO depending on the DPTXlator a more sophisticated translation to value is needed
            value = Values.create(translator.getValue());
        }

        return value;
    }

    static public AttributeType toAttributeType(Datapoint datapoint) {
        LOG.finer("toTypeClass looking for dptId = " + datapoint.getDPT());
        AttributeType t = dptToTypeMap.get(datapoint.getDPT());
        if (t == null) {
            if (datapoint.getMainNumber() == 1) {
                t = AttributeType.BOOLEAN;
            } else {
                t = AttributeType.STRING;
            }
        }
        return t;
    }

    static public String toDPTid(AttributeType type) {
        // TODO we might need to support more different DPT types
        return typeToDptMap.get(type);
    }

}
