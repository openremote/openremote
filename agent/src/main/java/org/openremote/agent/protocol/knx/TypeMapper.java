package org.openremote.agent.protocol.knx;

import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.impl.ColourRGB;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.dptxlator.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class TypeMapper {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TypeMapper.class);

    /** map dpt to OpenRemote type */
    static private final Map<String, ValueDescriptor<?>> dptToTypeMap;

    static {
        dptToTypeMap = new HashMap<>();

        // Main number 1
        dptToTypeMap.put(DPTXlatorBoolean.DPT_SWITCH.getID(), ValueType.BOOLEAN);
        dptToTypeMap.put(DPTXlatorBoolean.DPT_BOOL.getID(), ValueType.BOOLEAN);
        dptToTypeMap.put(DPTXlatorBoolean.DPT_OPENCLOSE.getID(), ValueType.BOOLEAN);

        // Main number 2
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_SWITCH_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_BOOL_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_ENABLE_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_RAMP_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_ALARM_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_BINARY_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_STEP_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_UPDOWN_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_OPENCLOSE_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_START_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_STATE_CONTROL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator1BitControlled.DPT_INVERT_CONTROL.getID(), ValueType.NUMBER);

        // Main number 3
        // dptToTypeMap.put(DPTXlator3BitControlled.DPT_CONTROL_DIMMING.getID(), ValueType.IncreaseDecrease);

        // Datapoint Types "8-Bit Unsigned Value", Main number 5
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_SCALING.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_ANGLE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_PERCENT_U8.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_DECIMALFACTOR.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_TARIFF.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitUnsigned.DPT_VALUE_1_UCOUNT.getID(), ValueType.NUMBER);

        // Datapoint Types "8-bit Signed Value", Main number 6
        dptToTypeMap.put(DPTXlator8BitSigned.DPT_PERCENT_V8.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitSigned.DPT_VALUE_1_UCOUNT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator8BitSigned.DPT_STATUS_MODE3.getID(), ValueType.NUMBER);

        // Datapoint Types "2-Octet Unsigned Value", Main number 7
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_VALUE_2_UCOUNT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_10.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_100.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_SEC.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_MIN.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_HOURS.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_PROP_DATATYPE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_LENGTH.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_ELECTRICAL_CURRENT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteUnsigned.DPT_BRIGHTNESS.getID(), ValueType.NUMBER);

        // Datapoint Types "2-Octet Float Value", Main number 9
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMPERATURE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMPERATURE_DIFFERENCE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMPERATURE_GRADIENT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_INTENSITY_OF_LIGHT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_WIND_SPEED.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_AIR_PRESSURE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_HUMIDITY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_AIRQUALITY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE1.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE2.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_VOLTAGE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_ELECTRICAL_CURRENT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_POWERDENSITY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_KELVIN_PER_PERCENT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_POWER.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_VOLUME_FLOW.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_RAIN_AMOUNT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_TEMP_F.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator2ByteFloat.DPT_WIND_SPEED_KMH.getID(), ValueType.NUMBER);

        // Datapoint Types "Time", Main number 10
        dptToTypeMap.put(DPTXlatorTime.DPT_TIMEOFDAY.getID(), ValueType.TIMESTAMP_ISO8601);

        // Datapoint Types "Date", Main number 11
        dptToTypeMap.put(DPTXlatorDate.DPT_DATE.getID(), ValueType.TIMESTAMP_ISO8601);

        // Datapoint Types "4-Octet Unsigned Value", Main number 12
        dptToTypeMap.put(DPTXlator4ByteUnsigned.DPT_VALUE_4_UCOUNT.getID(), ValueType.NUMBER);

        // Datapoint Types "4-Octet Signed Value", Main number 13
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_COUNT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_FLOWRATE.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_ACTIVE_ENERGY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_APPARENT_ENERGY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_REACTIVE_ENERGY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_ACTIVE_ENERGY_KWH.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_APPARENT_ENERGY_KVAH.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_REACTIVE_ENERGY_KVARH.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteSigned.DPT_DELTA_TIME.getID(), ValueType.NUMBER);

        // Datapoint Types "4-Octet Float Value", Main number 14
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ACCELERATION_ANGULAR.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ANGLE_DEG.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ELECTRIC_CURRENT.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_ELECTRIC_POTENTIAL.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_FREQUENCY.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_POWER.getID(), ValueType.NUMBER);
        dptToTypeMap.put(DPTXlator4ByteFloat.DPT_PRESSURE.getID(), ValueType.NUMBER);

        // Datapoint Types "String", Main number 16
        dptToTypeMap.put(DPTXlatorString.DPT_STRING_8859_1.getID(), ValueType.TEXT);

        // Datapoint Types "Scene Number", Main number 17
        dptToTypeMap.put(DPTXlatorSceneNumber.DPT_SCENE_NUMBER.getID(), ValueType.NUMBER);

        // Datapoint Types "Scene Control", Main number 18
        dptToTypeMap.put(DPTXlatorSceneControl.DPT_SCENE_CONTROL.getID(), ValueType.NUMBER);

        // Datapoint Types "DateTime", Main number 19
        dptToTypeMap.put(DPTXlatorDateTime.DPT_DATE_TIME.getID(), ValueType.TIMESTAMP_ISO8601);

        // Datapoint Types "RGB Color", Main number 232
        dptToTypeMap.put(DPTXlatorRGB.DPT_RGB.getID(), ValueType.COLOUR_RGB);
    }

    public static DPTXlator toDPTXlator(Datapoint datapoint, Object value) throws Exception {

        DPTXlator translator = TranslatorTypes.createTranslator(0, datapoint.getDPT());

        if (value == null) {
            return translator;
        }

        if (translator instanceof DPTXlatorBoolean && ValueUtil.isBoolean(value.getClass())) {
            ((DPTXlatorBoolean) translator).setValue(ValueUtil.getBoolean(value).orElse(false));
        } else if (translator instanceof DPTXlator8BitUnsigned && ValueUtil.isNumber(value.getClass())) {
            ((DPTXlator8BitUnsigned) translator).setValue(ValueUtil.getIntegerCoerced(value).orElse(0));
        } else if (translator instanceof DPTXlatorRGB) {
            ColourRGB colorRGB = ValueUtil.convert(value, ColourRGB.class);
            if (colorRGB != null) {
                ((DPTXlatorRGB) translator).setValue(colorRGB.getR(), colorRGB.getG(), colorRGB.getB());
            }
        } else {
            // TODO depending on the DPT and the value, a more sophisticated translation is needed
            translator.setValue(value.toString());
        }
        return translator;
    }

    public static Object toValue(Datapoint datapoint, byte[] data) throws Exception {

        DPTXlator translator = TranslatorTypes.createTranslator(0, datapoint.getDPT());
        translator.setData(data);
        LOG.info("Received KNX data: " + translator.getType().getID() + " (" + translator.getType().getDescription() + ") is " + translator.getValue()
                        + " (" + translator.getNumericValue() + ") - " + datapoint.getName());

        if (translator instanceof DPTXlatorBoolean) {
            return ((DPTXlatorBoolean) translator).getValueBoolean();
        }
        if (translator instanceof DPTXlator2ByteFloat) {
            return BigDecimal.valueOf(translator.getNumericValue()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        if (translator instanceof DPTXlator8BitUnsigned) {
            return translator.getNumericValue();
        }

        // TODO depending on the DPTXlator a more sophisticated translation to value is needed
        return translator.getValue();
    }

    static public ValueDescriptor<?> toAttributeType(Datapoint datapoint) {
        LOG.finer("toTypeClass looking for dptId = " + datapoint.getDPT());
        ValueDescriptor<?> t = dptToTypeMap.get(datapoint.getDPT());
        
        if (t == null) {
            if (datapoint.getMainNumber() == 1) {
                t = ValueType.BOOLEAN;
            } else {
                t = ValueType.TEXT;
            }
        }
        return t;
    }
}
