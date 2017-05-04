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
package org.openremote.model.attribute;

import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Optional;
import java.util.function.Function;

/**
 * The type of an {@link Attribute}, how its {@link Value} should be
 * interpreted when working with an attribute (e.g. when testing, rendering,
 * or editing the value).
 * <p>
 * Additional constraints and integrity rules upon attribute values can be declared by
 * adding arbitrary {@link Meta} to an {@link Attribute}.
 */
public enum AttributeType {

    // TODO Implement validator functions!

    STRING("String", ValueType.STRING, value -> Optional.empty()),

    INTEGER("Integer", ValueType.NUMBER, value -> Optional.empty()),

    DECIMAL("Decimal", ValueType.NUMBER, value -> Optional.empty()),

    BOOLEAN("Boolean", ValueType.BOOLEAN, value -> Optional.empty()),

    PERCENTAGE("Percentage", ValueType.NUMBER, value -> Optional.empty()),

    TIMESTAMP_MILLIS("Timestamp milliseconds", ValueType.NUMBER, value -> Optional.empty()),

    DATETIME("Timestamp in ISO 8601", ValueType.STRING, value -> Optional.empty()),

    COLOR_RGB("Color RGB", ValueType.ARRAY, value -> Optional.empty()),

    COLOR_ARGB("Color ARGB", ValueType.ARRAY, value -> Optional.empty()),

    COLOR_HEX("Color HEX", ValueType.STRING, value -> Optional.empty()),

    TEMPERATURE_CELCIUS("Temperature in Celcius", ValueType.NUMBER, value -> Optional.empty()),

    TEMPERATURE_KELVIN("Temperature in Kelvin", ValueType.NUMBER, value -> Optional.empty()),

    TEMPERATURE_FAHRENHEIT("Temperature in Fahrenheit", ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_M("Distance in meters", ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_CM("Distance in centimeters", ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_MM("Distance in millimeters", ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_IN("Distance in inch", ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_FT("Distance in feet", ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_YARD("Dinstance in yard", ValueType.NUMBER, value -> Optional.empty()),

    CO2_PPM("CO2 Level in PPM", ValueType.NUMBER, value -> Optional.empty()),

    HUMIDITY_PERCENTAGE("Humidity percentage", ValueType.NUMBER, value -> Optional.empty()),

    HUMIDITY_GPCM("Humidity in grams per cubic meter", ValueType.NUMBER, value -> Optional.empty()),

    POWER_WATT("Power in watt", ValueType.NUMBER, value -> Optional.empty()),

    POWER_KILOWATT("Power in kilowatt", ValueType.NUMBER, value -> Optional.empty()),

    POWER_MEGAWATT("Power in megawatt", ValueType.NUMBER, value -> Optional.empty()),

    CHARGE_PERCENTAGE("Charge percentage", ValueType.NUMBER, value -> Optional.empty()),

    CHARGE_KWH("Charge in kilowatt-hours", ValueType.NUMBER, value -> Optional.empty()),

    ENERGY_KWH("Energy in kilowatt-hours", ValueType.NUMBER, value -> Optional.empty()),

    ENERGY_JOULE("Energy in joule", ValueType.NUMBER, value -> Optional.empty()),

    ENERGY_MEGAJOULE("Energy in megajoule", ValueType.NUMBER, value -> Optional.empty()),

    FLOW_LPM("Flow in litre per minute", ValueType.NUMBER, value -> Optional.empty()),

    FLOW_CMPS("Flow in cubic metres per second", ValueType.NUMBER, value -> Optional.empty()),

    FLOW_SCCM("Flow in cubic centimetres per minute", ValueType.NUMBER, value -> Optional.empty()),

    FLOW_CFPS("Flow in cubic feet per second", ValueType.NUMBER, value -> Optional.empty()),

    FLOW_GPM("Flow in gallons per minute", ValueType.NUMBER, value -> Optional.empty());

    protected String displayName;
    protected ValueType valueType;
    protected Function<Value, Optional<String>> validator;

    AttributeType(String displayName, ValueType valueType, Function<Value, Optional<String>> validator) {
        this.displayName = displayName;
        this.valueType = valueType;

        this.validator = value -> {
            // Always perform some basic validation
            if (value != null && getValueType() != value.getType())
                return Optional.of("Value is " + value.getType() + ", but must be " + getValueType());

            // Custom attribute type validation
            return validator.apply(value);
        };
    }

    public String getDisplayName() {
        return displayName;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean isValid(Value value) {
        return !getValidationFailure(value).isPresent();
    }
    public Optional<String> getValidationFailure(Value value)  {
        return validator.apply(value);
    }

    public static Optional<AttributeType> optionalValueOf(String name) {
        try {
            return Optional.of(valueOf(name));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
