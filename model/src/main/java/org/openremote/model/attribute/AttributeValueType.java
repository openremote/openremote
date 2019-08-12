/*
 * Copyright 2017, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Optional;
import java.util.function.Function;

import static org.openremote.model.attribute.MetaItemType.*;
import static org.openremote.model.attribute.AttributeValueType.AttributeValueTypeFailureReason.ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH;

/**
 * The type of an {@link Attribute}, how its {@link Value} should be
 * interpreted when working with an attribute (e.g. when testing, rendering,
 * or editing the value).
 * <p>
 * Additional constraints and integrity rules upon attribute values can be declared by
 * adding arbitrary {@link Meta} to an {@link Attribute}.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AttributeValueType implements AttributeValueDescriptor {

    STRING("edit", ValueType.STRING),

    NUMBER("hashtag", ValueType.NUMBER),

    INTEGER("hashtag", ValueType.NUMBER),

    BOOLEAN("toggle-on", ValueType.BOOLEAN),

    SWITCH_TOGGLE("toggle-on", ValueType.BOOLEAN),

    SWITCH_MOMENTARY("toggle-on", ValueType.BOOLEAN),

    OBJECT("cube", ValueType.OBJECT),

    ARRAY("bars", ValueType.ARRAY),

    PERCENTAGE("percentage", ValueType.NUMBER,
            value -> Values.getNumber(value)
                    .filter(number -> number < 0 || number > 100)
                    .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
            RANGE_MIN.withInitialValue(Values.create(0)),
            RANGE_MAX.withInitialValue(Values.create(100)),
            FORMAT.withInitialValue(Values.create("%3d %%"))
    ),

    LEVEL_UINT8("ruler-vertical", ValueType.NUMBER,
            value -> Values.getNumber(value)
                    .filter(number -> number < 0 || number > 255)
                    .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID)),
            RANGE_MIN.withInitialValue(Values.create(0)),
            RANGE_MAX.withInitialValue(Values.create(255)),
            FORMAT.withInitialValue(Values.create("%3d"))
    ),

    TIMESTAMP_MILLIS("clock", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%d ms"))),

    TIMESTAMP_SECONDS("clock", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%d s"))),

    TIME_SECONDS("clock", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.1f s"))),

    TIME_MINUTES("clock", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.1f min"))),

    DATETIME("calendar-alt", ValueType.STRING),

    COLOR_RGB("palette", ValueType.ARRAY, value -> Values.getArray(value)
            .filter(array -> array.length() != 3)
            .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    COLOR_ARGB("palette", ValueType.ARRAY, value -> Values.getArray(value)
            .filter(array -> array.length() != 4)
            .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    COLOR_HEX("palette", ValueType.STRING, value -> Values.getString(value)
            .filter(s -> !s.matches("[a-fA-F0-9]{6}"))
            .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    SOUND_DB("headphone", ValueType.NUMBER, value -> Values.getNumber(value)
            .filter(n -> n < 0)
            .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_SOUND_OUT_OF_RANGE)),
            FORMAT.withInitialValue(Values.create("%d dB"))
    ),

    TEMPERATURE_CELCIUS("temperature-high", ValueType.NUMBER, value -> Values.getNumber(value)
            .filter(n -> n < -273.15)
            .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE)),
            FORMAT.withInitialValue(Values.create("%0.1f C")
            )
    ),

    TEMPERATURE_KELVIN("temperature-high", ValueType.NUMBER, value -> Values.getNumber(value)
            .filter(n -> n < 0)
            .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE))
    ),

    TEMPERATURE_FAHRENHEIT("temperature-high", ValueType.NUMBER, value -> Values.getNumber(value)
            .filter(n -> n < -459.67)
            .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE))
    ),

    RAINFALL("water", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.1f mm/h"))),

    BRIGHTNESS_LUX("lightbulb", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%d lx"))),

    DISTANCE_M("ruler", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f m"))),

    DISTANCE_CM("ruler", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f cm"))),

    DISTANCE_MM("ruler", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f mm"))),

    DISTANCE_IN("ruler", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f \""))),

    DISTANCE_FT("ruler", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f '"))),

    DISTANCE_YARD("ruler", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f yd"))),

    SPEED_MS("circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.3f m/s"))),

    SPEED_KPH("circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.1f km/h"))),

    SPEED_MPH("circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.1f mi/h"))),

    CO2_PPM("leaf", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%4d ppm"))),

    HUMIDITY_PERCENTAGE("water", ValueType.NUMBER, value -> Values.getNumber(value)
            .filter(number -> number < 0 || number > 100)
            .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
            RANGE_MIN.withInitialValue(Values.create(0)),
            RANGE_MAX.withInitialValue(Values.create(100)),
            FORMAT.withInitialValue(Values.create("%3d %%"))
    ),

    HUMIDITY_GPCM("water", ValueType.NUMBER),

    POWER_WATT("bolt", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f W"))),

    POWER_KILOWATT("bolt", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f KW"))),

    POWER_MEGAWATT("bolt", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f MW"))),

    POWER_PERCENTAGE("bolt", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%3d %%"))),

    CHARGE_PERCENTAGE("battery-half", ValueType.NUMBER, value -> Values.getNumber(value)
            .filter(number -> number < 0 || number > 100)
            .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
            RANGE_MIN.withInitialValue(Values.create(0)),
            RANGE_MAX.withInitialValue(Values.create(100)),
            FORMAT.withInitialValue(Values.create("%3d %%"))
    ),

    CHARGE_KWH("battery-half", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f KWh"))),

    ENERGY_KWH("plug", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f KWh"))),

    ENERGY_JOULE("plug", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f J"))),

    ENERGY_MEGAJOULE("plug", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f MJ"))),

    FLOW_LPM("dot-circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f L/m"))),

    FLOW_CMPS("dot-circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f m³/s"))),

    FLOW_CCMPS("dot-circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f cm³/s"))),

    FLOW_CFPS("dot-circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f cfs"))),

    FLOW_GPM("dot-circle", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.5f gpm"))),

    DIRECTION_DECIMAL_DEGREES("compass", ValueType.NUMBER, FORMAT.withInitialValue(Values.create("%0.1f deg"))),

    GEO_JSON_POINT("map-marker-alt", ValueType.OBJECT),

    EMAIL("at", ValueType.STRING, value -> Values.getString(value)
            .filter(s -> !s.matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
            .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_EMAIL_FORMAT))
    ),

    // TODO Implement "Saved Filter/Searches" properly, see AssetResourceImpl
    RULES_TEMPLATE_FILTER("filter", ValueType.ARRAY);


    public enum AttributeValueTypeFailureReason implements ValidationFailure.Reason {
        ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH
    }
    public static final String DEFAULT_ICON = "circle";
    final protected String icon;
    final protected ValueType valueType;
    @JsonIgnore
    final protected Function<Value, Optional<ValidationFailure>> validator;
    final protected MetaItemDescriptor[] metaItemDescriptors;

    AttributeValueType(String icon, ValueType valueType, MetaItemDescriptor... metaItemDescriptors) {
        this(icon, valueType, null, metaItemDescriptors);
    }

    AttributeValueType(String icon, ValueType valueType, Function<Value, Optional<ValidationFailure>> validator, MetaItemDescriptor... metaItemDescriptors) {
        this.icon = icon;
        this.valueType = valueType;
        this.validator = value -> {
            // Always perform some basic validation
            if (value != null && getValueType() != value.getType())
                return Optional.of(new ValidationFailure(ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH, getValueType().name()));

            // Custom attribute type validation
            if (validator != null) {
                return validator.apply(value);
            }

            return Optional.empty();
        };
        this.metaItemDescriptors = metaItemDescriptors;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public MetaItemDescriptor[] getMetaItemDescriptors() {
        return metaItemDescriptors;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public Optional<Function<Value, Optional<ValidationFailure>>> getValidator() {
        return Optional.ofNullable(validator);
    }
}
