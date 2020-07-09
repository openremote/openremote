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

import org.openremote.model.Constants;
import org.openremote.model.attribute.AttributeValueDescriptor;
import org.openremote.model.attribute.AttributeValueType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.protocol.zwave.model.commandclasses.channel.ChannelType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TypeMapper {

    private static final Logger LOG = Logger.getLogger(TypeMapper.class.getName());

    static private Map<ChannelType, AttributeValueDescriptor> typeMap = new HashMap<>();

    static private Map<ChannelType, List<MetaItem>> metaItemMap = new HashMap<>();

    static {

        // Basic types

        typeMap.put(ChannelType.INTEGER, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.NUMBER, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.STRING, AttributeValueType.STRING);
        typeMap.put(ChannelType.BOOLEAN, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.ARRAY, AttributeValueType.ARRAY);

        // COMMAND_CLASS_SENSOR_MULTILEVEL

        typeMap.put(ChannelType.TEMPERATURE_CELSIUS, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.TEMPERATURE_FAHRENHEIT, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.PERCENTAGE, AttributeValueType.PERCENTAGE);
        typeMap.put(ChannelType.LUMINANCE_PERCENTAGE, AttributeValueType.PERCENTAGE);
        typeMap.put(ChannelType.LUMINANCE_LUX, AttributeValueType.BRIGHTNESS);
        typeMap.put(ChannelType.POWER_WATT, AttributeValueType.POWER);
        typeMap.put(ChannelType.POWER_BTU_H, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.HUMIDITY_PERCENTAGE, AttributeValueType.HUMIDITY);
        typeMap.put(ChannelType.HUMIDITY_ABSOLUTE, AttributeValueType.HUMIDITY);
        typeMap.put(ChannelType.SPEED_MS, AttributeValueType.SPEED);
        typeMap.put(ChannelType.SPEED_MPH, AttributeValueType.SPEED);
        typeMap.put(ChannelType.DIRECTION_DECIMAL_DEGREES, AttributeValueType.DIRECTION);
        typeMap.put(ChannelType.PRESSURE_KPA, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.PRESSURE_IN_HG, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SOLAR_RADIATION_WATT_M2, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.DEW_POINT_CELSIUS, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.DEW_POINT_FAHRENHEIT, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.RAINFALL_MMPH, AttributeValueType.RAINFALL);
        typeMap.put(ChannelType.RAINFALL_INPH, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.TIDE_LEVEL_M, AttributeValueType.DISTANCE);
        typeMap.put(ChannelType.TIDE_LEVEL_FT, AttributeValueType.DISTANCE);
        typeMap.put(ChannelType.WEIGHT_KG, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.WEIGHT_LB, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.VOLTAGE_V, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.VOLTAGE_MV, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.CURRENT_A, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.CURRENT_MA, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.CO2_PPM, AttributeValueType.CO2);
        typeMap.put(ChannelType.AIR_FLOW_CMPH, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.AIR_FLOW_CFTPM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.TANK_CAPACITY_L, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.TANK_CAPACITY_CBM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.TANK_CAPACITY_GAL, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.DISTANCE_M, AttributeValueType.DISTANCE);
        typeMap.put(ChannelType.DISTANCE_CM, AttributeValueType.DISTANCE);
        typeMap.put(ChannelType.DISTANCE_FT, AttributeValueType.DISTANCE);
        typeMap.put(ChannelType.ANGLE_POSITION_PERCENT, AttributeValueType.PERCENTAGE);
        typeMap.put(ChannelType.ANGLE_POSITION_DEGREE_NORTH_POLE, AttributeValueType.DIRECTION);
        typeMap.put(ChannelType.ANGLE_POSITION_DEGREE_SOUTH_POLE, AttributeValueType.DIRECTION);
        typeMap.put(ChannelType.ROTATION_HZ, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ROTATION_RPM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.WATER_TEMPERATURE_CELSIUS, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.WATER_TEMPERATURE_FAHRENHEIT, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.SOIL_TEMPERATURE_CELSIUS, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.SOIL_TEMPERATURE_FAHRENHEIT, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.SEISMIC_INTENSITY_MERCALLI, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_INTENSITY_EU_MACROSEISMIC, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_INTENSITY_LIEDU, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_INTENSITY_SHINDO, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_LOCAL, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_MOMENT, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_SURFACE_WAVE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SEISMIC_MAGNITUDE_BODY_WAVE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ULTRAVIOLET_UV_INDEX, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.RESISTIVITY_OHM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.CONDUCTIVITY_SPM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.LOUDNESS_DB, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.LOUDNESS_DBA, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.MOISTURE_PERCENTAGE, AttributeValueType.PERCENTAGE);
        typeMap.put(ChannelType.MOISTURE_VOLUME_WATER_CONTENT, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.MOISTURE_IMPEDANCE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.MOISTURE_WATER_ACTIVITY, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.FREQUENCY_HZ, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.FREQUENCY_KHZ, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.TIME_SECONDS, AttributeValueType.TIMESTAMP);
        typeMap.put(ChannelType.TARGET_TEMPERATUE_CELSIUS, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.TARGET_TEMPERATUE_FAHRENHEIT, AttributeValueType.TEMPERATURE);
        typeMap.put(ChannelType.PARTICULATE_MATTER_2_5_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.PARTICULATE_MATTER_2_5_MCGPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.FORMALDEHYDE_LEVEL_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.RADON_CONCENTRATION_BQPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.RADON_CONCENTRATION_PCIPL, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.METHANE_DENSITY_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.VOLATILE_ORGANIC_COMPOUND_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.VOLATILE_ORGANIC_COMPOUND_PPM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.CO_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.CO_PPM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SOIL_HUMIDITY_PERCENTAGE, AttributeValueType.PERCENTAGE);
        typeMap.put(ChannelType.SOIL_REACTIVITY_PH, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SOIL_SALINITY_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.HEART_RATE_BPM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.BLOOD_PRESSURE_SYSTOLIC, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.BLOOD_PRESSURE_DIASTOLIC, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.MUSCLE_MASS_KG, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.FAT_MASS_KG, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.BONE_MASS_KG, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.TOTAL_BODY_WATER_KG, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.BASIC_METABOLIC_RATE_JOULE, AttributeValueType.ENERGY);
        typeMap.put(ChannelType.BODY_MASS_INDEX, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ACCELERATION_X_MPSS, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ACCELERATION_Y_MPSS, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ACCELERATION_Z_MPSS, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.SMOKE_DENSITY_PERCENTAGE, AttributeValueType.PERCENTAGE);
        typeMap.put(ChannelType.WATER_FLOW_LPH, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.WATER_PRESSURE_KPA, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.RF_SIGNAL_STRENGTH_RSSI, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.RF_SIGNAL_STRENGTH_DBM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.PARTICULATE_MATTER_MOLPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.PARTICULATE_MATTER_MCGPCM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.RESPIRATORY_RATE_BPM, AttributeValueType.NUMBER);

        // COMMAND_CLASS_METER

        // Electric Meter
        typeMap.put(ChannelType.ELECTRIC_METER_ENERGY_KWH, AttributeValueType.ENERGY);
        typeMap.put(ChannelType.ELECTRIC_METER_ENERGY_KVAH, AttributeValueType.ENERGY);
        typeMap.put(ChannelType.ELECTRIC_METER_POWER_W, AttributeValueType.POWER);
        typeMap.put(ChannelType.ELECTRIC_METER_PULSE_COUNT, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ELECTRIC_METER_VOLTAGE_V, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ELECTRIC_METER_CURRENT_A, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ELECTRIC_METER_POWER_FACTOR, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.ELECTRIC_METER_POWER_KVAR, AttributeValueType.POWER);
        typeMap.put(ChannelType.ELECTRIC_METER_ENERGY_KVARH, AttributeValueType.ENERGY);

        // Gas Meter
        typeMap.put(ChannelType.GAS_METER_VOLUME_CM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.GAS_METER_VOLUME_CFT, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.GAS_METER_PULSE_COUNT, AttributeValueType.NUMBER);

        // Water Meter
        typeMap.put(ChannelType.WATER_METER_VOLUME_CM, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.WATER_METER_VOLUME_CFT, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.WATER_METER_VOLUME_GAL, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.WATER_METER_PULSE_COUNT, AttributeValueType.NUMBER);

        // COMMAND_CLASS_COLOR_CONTROL

        typeMap.put(ChannelType.COLOR_WARM_WHITE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_COLD_WHITE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_RED, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_GREEN, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_BLUE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_AMBER, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_CYAN, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_PURPLE, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_INDEXED, AttributeValueType.NUMBER);
        typeMap.put(ChannelType.COLOR_RGB, AttributeValueType.COLOR_RGB);
        typeMap.put(ChannelType.COLOR_ARGB, AttributeValueType.COLOR_ARGB);

        // COMMAND_CLASS_SENSOR_ALARM

        typeMap.put(ChannelType.GENERAL_PURPOSE_ALARM, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.SMOKE_ALARM, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.CO_ALARM, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.CO2_ALARM, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.HEAT_ALARM, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.WATER_LEAK_ALARM, AttributeValueType.BOOLEAN);
        typeMap.put(ChannelType.FIRST_SUPPORTED_ALARM, AttributeValueType.BOOLEAN);

        // COMMAND_CLASS_BATTERY

        typeMap.put(ChannelType.CHARGE_PERCENTAGE, AttributeValueType.PERCENTAGE);

        // COMMAND_CLASS_CLOCK

        typeMap.put(ChannelType.DATETIME, AttributeValueType.TIMESTAMP_ISO8601);



        // Basic types

        metaItemMap.put(ChannelType.INTEGER, new ArrayList<>());
        metaItemMap.put(ChannelType.NUMBER, new ArrayList<>());
        metaItemMap.put(ChannelType.STRING, new ArrayList<>());
        metaItemMap.put(ChannelType.BOOLEAN, new ArrayList<>());
        metaItemMap.put(ChannelType.ARRAY, new ArrayList<>());

        // COMMAND_CLASS_SENSOR_MULTILEVEL

        metaItemMap.put(
            ChannelType.TEMPERATURE_CELSIUS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_TEMPERATURE_CELCIUS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f C"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.TEMPERATURE_FAHRENHEIT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("FAHRENHEIT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f F"))
                }
            )
        );
        metaItemMap.put(ChannelType.PERCENTAGE, new ArrayList<>());
        metaItemMap.put(ChannelType.LUMINANCE_PERCENTAGE, new ArrayList<>());
        metaItemMap.put(
            ChannelType.LUMINANCE_LUX,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("LUX")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%d lx"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.POWER_WATT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("WATT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f W"))
                }
            )
        );
        metaItemMap.put(ChannelType.POWER_BTU_H, new ArrayList<>());
        metaItemMap.put(
            ChannelType.HUMIDITY_PERCENTAGE,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("PERCENTAGE")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%3d %%"))
                }
            )
        );
        metaItemMap.put(ChannelType.HUMIDITY_ABSOLUTE, new ArrayList<>());
        metaItemMap.put(
            ChannelType.SPEED_MS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_SPEED_MPS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.3f m/s"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.SPEED_MPH,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("SPEED_MPH")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f mi/h"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.DIRECTION_DECIMAL_DEGREES,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_DIRECTION_DEGREES)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f deg"))
                }
            )
        );
        metaItemMap.put(ChannelType.PRESSURE_KPA, new ArrayList<>());
        metaItemMap.put(ChannelType.PRESSURE_IN_HG, new ArrayList<>());
        metaItemMap.put(ChannelType.SOLAR_RADIATION_WATT_M2, new ArrayList<>());
        metaItemMap.put(
            ChannelType.DEW_POINT_CELSIUS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_TEMPERATURE_CELCIUS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f C"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.DEW_POINT_FAHRENHEIT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("FAHRENHEIT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f F"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.RAINFALL_MMPH,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("RAINFALL_MMPH")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f mm/h"))
                }
            )
        );
        metaItemMap.put(ChannelType.RAINFALL_INPH, new ArrayList<>());
        metaItemMap.put(
            ChannelType.TIDE_LEVEL_M,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("TIDE_LEVEL_M")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f m"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.TIDE_LEVEL_FT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("TIDE_LEVEL_FT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f '"))
                }
            )
        );
        metaItemMap.put(ChannelType.WEIGHT_KG, new ArrayList<>());
        metaItemMap.put(ChannelType.WEIGHT_LB, new ArrayList<>());
        metaItemMap.put(ChannelType.VOLTAGE_V, new ArrayList<>());
        metaItemMap.put(ChannelType.VOLTAGE_MV, new ArrayList<>());
        metaItemMap.put(ChannelType.CURRENT_A, new ArrayList<>());
        metaItemMap.put(ChannelType.CURRENT_MA, new ArrayList<>());
        metaItemMap.put(
            ChannelType.CO2_PPM,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("CO2_PPM")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%4d ppm"))
                }
            )
        );
        metaItemMap.put(ChannelType.AIR_FLOW_CMPH, new ArrayList<>());
        metaItemMap.put(ChannelType.AIR_FLOW_CFTPM, new ArrayList<>());
        metaItemMap.put(ChannelType.TANK_CAPACITY_L, new ArrayList<>());
        metaItemMap.put(ChannelType.TANK_CAPACITY_CBM, new ArrayList<>());
        metaItemMap.put(ChannelType.TANK_CAPACITY_GAL, new ArrayList<>());
        metaItemMap.put(
            ChannelType.DISTANCE_M,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("METERS")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f m"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.DISTANCE_CM,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("CENTIMETERS")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f cm"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.DISTANCE_FT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("FEET")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f '"))
                }
            )
        );
        metaItemMap.put(ChannelType.ANGLE_POSITION_PERCENT, new ArrayList<>());
        metaItemMap.put(
            ChannelType.ANGLE_POSITION_DEGREE_NORTH_POLE,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_DIRECTION_DEGREES)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f deg"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.ANGLE_POSITION_DEGREE_SOUTH_POLE,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_DIRECTION_DEGREES)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f deg"))
                }
            )
        );
        metaItemMap.put(ChannelType.ROTATION_HZ, new ArrayList<>());
        metaItemMap.put(ChannelType.ROTATION_RPM, new ArrayList<>());
        metaItemMap.put(
            ChannelType.WATER_TEMPERATURE_CELSIUS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_TEMPERATURE_CELCIUS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f C"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.WATER_TEMPERATURE_FAHRENHEIT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("FAHRENHEIT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f F"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.SOIL_TEMPERATURE_CELSIUS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_TEMPERATURE_CELCIUS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f C"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.SOIL_TEMPERATURE_FAHRENHEIT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("FAHRENHEIT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f F"))
                }
            )
        );
        metaItemMap.put(ChannelType.SEISMIC_INTENSITY_MERCALLI, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_INTENSITY_EU_MACROSEISMIC, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_INTENSITY_LIEDU, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_INTENSITY_SHINDO, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_MAGNITUDE_LOCAL, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_MAGNITUDE_MOMENT, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_MAGNITUDE_SURFACE_WAVE, new ArrayList<>());
        metaItemMap.put(ChannelType.SEISMIC_MAGNITUDE_BODY_WAVE, new ArrayList<>());
        metaItemMap.put(ChannelType.ULTRAVIOLET_UV_INDEX, new ArrayList<>());
        metaItemMap.put(ChannelType.RESISTIVITY_OHM, new ArrayList<>());
        metaItemMap.put(ChannelType.CONDUCTIVITY_SPM, new ArrayList<>());
        metaItemMap.put(
            ChannelType.LOUDNESS_DB,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_SOUND_DECIBELS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%d dB"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.LOUDNESS_DBA,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_SOUND_DECIBELS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%d dBA"))
                }
            )
        );
        metaItemMap.put(ChannelType.MOISTURE_PERCENTAGE, new ArrayList<>());
        metaItemMap.put(ChannelType.MOISTURE_VOLUME_WATER_CONTENT, new ArrayList<>());
        metaItemMap.put(ChannelType.MOISTURE_IMPEDANCE, new ArrayList<>());
        metaItemMap.put(ChannelType.MOISTURE_WATER_ACTIVITY, new ArrayList<>());
        metaItemMap.put(ChannelType.FREQUENCY_HZ, new ArrayList<>());
        metaItemMap.put(ChannelType.FREQUENCY_KHZ, new ArrayList<>());
        metaItemMap.put(
            ChannelType.TIME_SECONDS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("SECONDS")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%d s"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.TARGET_TEMPERATUE_CELSIUS,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue(Constants.UNITS_TEMPERATURE_CELCIUS)),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f C"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.TARGET_TEMPERATUE_FAHRENHEIT,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("FAHRENHEIT")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.1f F"))
                }
            )
        );
        metaItemMap.put(ChannelType.PARTICULATE_MATTER_2_5_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.PARTICULATE_MATTER_2_5_MCGPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.FORMALDEHYDE_LEVEL_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.RADON_CONCENTRATION_BQPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.RADON_CONCENTRATION_PCIPL, new ArrayList<>());
        metaItemMap.put(ChannelType.METHANE_DENSITY_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.VOLATILE_ORGANIC_COMPOUND_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.VOLATILE_ORGANIC_COMPOUND_PPM, new ArrayList<>());
        metaItemMap.put(ChannelType.CO_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.CO_PPM, new ArrayList<>());
        metaItemMap.put(ChannelType.SOIL_HUMIDITY_PERCENTAGE, new ArrayList<>());
        metaItemMap.put(ChannelType.SOIL_REACTIVITY_PH, new ArrayList<>());
        metaItemMap.put(ChannelType.SOIL_SALINITY_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.HEART_RATE_BPM, new ArrayList<>());
        metaItemMap.put(ChannelType.BLOOD_PRESSURE_SYSTOLIC, new ArrayList<>());
        metaItemMap.put(ChannelType.BLOOD_PRESSURE_DIASTOLIC, new ArrayList<>());
        metaItemMap.put(ChannelType.MUSCLE_MASS_KG, new ArrayList<>());
        metaItemMap.put(ChannelType.FAT_MASS_KG, new ArrayList<>());
        metaItemMap.put(ChannelType.BONE_MASS_KG, new ArrayList<>());
        metaItemMap.put(ChannelType.TOTAL_BODY_WATER_KG, new ArrayList<>());
        metaItemMap.put(
            ChannelType.BASIC_METABOLIC_RATE_JOULE,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("JOULE")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f J"))
                }
            )
        );
        metaItemMap.put(ChannelType.BODY_MASS_INDEX, new ArrayList<>());
        metaItemMap.put(ChannelType.ACCELERATION_X_MPSS, new ArrayList<>());
        metaItemMap.put(ChannelType.ACCELERATION_Y_MPSS, new ArrayList<>());
        metaItemMap.put(ChannelType.ACCELERATION_Z_MPSS, new ArrayList<>());
        metaItemMap.put(ChannelType.SMOKE_DENSITY_PERCENTAGE, new ArrayList<>());
        metaItemMap.put(ChannelType.WATER_FLOW_LPH, new ArrayList<>());
        metaItemMap.put(ChannelType.WATER_PRESSURE_KPA, new ArrayList<>());
        metaItemMap.put(ChannelType.RF_SIGNAL_STRENGTH_RSSI, new ArrayList<>());
        metaItemMap.put(ChannelType.RF_SIGNAL_STRENGTH_DBM, new ArrayList<>());
        metaItemMap.put(ChannelType.PARTICULATE_MATTER_MOLPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.PARTICULATE_MATTER_MCGPCM, new ArrayList<>());
        metaItemMap.put(ChannelType.RESPIRATORY_RATE_BPM, new ArrayList<>());

        // COMMAND_CLASS_METER

        // Electric Meter
        metaItemMap.put(
            ChannelType.ELECTRIC_METER_ENERGY_KWH,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("ENERGY_KWH")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f KWh"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.ELECTRIC_METER_ENERGY_KVAH,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("ENERGY_KVAH")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f KVAh"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.ELECTRIC_METER_POWER_W,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("POWER_W")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f W"))
                }
            )
        );
        metaItemMap.put(ChannelType.ELECTRIC_METER_PULSE_COUNT, new ArrayList<>());
        metaItemMap.put(ChannelType.ELECTRIC_METER_VOLTAGE_V, new ArrayList<>());
        metaItemMap.put(ChannelType.ELECTRIC_METER_CURRENT_A, new ArrayList<>());
        metaItemMap.put(ChannelType.ELECTRIC_METER_POWER_FACTOR, new ArrayList<>());
        metaItemMap.put(
            ChannelType.ELECTRIC_METER_POWER_KVAR,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("POWER_KVAR")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f W"))
                }
            )
        );
        metaItemMap.put(
            ChannelType.ELECTRIC_METER_ENERGY_KVARH,
            Arrays.asList(
                new MetaItem[] {
                    new MetaItem(MetaItemType.UNIT_TYPE.withInitialValue("ENERGY_KVARH")),
                    new MetaItem(MetaItemType.FORMAT.withInitialValue("%0.5f KW"))
                }
            )
        );

        // Gas Meter
        metaItemMap.put(ChannelType.GAS_METER_VOLUME_CM, new ArrayList<>());
        metaItemMap.put(ChannelType.GAS_METER_VOLUME_CFT, new ArrayList<>());
        metaItemMap.put(ChannelType.GAS_METER_PULSE_COUNT, new ArrayList<>());

        // Water Meter
        metaItemMap.put(ChannelType.WATER_METER_VOLUME_CM, new ArrayList<>());
        metaItemMap.put(ChannelType.WATER_METER_VOLUME_CFT, new ArrayList<>());
        metaItemMap.put(ChannelType.WATER_METER_VOLUME_GAL, new ArrayList<>());
        metaItemMap.put(ChannelType.WATER_METER_PULSE_COUNT, new ArrayList<>());

        // COMMAND_CLASS_COLOR_CONTROL

        metaItemMap.put(ChannelType.COLOR_WARM_WHITE, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_COLD_WHITE, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_RED, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_GREEN, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_BLUE, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_AMBER, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_CYAN, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_PURPLE, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_INDEXED, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_RGB, new ArrayList<>());
        metaItemMap.put(ChannelType.COLOR_ARGB, new ArrayList<>());

        // COMMAND_CLASS_SENSOR_ALARM

        metaItemMap.put(ChannelType.GENERAL_PURPOSE_ALARM, new ArrayList<>());
        metaItemMap.put(ChannelType.SMOKE_ALARM, new ArrayList<>());
        metaItemMap.put(ChannelType.CO_ALARM, new ArrayList<>());
        metaItemMap.put(ChannelType.CO2_ALARM, new ArrayList<>());
        metaItemMap.put(ChannelType.HEAT_ALARM, new ArrayList<>());
        metaItemMap.put(ChannelType.WATER_LEAK_ALARM, new ArrayList<>());
        metaItemMap.put(ChannelType.FIRST_SUPPORTED_ALARM, new ArrayList<>());

        // COMMAND_CLASS_BATTERY

        metaItemMap.put(ChannelType.CHARGE_PERCENTAGE, new ArrayList<>());

        // COMMAND_CLASS_CLOCK

        metaItemMap.put(ChannelType.DATETIME, new ArrayList<>());


    }

    static public AttributeValueDescriptor toAttributeType(ChannelType channelType) {

        AttributeValueDescriptor attributeType = AttributeValueType.STRING;

        if (typeMap.containsKey(channelType)) {
            attributeType = typeMap.get(channelType);
        } else {
            switch(channelType.getValueType()) {
                case INTEGER:
                    attributeType = AttributeValueType.NUMBER;
                    break;
                case NUMBER:
                    attributeType = AttributeValueType.NUMBER;
                    break;
                case BOOLEAN:
                    attributeType = AttributeValueType.BOOLEAN;
                    break;
                case STRING:
                    attributeType = AttributeValueType.STRING;
                    break;
                case ARRAY:
                    attributeType = AttributeValueType.ARRAY;
                    break;
            }
        }
        return attributeType;
    }

    static public List<MetaItem> toMetaItems(ChannelType channelType) {
        if (metaItemMap.containsKey(channelType)) {
            return metaItemMap.get(channelType);
        } else {
            return new ArrayList<>();
        }
    }
}
