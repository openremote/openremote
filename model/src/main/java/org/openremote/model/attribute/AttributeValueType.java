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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static org.openremote.model.Constants.*;
import static org.openremote.model.attribute.AttributeValueType.AttributeValueTypeFailureReason.ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH;
import static org.openremote.model.attribute.MetaItemType.*;

/**
 * The type of an {@link Attribute}, how its {@link Value} should be
 * interpreted when working with an attribute (e.g. when testing, rendering,
 * or editing the value).
 * <p>
 * Additional constraints and integrity rules upon attribute values can be declared by
 * adding arbitrary {@link Meta} to an {@link Attribute}.
 */
public enum AttributeValueType implements AttributeValueDescriptor {

    STRING("edit", ValueType.STRING),

    NUMBER("hashtag", ValueType.NUMBER),

    INTEGER("hashtag", ValueType.NUMBER),

    BOOLEAN("toggle-on", ValueType.BOOLEAN),

    SWITCH_TOGGLE("toggle-on", ValueType.BOOLEAN, UNIT_TYPE.withInitialValue(UNITS_ON_OFF)),

    SWITCH_MOMENTARY("toggle-on", ValueType.BOOLEAN, UNIT_TYPE.withInitialValue(UNITS_PRESSED_RELEASED)),

    OBJECT("cube", ValueType.OBJECT),

    ARRAY("bars", ValueType.ARRAY),

    PERCENTAGE("percentage", ValueType.NUMBER,
            value -> Values.getNumber(value)
                    .filter(number -> number < 0 || number > 100)
                    .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
            RANGE_MIN.withInitialValue(Values.create(0)),
            RANGE_MAX.withInitialValue(Values.create(100)),
            STEP.withInitialValue(Values.create(1)),
            UNIT_TYPE.withInitialValue(UNITS_PERCENTAGE),
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

    TIMESTAMP("clock", ValueType.NUMBER),

    TIMESTAMP_ISO8601("clock", ValueType.STRING),

    DURATION("clock", ValueType.NUMBER),

    DURATION_STRING("clock", ValueType.NUMBER),

    COLOR_RGB("palette", ValueType.ARRAY, value -> Values.getArray(value)
        .filter(array -> array.length() != 3)
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    COLOR_RGBW("palette", ValueType.ARRAY, value -> Values.getArray(value)
        .filter(array -> array.length() != 4)
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT)),
        UNIT_TYPE.withInitialValue("")
    ),

    COLOR_ARGB("palette", ValueType.ARRAY, value -> Values.getArray(value)
        .filter(array -> array.length() != 4)
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    COLOR_HEX("palette", ValueType.STRING, value -> Values.getString(value)
        .filter(s -> !s.matches("[a-fA-F0-9]{6}"))
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    SOUND("headphone", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(n -> n < 0)
        .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_SOUND_OUT_OF_RANGE)),
        UNIT_TYPE.withInitialValue(Constants.UNITS_SOUND_DECIBELS),
        FORMAT.withInitialValue(Values.create("%d dB"))
    ),

    TEMPERATURE("temperature-high", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(n -> n < -273.15)
        .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE)),
        UNIT_TYPE.withInitialValue(Constants.UNITS_TEMPERATURE_CELSIUS),
        FORMAT.withInitialValue(Values.create("%0.1f C")
        )
    ),

    CURRENCY("money", ValueType.NUMBER),

    RAINFALL("water", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_DISTANCE_MILLIMETRES)),

    BRIGHTNESS("lightbulb", ValueType.NUMBER,
        STEP.withInitialValue(Values.create(1))),

    DISTANCE("ruler", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_DISTANCE_METRES)),

    SPEED("circle", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_SPEED_METRES_SECOND)),

    CO2("leaf", ValueType.NUMBER),

    HUMIDITY("water", ValueType.NUMBER),

    POWER("bolt", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_POWER_KILOWATT)),

    CHARGE("battery-half", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(number -> number < 0 || number > 100)
        .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
        RANGE_MIN.withInitialValue(Values.create(0)),
        RANGE_MAX.withInitialValue(Values.create(100))
    ),

    ENERGY("plug", ValueType.NUMBER),

    FLOW("dot-circle", ValueType.NUMBER),

    DIRECTION("compass", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_ANGLE_DEGREES)),

    GEO_JSON_POINT("map-marker-alt", ValueType.OBJECT),

    EMAIL("at", ValueType.STRING, value -> Values.getString(value)
        .filter(s -> !s.matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_EMAIL_FORMAT))
    ),

    // TODO Implement "Saved Filter/Searches" properly, see AssetResourceImpl
    RULES_TEMPLATE_FILTER("filter", ValueType.ARRAY),

    ASSET_STATUS("heart-pulse", ValueType.STRING),

    CALENDAR_EVENT("calendar", ValueType.OBJECT),

    EXECUTION_STATUS("play-circle", ValueType.STRING, new MetaItemDescriptorImpl(ALLOWED_VALUES, Values.createArray(Arrays.stream(AttributeExecuteStatus.values()).map(executeStatus -> Values.create(executeStatus.name())).toArray(Value[]::new)))),

    CONNECTION_STATUS("lan-connect", ValueType.STRING, new MetaItemDescriptorImpl(ALLOWED_VALUES, Values.createArray(Arrays.stream(ConnectionStatus.values()).map(connectionStatus -> Values.create(connectionStatus.name())).toArray(Value[]::new)))),

    MASS("weight", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_MASS_KILOGRAM)),

    DENSITY("cube", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_DENSITY_KILOGRAMS_CUBIC_M)),

    RATE("guage", ValueType.NUMBER, UNIT_TYPE.withInitialValue(UNITS_COUNT_PER_SECOND));

    public enum AttributeValueTypeFailureReason implements ValidationFailure.Reason {
        ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH
    }

    public static final String DEFAULT_ICON = "circle";
    final protected String icon;
    final protected ValueType valueType;
    @JsonIgnore
    protected Function<Value, Optional<ValidationFailure>> validator;
    protected MetaItemDescriptor[] metaItemDescriptors;

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

    public AttributeValueDescriptor withUnitType(String units) {
        MetaItemDescriptor unitType = UNIT_TYPE.withInitialValue(units);
        if (unitType.getUrn().equals(UNIT_TYPE.getUrn())) {
            return new AttributeValueDescriptorImpl(getName(), getIcon(), getValueType(), new MetaItemDescriptor[]{unitType});
        } else {
            throw new IllegalArgumentException("Parameter unitType should be of MetaItem type: " + UNIT_TYPE.getUrn());
        }
    }
}
