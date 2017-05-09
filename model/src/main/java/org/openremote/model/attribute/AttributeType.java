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

import org.openremote.model.ValidationFailure;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Optional;
import java.util.function.Function;

import static org.openremote.model.attribute.AttributeType.AttributeTypeValidationFailure.*;

/**
 * The type of an {@link Attribute}, how its {@link Value} should be
 * interpreted when working with an attribute (e.g. when testing, rendering,
 * or editing the value).
 * <p>
 * Additional constraints and integrity rules upon attribute values can be declared by
 * adding arbitrary {@link Meta} to an {@link Attribute}.
 */
public enum AttributeType {

    // TODO Implement more validator functions!

    STRING(ValueType.STRING, value -> Optional.empty()),

    INTEGER(ValueType.NUMBER, value -> Optional.empty()),

    DECIMAL(ValueType.NUMBER, value -> Optional.empty()),

    BOOLEAN(ValueType.BOOLEAN, value -> Optional.empty()),

    PERCENTAGE(ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(number -> number < 0 || number > 100)
        .map(number -> PERCENTAGE_NOT_WITHIN_BOUNDS)
    ),

    TIMESTAMP_MILLIS(ValueType.NUMBER, value -> Optional.empty()),

    DATETIME(ValueType.STRING, value -> Optional.empty()),

    COLOR_RGB(ValueType.ARRAY, value -> Optional.empty()),

    COLOR_ARGB(ValueType.ARRAY, value -> Optional.empty()),

    COLOR_HEX(ValueType.STRING, value -> Optional.empty()),

    TEMPERATURE_CELCIUS(ValueType.NUMBER, value -> Optional.empty()),

    TEMPERATURE_KELVIN(ValueType.NUMBER, value -> Optional.empty()),

    TEMPERATURE_FAHRENHEIT(ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_M(ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_CM(ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_MM(ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_IN(ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_FT(ValueType.NUMBER, value -> Optional.empty()),

    DISTANCE_YARD(ValueType.NUMBER, value -> Optional.empty()),

    CO2_PPM(ValueType.NUMBER, value -> Optional.empty()),

    HUMIDITY_PERCENTAGE(ValueType.NUMBER, value -> Optional.empty()),

    HUMIDITY_GPCM(ValueType.NUMBER, value -> Optional.empty()),

    POWER_WATT(ValueType.NUMBER, value -> Optional.empty()),

    POWER_KILOWATT(ValueType.NUMBER, value -> Optional.empty()),

    POWER_MEGAWATT(ValueType.NUMBER, value -> Optional.empty()),

    CHARGE_PERCENTAGE(ValueType.NUMBER, value -> Optional.empty()),

    CHARGE_KWH(ValueType.NUMBER, value -> Optional.empty()),

    ENERGY_KWH(ValueType.NUMBER, value -> Optional.empty()),

    ENERGY_JOULE(ValueType.NUMBER, value -> Optional.empty()),

    ENERGY_MEGAJOULE(ValueType.NUMBER, value -> Optional.empty()),

    FLOW_LPM(ValueType.NUMBER, value -> Optional.empty()),

    FLOW_CMPS(ValueType.NUMBER, value -> Optional.empty()),

    FLOW_SCCM(ValueType.NUMBER, value -> Optional.empty()),

    FLOW_CFPS(ValueType.NUMBER, value -> Optional.empty()),

    FLOW_GPM(ValueType.NUMBER, value -> Optional.empty());

    public enum AttributeTypeValidationFailure implements ValidationFailure {
        VALUE_DOES_NOT_MATCH_ATTRIBUTE_TYPE,
        PERCENTAGE_NOT_WITHIN_BOUNDS
    }

    protected ValueType valueType;
    protected Function<Value, Optional<ValidationFailure>> validator;

    AttributeType(ValueType valueType, Function<Value, Optional<ValidationFailure>> validator) {
        this.valueType = valueType;

        this.validator = value -> {
            // Always perform some basic validation
            if (value != null && getValueType() != value.getType())
                return Optional.of(VALUE_DOES_NOT_MATCH_ATTRIBUTE_TYPE);

            // Custom attribute type validation
            return validator.apply(value);
        };
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Optional<ValidationFailure> isValidValue(Value value) {
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
