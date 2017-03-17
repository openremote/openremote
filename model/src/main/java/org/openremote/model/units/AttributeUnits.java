/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.units;

/**
 * The well-known attribute units (attribute sub-type) we can depend on
 * in our platform. Together with the {@link org.openremote.model.AttributeType}
 * of an {@link org.openremote.model.Attribute}, you can use units in attribute
 * metadata to discriminate rendering, testing, and editing of attribute values.
 */
public enum AttributeUnits {

    /**
     * Unix epoch timestamp
     */
    TIMESTAMP,

    /**
     * ISO 8601, e.g. <code>2012-04-23T18:25:43.511Z</code>
     */
    DATETIME,

    /**
     * RGB integers
     */
    COLOR_RGB,

    /**
     * ARGB integers
     */
    COLOR_ARGB,

    /**
     * HTML Hex Value, e.g. <code>#ffffff</code>
     */
    COLOR_HEX,

    TEMPERATURE_CELCIUS,

    TEMPERATURE_KELVIN,

    TEMPERATURE_FAHRENHEIT,

    DISTANCE_M,

    DISTANCE_CM,

    DISTANCE_MM,

    DISTANCE_IN,

    DISTANCE_FT,

    DISTANCE_YARD,

    CO2_PPM,

    CO2_PERCENTAGE,

    HUMIDITY_PERCENTAGE,

    /**
     * Grams per cubic metre
     */
    HUMIDITY_GPCM,

    POWER_WATTS,

    POWER_KILOWATT,

    POWER_MEGAWATT,

    CHARGE_PERCENTAGE,

    CHARGE_KWH,

    PERCENTAGE,

    ENERGY_KWH,

    ENERGY_JOULE,

    ENERGY_MEGAJOULE,

    /**
     * Litre per minute
     */
    FLOW_LPM,

    /**
     * Cubic metres per second
     */
    FLOW_CMPS,

    /**
     * Standard cubic centimetres per minute
     */
    FLOW_SCCM,

    /**
     * Cubic feet per second
     */
    FLOW_CFPS,

    /**
     * Gallons per minute
     */
    FLOW_GPM,
}
