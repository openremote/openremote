package org.openremote.agent.protocol.knx;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openremote.model.attribute.AttributeType;
import org.openremote.model.value.Value;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteSigned;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.DPTXlatorDate;
import tuwien.auto.calimero.dptxlator.DPTXlatorDateTime;
import tuwien.auto.calimero.dptxlator.DPTXlatorRGB;
import tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl;
import tuwien.auto.calimero.dptxlator.DPTXlatorSceneNumber;
import tuwien.auto.calimero.dptxlator.DPTXlatorString;
import tuwien.auto.calimero.dptxlator.DPTXlatorTime;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

public class TypeMapper {

    private static final Logger LOG = Logger.getLogger(TypeMapper.class.getName());

    /** map dpt to OpenRemote type */
    static private Map<String, AttributeType> dptToTypeMap;

    /** map KNX DPT to OpenRemote type */
    static private Map<AttributeType, String> typeToDptMap;

    static {
        dptToTypeMap = new HashMap<String, AttributeType>();

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

        typeToDptMap = new HashMap<AttributeType, String>();
        typeToDptMap.put(AttributeType.BOOLEAN, DPTXlatorBoolean.DPT_SWITCH.getID());
        typeToDptMap.put(AttributeType.PERCENTAGE, DPTXlator8BitUnsigned.DPT_SCALING.getID());
        typeToDptMap.put(AttributeType.NUMBER, DPTXlator2ByteFloat.DPT_TEMPERATURE.getID());
        typeToDptMap.put(AttributeType.DATETIME, DPTXlatorTime.DPT_TIMEOFDAY.getID());
        typeToDptMap.put(AttributeType.STRING, DPTXlatorString.DPT_STRING_8859_1.getID());
        typeToDptMap.put(AttributeType.COLOR_RGB, DPTXlatorRGB.DPT_RGB.getID());
    }

    public String toDPTValue(AttributeType type, String dptID) {

        DPT dpt;
        int mainNumber = getMainNumber(dptID);
        if (mainNumber == -1) {
            LOG.warning("toDPTValue couldn't identify mainnumber in dptID: " + dptID);
            return null;
        }

        try {
            DPTXlator translator = TranslatorTypes.createTranslator(mainNumber, dptID);
            dpt = translator.getType();

        } catch (KNXException e) {
            e.printStackTrace();
            return null;
        }

        // // check for HSBType first, because it extends PercentType as well
        // if (type == AttributeType.COLOR_RGB) {
        // Color color = ((HSBType) type).toColor();
        //
        // return "r:" + Integer.toString(color.getRed()) + " g:" + Integer.toString(color.getGreen()) + " b:"
        // + Integer.toString(color.getBlue());
        // } else if (type instanceof OnOffType) {
        // return type.equals(OnOffType.OFF) ? dpt.getLowerValue() : dpt.getUpperValue();
        // } else if (type instanceof UpDownType) {
        // return type.equals(UpDownType.UP) ? dpt.getLowerValue() : dpt.getUpperValue();
        // } else if (type instanceof IncreaseDecreaseType) {
        // DPT valueDPT = ((DPTXlator3BitControlled.DPT3BitControlled) dpt).getControlDPT();
        // return type.equals(IncreaseDecreaseType.DECREASE) ? valueDPT.getLowerValue() + " 5"
        // : valueDPT.getUpperValue() + " 5";
        // } else if (type instanceof OpenClosedType) {
        // return type.equals(OpenClosedType.CLOSED) ? dpt.getLowerValue() : dpt.getUpperValue();
        // } else if (type instanceof StopMoveType) {
        // return type.equals(StopMoveType.STOP) ? dpt.getLowerValue() : dpt.getUpperValue();
        // } else if (type instanceof PercentType) {
        // return type.toString();
        // } else if (type instanceof DecimalType) {
        // switch (mainNumber) {
        // case 2:
        // DPT valueDPT = ((DPTXlator1BitControlled.DPT1BitControlled) dpt).getValueDPT();
        // switch (((DecimalType) type).intValue()) {
        // case 0:
        // return "0 " + valueDPT.getLowerValue();
        // case 1:
        // return "0 " + valueDPT.getUpperValue();
        // case 2:
        // return "1 " + valueDPT.getLowerValue();
        // default:
        // return "1 " + valueDPT.getUpperValue();
        // }
        // case 18:
        // int intVal = ((DecimalType) type).intValue();
        // if (intVal > 63) {
        // return "learn " + (intVal - 0x80);
        // } else {
        // return "activate " + intVal;
        // }
        // default:
        // return type.toString();
        // }
        // } else if (type instanceof StringType) {
        // return type.toString();
        // } else if (type instanceof DateTimeType) {
        // return formatDateTime((DateTimeType) type, dptID);
        // }

        // logger.debug("toDPTValue: Couldn't get value for {} dpt id {} (no mapping).", type, dptID);

        return null;
    }

    public Value toValue(Datapoint datapoint, byte[] data) {
        try {
            DPTXlator translator = TranslatorTypes.createTranslator(datapoint.getMainNumber(), datapoint.getDPT());
            translator.setData(data);
            String value = translator.getValue();

            String id = translator.getType().getID();
            LOG.fine("toValue datapoint DPT = " + datapoint.getDPT());

            int mainNumber = getMainNumber(id);
            if (mainNumber == -1) {
                LOG.warning("toValue: couldn't identify mainnumber in dptID: " + id);
                return null;
            }
            int subNumber = getSubNumber(id);
            if (subNumber == -1) {
                LOG.warning("toType: couldn't identify sub number in dptID: " + id);
                return null;
            }
            // /*
            // * Following code section deals with specific mapping of values from KNX to openHAB types were the String
            // * received from the DPTXlator is not sufficient to set the openHAB type or has bugs
            // */
            // switch (mainNumber) {
            // case 1:
            // DPTXlatorBoolean translatorBoolean = (DPTXlatorBoolean) translator;
            // return new BooleanValueImpl(translatorBoolean.getValueBoolean());
            //
            // case 2:
            // DPTXlator1BitControlled translator1BitControlled = (DPTXlator1BitControlled) translator;
            // int decValue = (translator1BitControlled.getControlBit() ? 2 : 0)
            // + (translator1BitControlled.getValueBit() ? 1 : 0);
            // return new NumberValueImpl(decValue);
            // case 18:
            // DPTXlatorSceneControl translatorSceneControl = (DPTXlatorSceneControl) translator;
            // int decimalValue = translatorSceneControl.getSceneNumber();
            // if (value.startsWith("learn")) {
            // decimalValue += 0x80;
            // }
            // value = String.valueOf(decimalValue);
            // break;
            // }
            //
            // Class<? extends Type> typeClass = toAttributeType(id);
            // if (typeClass == null) {
            // return null;
            // }
            //
            // if (typeClass.equals(PercentType.class)) {
            // return PercentType.valueOf(value.split(" ")[0]);
            // }
            // if (typeClass.equals(AttributeType.NUMBER)) {
            // return DecimalType.valueOf(value.split(" ")[0]);
            // }
            // if (typeClass.equals(StringType.class)) {
            // return StringType.valueOf(value);
            // }
            //
            // if (typeClass.equals(DateTimeType.class)) {
            // String date = formatDateTime(value, datapoint.getDPT());
            // if ((date == null) || (date.isEmpty())) {
            // logger.debug("toType: KNX clock msg ignored: date object null or empty {}.", date);
            // return null;
            // } else {
            // return DateTimeType.valueOf(date);
            // }
            // }
            //
            // if (typeClass.equals(HSBType.class)) {
            // // value has format of "r:<red value> g:<green value> b:<blue value>"
            // int r = Integer.parseInt(value.split(" ")[0].split(":")[1]);
            // int g = Integer.parseInt(value.split(" ")[1].split(":")[1]);
            // int b = Integer.parseInt(value.split(" ")[2].split(":")[1]);
            //
            // Color color = new Color(r, g, b);
            // return new HSBType(color);
            // }
        } catch (KNXFormatException kfe) {
            LOG.info("Translator couldn't parse data for datapoint type '" + datapoint.getDPT() + "' (KNXFormatException).");
        } catch (KNXIllegalArgumentException kiae) {
            LOG.info("Translator couldn't parse data for datapoint type '" + datapoint.getDPT() + "' (KNXIllegalArgumentException).");
        } catch (KNXException e) {
            LOG.log(Level.WARNING, "Failed creating a translator for datapoint type '" + datapoint.getDPT() + "'.", e);
        }

        return null;
    }

    static public AttributeType toAttributeType(String dptId) {
        LOG.finer("toTypeClass looking for dptId = " + dptId);
        return dptToTypeMap.get(dptId);
    }

    static public String toDPTid(AttributeType type) {
        return typeToDptMap.get(type);
    }

    /**
     * Retrieves sub number from a DTP ID such as "14.001"
     *
     * @param dptID
     *            String with DPT ID
     * @return sub number or -1
     */
    private int getSubNumber(String dptID) {
        int result = -1;
        if (dptID == null) {
            throw new IllegalArgumentException("Parameter dptID cannot be null");
        }

        int dptSepratorPosition = dptID.indexOf('.');
        if (dptSepratorPosition > 0) {
            try {
                result = Integer.parseInt(dptID.substring(dptSepratorPosition + 1, dptID.length()));
            } catch (NumberFormatException nfe) {
                LOG.warning("toType couldn't identify main and/or sub number in dptID (NumberFormatException): " + dptID);
            } catch (IndexOutOfBoundsException ioobe) {
                LOG.warning("toType couldn't identify main and/or sub number in dptID (IndexOutOfBoundsException): " + dptID);
            }
        }
        return result;
    }

    /**
     * Retrieves main number from a DTP ID such as "14.001"
     *
     * @param dptID
     *            String with DPT ID
     * @return main number or -1
     */
    private int getMainNumber(String dptID) {
        int result = -1;
        if (dptID == null) {
            throw new IllegalArgumentException("Parameter dptID cannot be null");
        }

        int dptSepratorPosition = dptID.indexOf('.');
        if (dptSepratorPosition > 0) {
            try {
                result = Integer.parseInt(dptID.substring(0, dptSepratorPosition));
            } catch (NumberFormatException nfe) {
                LOG.warning("toType couldn't identify main and/or sub number in dptID (NumberFormatException): " + dptID);
            } catch (IndexOutOfBoundsException ioobe) {
                LOG.warning("toType couldn't identify main and/or sub number in dptID (IndexOutOfBoundsException): " + dptID);
            }
        }
        return result;
    }
}
