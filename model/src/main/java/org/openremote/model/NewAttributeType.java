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

import elemental.json.JsonValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.function.Predicate;

/**
 * The base type of an {@link Attribute}, how its {@link JsonValue} should be
 * interpreted when working with an attribute (e.g. when testing, rendering, or editing
 * the value).
 *
 * Additional constraints and integrity rules upon attribute values can be declared by
 * adding arbitrary {@link Meta} to an {@link Attribute}.
 */
public enum NewAttributeType {
    // TODO: Implement new attribute types
    STRING("String", ValueType.STRING),
    INTEGER("Integer", ValueType.NUMBER),
    DECIMAL("Decimal", ValueType.NUMBER),
    BOOLEAN("Boolean", ValueType.BOOLEAN);

/*    *//**
     * Unix epoch timestamp
     *//*
    // TODO: Seconds or milliseconds
    TIMESTAMP("Timestamp", ValueType.NUMBER),*/
/*
    *//**
     * ISO 8601, e.g. <code>2012-04-23T18:25:43.511Z</code>
     *//*
    DATETIME,

    *//**
     * RGB integers
     *//*
    COLOR_RGB,

    *//**
     * ARGB integers
     *//*
    COLOR_ARGB,

    *//**
     * HTML Hex Value, e.g. <code>#ffffff</code>
     *//*
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

    *//**
     * Grams per cubic metre
     *//*
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

    *//**
     * Litre per minute
     *//*
    FLOW_LPM,

    *//**
     * Cubic metres per second
     *//*
    FLOW_CMPS,

    *//**
     * Standard cubic centimetres per minute
     *//*
    FLOW_SCCM,

    *//**
     * Cubic feet per second
     *//*
    FLOW_CFPS,

    *//**
     * Gallons per minute
     *//*
    FLOW_GPM;*/

    private String displayName;
    private ValueType valueType;
    private Predicate<Value> isValid;

    NewAttributeType(String displayName, ValueType valueType) {
        this(displayName, valueType, null);
    }

    NewAttributeType(String displayName, ValueType valueType, Predicate<Value> isValid) {
        this.displayName = displayName;
        this.valueType = valueType;
        this.isValid = isValid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean isValid(Value value) {
        // Check type
        if (value == null) {
            // TODO: Should we allow nulls for any type?
            return true;
        }

        return value.getType() == getValueType() && (isValid == null || isValid.test(value));
    }

    public static NewAttributeType fromValue(String value) {
        if (value != null && !"".equals(value)) {
            NewAttributeType[] values = NewAttributeType.values();
            for (NewAttributeType v : values) {
                if (v.getDisplayName().equalsIgnoreCase(value))
                    return v;
            }
        }
        return STRING;
    }
}
