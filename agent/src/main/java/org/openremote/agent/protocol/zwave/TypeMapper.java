/*
 * Copyright 2018, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.zwave;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;
import org.openremote.protocol.zwave.model.commandclasses.channel.ChannelType;

import java.util.HashMap;
import java.util.Map;

import static org.openremote.model.Constants.*;

public class TypeMapper {

    public static final class TypeInfo {
        protected ValueDescriptor<?> valueDescriptor;
        protected String[] units;
        protected ValueConstraint[] constraints;
        protected ValueFormat valueFormat;

        public TypeInfo(ValueDescriptor<?> valueDescriptor) {
            this.valueDescriptor = valueDescriptor;
        }

        public TypeInfo(ValueDescriptor<?> valueDescriptor, String[] units) {
            this.valueDescriptor = valueDescriptor;
            this.units = units;
        }

        public TypeInfo(ValueDescriptor<?> valueDescriptor, String[] units, ValueFormat valueFormat) {
            this.valueDescriptor = valueDescriptor;
            this.units = units;
            this.valueFormat = valueFormat;
        }

        public TypeInfo(ValueDescriptor<?> valueDescriptor, String[] units, ValueFormat valueFormat, ValueConstraint[] constraints) {
            this.valueDescriptor = valueDescriptor;
            this.units = units;
            this.constraints = constraints;
            this.valueFormat = valueFormat;
        }
    }
    
    static private final Map<ChannelType, TypeInfo> typeMap = new HashMap<>();

    static {

        // Basic types

        typeMap.put(ChannelType.INTEGER, new TypeInfo(ValueType.INTEGER));
        typeMap.put(ChannelType.NUMBER, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.STRING, new TypeInfo(ValueType.TEXT));
        typeMap.put(ChannelType.BOOLEAN, new TypeInfo(ValueType.BOOLEAN));
        typeMap.put(ChannelType.ARRAY, new TypeInfo(ValueType.JSON_OBJECT.asArray()));

        // COMMAND_CLASS_SENSOR_MULTILEVEL

        typeMap.put(ChannelType.TEMPERATURE_CELSIUS, new TypeInfo(ValueType.NUMBER, units(UNITS_CELSIUS), ValueFormat.NUMBER_1_DP()));
        typeMap.put(ChannelType.TEMPERATURE_FAHRENHEIT, new TypeInfo(ValueType.NUMBER, units(UNITS_FAHRENHEIT), ValueFormat.NUMBER_1_DP()));
        typeMap.put(ChannelType.PERCENTAGE, new TypeInfo(ValueType.NUMBER, units(UNITS_PERCENTAGE)));
        typeMap.put(ChannelType.LUMINANCE_PERCENTAGE, new TypeInfo(ValueType.INTEGER, units(UNITS_PERCENTAGE), null, ValueConstraint.constraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100))));
        typeMap.put(ChannelType.LUMINANCE_LUX, new TypeInfo(ValueType.INTEGER, units(UNITS_LUX)));
        typeMap.put(ChannelType.POWER_WATT, new TypeInfo(ValueType.INTEGER, units(UNITS_WATT)));
        typeMap.put(ChannelType.POWER_BTU_H, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_BTU, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.HUMIDITY_PERCENTAGE, new TypeInfo(ValueType.INTEGER, units(UNITS_PERCENTAGE)));
        typeMap.put(ChannelType.HUMIDITY_ABSOLUTE, new TypeInfo(ValueType.INTEGER.withUnits(UNITS_GRAM, UNITS_PER, UNITS_METRE, UNITS_CUBED)));
        typeMap.put(ChannelType.SPEED_MS, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND)));
        typeMap.put(ChannelType.SPEED_MPH, new TypeInfo(ValueType.INTEGER.withUnits(UNITS_MILE, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.DIRECTION_DECIMAL_DEGREES, new TypeInfo(ValueType.INTEGER, units(UNITS_DEGREE)));
        typeMap.put(ChannelType.PRESSURE_KPA, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_PASCAL)));
        typeMap.put(ChannelType.PRESSURE_IN_HG, new TypeInfo(ValueType.NUMBER, units(UNITS_IN_HG)));
        typeMap.put(ChannelType.SOLAR_RADIATION_WATT_M2, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_WATT, UNITS_PER, UNITS_METRE, UNITS_SQUARED)));
        typeMap.put(ChannelType.DEW_POINT_CELSIUS, new TypeInfo(ValueType.NUMBER, units(UNITS_CELSIUS)));
        typeMap.put(ChannelType.DEW_POINT_FAHRENHEIT, new TypeInfo(ValueType.NUMBER, units(UNITS_FAHRENHEIT)));
        typeMap.put(ChannelType.RAINFALL_MMPH, new TypeInfo(ValueType.INTEGER.withUnits(UNITS_MILLI, UNITS_METRE, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.RAINFALL_INPH, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_INCH, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.TIDE_LEVEL_M, new TypeInfo(ValueType.NUMBER, units(UNITS_METRE)));
        typeMap.put(ChannelType.TIDE_LEVEL_FT, new TypeInfo(ValueType.NUMBER, units(UNITS_FOOT)));
        typeMap.put(ChannelType.WEIGHT_KG, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_GRAM)));
        typeMap.put(ChannelType.WEIGHT_LB, new TypeInfo(ValueType.NUMBER, units(UNITS_MASS_POUND)));
        typeMap.put(ChannelType.VOLTAGE_V, new TypeInfo(ValueType.NUMBER, units(UNITS_VOLT)));
        typeMap.put(ChannelType.VOLTAGE_MV, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_MILLI, UNITS_VOLT)));
        typeMap.put(ChannelType.CURRENT_A, new TypeInfo(ValueType.NUMBER, units(UNITS_AMP)));
        typeMap.put(ChannelType.CURRENT_MA, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_MILLI, UNITS_AMP)));
        typeMap.put(ChannelType.CO2_PPM, new TypeInfo(ValueType.INTEGER, units(UNITS_PART_PER_MILLION)));
        typeMap.put(ChannelType.AIR_FLOW_CMPH, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_CUBED, UNITS_PER, UNITS_MINUTE)));
        typeMap.put(ChannelType.AIR_FLOW_CFTPM, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_FOOT, UNITS_CUBED, UNITS_PER, UNITS_MINUTE)));
        typeMap.put(ChannelType.TANK_CAPACITY_L, new TypeInfo(ValueType.NUMBER, units(UNITS_LITRE)));
        typeMap.put(ChannelType.TANK_CAPACITY_CBM, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_CUBED)));
        typeMap.put(ChannelType.TANK_CAPACITY_GAL, new TypeInfo(ValueType.NUMBER, units(UNITS_GALLON)));
        typeMap.put(ChannelType.DISTANCE_M, new TypeInfo(ValueType.NUMBER, units(UNITS_METRE)));
        typeMap.put(ChannelType.DISTANCE_CM, new TypeInfo(ValueType.POSITIVE_NUMBER.withUnits(UNITS_CENTI, UNITS_METRE)));
        typeMap.put(ChannelType.DISTANCE_FT, new TypeInfo(ValueType.POSITIVE_NUMBER, units(UNITS_FOOT)));
        typeMap.put(ChannelType.ANGLE_POSITION_PERCENT, new TypeInfo(ValueType.INTEGER, units(UNITS_PERCENTAGE), null, ValueConstraint.constraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100))));
        typeMap.put(ChannelType.ANGLE_POSITION_DEGREE_NORTH_POLE, new TypeInfo(ValueType.INTEGER, units(UNITS_DEGREE)));
        typeMap.put(ChannelType.ANGLE_POSITION_DEGREE_SOUTH_POLE, new TypeInfo(ValueType.INTEGER, units(UNITS_DEGREE)));
        typeMap.put(ChannelType.ROTATION_HZ, new TypeInfo(ValueType.NUMBER, units(UNITS_HERTZ)));
        typeMap.put(ChannelType.ROTATION_RPM, new TypeInfo(ValueType.NUMBER, units(UNITS_RPM)));
        typeMap.put(ChannelType.WATER_TEMPERATURE_CELSIUS, new TypeInfo(ValueType.NUMBER, units(UNITS_CELSIUS)));
        typeMap.put(ChannelType.WATER_TEMPERATURE_FAHRENHEIT, new TypeInfo(ValueType.NUMBER, units(UNITS_FAHRENHEIT)));
        typeMap.put(ChannelType.SOIL_TEMPERATURE_CELSIUS, new TypeInfo(ValueType.NUMBER, units(UNITS_CELSIUS)));
        typeMap.put(ChannelType.SOIL_TEMPERATURE_FAHRENHEIT, new TypeInfo(ValueType.NUMBER, units(UNITS_FAHRENHEIT)));
        typeMap.put(ChannelType.SEISMIC_INTENSITY_MERCALLI, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_INTENSITY_EU_MACROSEISMIC, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_INTENSITY_LIEDU, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_INTENSITY_SHINDO, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_LOCAL, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_MOMENT, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_SURFACE_WAVE, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_BODY_WAVE, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.ULTRAVIOLET_UV_INDEX, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.RESISTIVITY_OHM, new TypeInfo(ValueType.NUMBER, units(UNITS_OHM)));
        typeMap.put(ChannelType.CONDUCTIVITY_SPM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.LOUDNESS_DB, new TypeInfo(ValueType.NUMBER, units(UNITS_DECIBEL)));
        typeMap.put(ChannelType.LOUDNESS_DBA, new TypeInfo(ValueType.NUMBER, units(UNITS_DECIBEL_ATTENUATED)));
        typeMap.put(ChannelType.MOISTURE_PERCENTAGE, new TypeInfo(ValueType.NUMBER, units(UNITS_PERCENTAGE)));
        typeMap.put(ChannelType.MOISTURE_VOLUME_WATER_CONTENT, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.MOISTURE_IMPEDANCE, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.MOISTURE_WATER_ACTIVITY, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.FREQUENCY_HZ, new TypeInfo(ValueType.NUMBER, units(UNITS_HERTZ)));
        typeMap.put(ChannelType.FREQUENCY_KHZ, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_HERTZ)));
        typeMap.put(ChannelType.TIME_SECONDS, new TypeInfo(ValueType.NUMBER, units(UNITS_SECOND)));
        typeMap.put(ChannelType.TARGET_TEMPERATUE_CELSIUS, new TypeInfo(ValueType.NUMBER, units(UNITS_CELSIUS)));
        typeMap.put(ChannelType.TARGET_TEMPERATUE_FAHRENHEIT, new TypeInfo(ValueType.NUMBER, units(UNITS_FAHRENHEIT)));
        typeMap.put(ChannelType.PARTICULATE_MATTER_2_5_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.PARTICULATE_MATTER_2_5_MCGPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.FORMALDEHYDE_LEVEL_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.RADON_CONCENTRATION_BQPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.RADON_CONCENTRATION_PCIPL, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.METHANE_DENSITY_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.VOLATILE_ORGANIC_COMPOUND_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.VOLATILE_ORGANIC_COMPOUND_PPM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.CO_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.CO_PPM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SOIL_HUMIDITY_PERCENTAGE, new TypeInfo(ValueType.NUMBER, units(UNITS_PERCENTAGE)));
        typeMap.put(ChannelType.SOIL_REACTIVITY_PH, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.SOIL_SALINITY_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.HEART_RATE_BPM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.BLOOD_PRESSURE_SYSTOLIC, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.BLOOD_PRESSURE_DIASTOLIC, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.MUSCLE_MASS_KG, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_GRAM)));
        typeMap.put(ChannelType.FAT_MASS_KG, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_GRAM)));
        typeMap.put(ChannelType.BONE_MASS_KG, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_GRAM)));
        typeMap.put(ChannelType.TOTAL_BODY_WATER_KG, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_GRAM)));
        typeMap.put(ChannelType.BASIC_METABOLIC_RATE_JOULE, new TypeInfo(ValueType.NUMBER, units(UNITS_JOULE)));
        typeMap.put(ChannelType.BODY_MASS_INDEX, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.ACCELERATION_X_MPSS, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND, UNITS_SQUARED)));
        typeMap.put(ChannelType.ACCELERATION_Y_MPSS, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND, UNITS_SQUARED)));
        typeMap.put(ChannelType.ACCELERATION_Z_MPSS, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND, UNITS_SQUARED)));
        typeMap.put(ChannelType.SMOKE_DENSITY_PERCENTAGE, new TypeInfo(ValueType.NUMBER, units(UNITS_PERCENTAGE)));
        typeMap.put(ChannelType.WATER_FLOW_LPH, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.WATER_PRESSURE_KPA, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_PASCAL)));
        typeMap.put(ChannelType.RF_SIGNAL_STRENGTH_RSSI, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.RF_SIGNAL_STRENGTH_DBM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.PARTICULATE_MATTER_MOLPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.PARTICULATE_MATTER_MCGPCM, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.RESPIRATORY_RATE_BPM, new TypeInfo(ValueType.NUMBER));

        // COMMAND_CLASS_METER

        // Electric Meter
        typeMap.put(ChannelType.ELECTRIC_METER_ENERGY_KWH, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_WATT, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.ELECTRIC_METER_ENERGY_KVAH, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_WATT, UNITS_PER, UNITS_HOUR)));
        typeMap.put(ChannelType.ELECTRIC_METER_POWER_W, new TypeInfo(ValueType.NUMBER, units(UNITS_WATT)));
        typeMap.put(ChannelType.ELECTRIC_METER_PULSE_COUNT, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.ELECTRIC_METER_VOLTAGE_V, new TypeInfo(ValueType.NUMBER, units(UNITS_VOLT)));
        typeMap.put(ChannelType.ELECTRIC_METER_CURRENT_A, new TypeInfo(ValueType.NUMBER, units(UNITS_AMP)));
        typeMap.put(ChannelType.ELECTRIC_METER_POWER_FACTOR, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.ELECTRIC_METER_POWER_KVAR, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_WATT)));
        typeMap.put(ChannelType.ELECTRIC_METER_ENERGY_KVARH, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_KILO, UNITS_WATT, UNITS_PER, UNITS_HOUR)));

        // Gas Meter
        typeMap.put(ChannelType.GAS_METER_VOLUME_CM, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_CUBED)));
        typeMap.put(ChannelType.GAS_METER_VOLUME_CFT, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_FOOT, UNITS_CUBED)));
        typeMap.put(ChannelType.GAS_METER_PULSE_COUNT, new TypeInfo(ValueType.NUMBER));

        // Water Meter
        typeMap.put(ChannelType.WATER_METER_VOLUME_CM, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_METRE, UNITS_CUBED)));
        typeMap.put(ChannelType.WATER_METER_VOLUME_CFT, new TypeInfo(ValueType.NUMBER.withUnits(UNITS_FOOT, UNITS_CUBED)));
        typeMap.put(ChannelType.WATER_METER_VOLUME_GAL, new TypeInfo(ValueType.NUMBER, units(UNITS_GALLON)));
        typeMap.put(ChannelType.WATER_METER_PULSE_COUNT, new TypeInfo(ValueType.NUMBER));

        // COMMAND_CLASS_COLOR_CONTROL

        typeMap.put(ChannelType.COLOR_WARM_WHITE, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_COLD_WHITE, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_RED, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_GREEN, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_BLUE, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_AMBER, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_CYAN, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_PURPLE, new TypeInfo(ValueType.INT_BYTE));
        typeMap.put(ChannelType.COLOR_INDEXED, new TypeInfo(ValueType.NUMBER));
        typeMap.put(ChannelType.COLOR_RGB, new TypeInfo(ValueType.COLOUR_RGB));
        typeMap.put(ChannelType.COLOR_ARGB, new TypeInfo(ValueType.COLOUR_RGB));

        // COMMAND_CLASS_SENSOR_ALARM

        typeMap.put(ChannelType.GENERAL_PURPOSE_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));
        typeMap.put(ChannelType.SMOKE_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));
        typeMap.put(ChannelType.CO_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));
        typeMap.put(ChannelType.CO2_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));
        typeMap.put(ChannelType.HEAT_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));
        typeMap.put(ChannelType.WATER_LEAK_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));
        typeMap.put(ChannelType.FIRST_SUPPORTED_ALARM, new TypeInfo(ValueType.BOOLEAN, null, ValueFormat.BOOLEAN_ON_OFF()));

        // COMMAND_CLASS_BATTERY

        typeMap.put(ChannelType.CHARGE_PERCENTAGE, new TypeInfo(ValueType.NUMBER, units(UNITS_PERCENTAGE)));

        // COMMAND_CLASS_CLOCK

        typeMap.put(ChannelType.DATETIME, new TypeInfo(ValueType.TIMESTAMP_ISO8601));
    }

    public static Attribute<?> createAttribute(String name, ChannelType channelType) {

        ValueDescriptor<?> valueType = ValueType.TEXT;
        ValueConstraint[] constraints = null;
        ValueFormat format = null;
        String[] units = null;

        if (typeMap.containsKey(channelType)) {
            TypeInfo typeInfo = typeMap.get(channelType);
            valueType = typeInfo.valueDescriptor;
            constraints = typeInfo.constraints;
            format = typeInfo.valueFormat;
            units = typeInfo.units;
        } else {
            switch(channelType.getValueType()) {
                case INTEGER:
                    valueType = ValueType.INTEGER;
                    break;
                case NUMBER:
                    valueType = ValueType.NUMBER;
                    break;
                case BOOLEAN:
                    valueType = ValueType.BOOLEAN;
                    break;
                case STRING:
                    valueType = ValueType.TEXT;
                    break;
                case ARRAY:
                    valueType = ValueDescriptor.UNKNOWN.asArray();
                    break;
            }
        }

        Attribute<?> attribute = new Attribute<>(name, valueType);
        if (constraints != null) {
            attribute.addMeta(new MetaItem<>(MetaItemType.CONSTRAINTS, constraints));
        }
        if (format != null) {
            attribute.addMeta(new MetaItem<>(MetaItemType.FORMAT, format));
        }
        if (units != null) {
            attribute.addMeta(new MetaItem<>(MetaItemType.UNITS, units));
        }
        return attribute;
    }
}
