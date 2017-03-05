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
package org.openremote.model;

public enum AttributeUnits {
    TIMESTAMP, // Unix epoch timestamp
    DATETIME, // ISO 8601, e.g. "2012-04-23T18:25:43.511Z"
    COLOR_RGB, // RGB integers
    COLOR_ARGB, // ARGB integers
    COLOR_HEX, // Hex value e.g. #FFFFFF
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
    HUMIDITY_GPCM,          // Grams per cubic metre
    POWER_WATTS,
    POWER_KILOWATT,
    POWER_MEGAWATT,
    CHARGE_PERCENTAGE,
    CHARGE_KWH,
    PERCENTAGE,
    ENERGY_KWH,
    ENERGY_JOULE,
    ENERGY_MEGAJOULE,
    FLOW_LPM,               // Litre per minute
    FLOW_CMPS,              // Cubic metres per second
    FLOW_SCCM,               // Standard cubic centimetres per minute
    FLOW_CFPS,               // Cubic feet per second
    FLOW_GPM,               // Gallons per minute
}
