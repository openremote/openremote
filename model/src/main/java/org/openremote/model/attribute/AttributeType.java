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
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.template.TemplateFilter;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Optional;
import java.util.function.Function;

import static org.openremote.model.asset.AssetMeta.FORMAT;
import static org.openremote.model.attribute.AttributeType.AttributeTypeFailureReason.ATTRIBUTE_TYPE_INVALID_TEMPLATE_FILTER;
import static org.openremote.model.attribute.AttributeType.AttributeTypeFailureReason.ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH;

/**
 * The type of an {@link Attribute}, how its {@link Value} should be
 * interpreted when working with an attribute (e.g. when testing, rendering,
 * or editing the value).
 * <p>
 * Additional constraints and integrity rules upon attribute values can be declared by
 * adding arbitrary {@link Meta} to an {@link Attribute}.
 */
public enum AttributeType {

    STRING("file-text-o", ValueType.STRING, value -> Optional.empty()),

    NUMBER("hashtag", ValueType.NUMBER, value -> Optional.empty()),

    BOOLEAN("toggle-off", ValueType.BOOLEAN, value -> Optional.empty()),

    OBJECT("cubes", ValueType.OBJECT, value -> Optional.empty()),

    ARRAY("ellipsis-h", ValueType.ARRAY, value -> Optional.empty()),

    /**
     * When {@link Ruleset#templateAssetId} references an asset, use the attribute to customize the
     * template when the ruleset is deployed. The attribute name is available in the template as
     * parameter {@link TemplateFilter#TEMPLATE_PARAM_FILTER_NAME}, the attribute value is converted
     * into {@link TemplateFilter} and available in the template as
     * {@link TemplateFilter#TEMPLATE_PARAM_ASSET_STATE} and {@link TemplateFilter#TEMPLATE_PARAM_ASSET_EVENT}.
     */
    RULES_TEMPLATE_FILTER("filter", ValueType.ARRAY, value ->
        TemplateFilter.fromModelValue("test", value).isPresent()
            ? Optional.empty()
            : Optional.of(new ValidationFailure(ATTRIBUTE_TYPE_INVALID_TEMPLATE_FILTER))
    ),

    PERCENTAGE("percent", ValueType.NUMBER,
        value -> Values.getNumber(value)
            .filter(number -> number < 0 || number > 100)
            .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
        new MetaItem(AssetMeta.RANGE_MIN, Values.create(0)),
        new MetaItem(AssetMeta.RANGE_MAX, Values.create(100)),
        new MetaItem(AssetMeta.FORMAT, Values.create("%3d %%"))
    ),

    TIMESTAMP_MILLIS("clock-o", ValueType.NUMBER, value -> Optional.empty()),

    TIME_SECONDS("clock-o", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.1f s"))
    ),

    TIME_MINUTES("clock-o", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.1f min"))
    ),

    DATETIME("calendar", ValueType.STRING, value -> Optional.empty()),

    COLOR_RGB("paint-brush", ValueType.ARRAY, value -> Values.getArray(value)
        .filter(array -> array.length() != 3)
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    COLOR_ARGB("paint-brush", ValueType.ARRAY, value -> Values.getArray(value)
        .filter(array -> array.length() != 4)
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    COLOR_HEX("paint-brush", ValueType.STRING, value -> Values.getString(value)
        .filter(s -> !s.matches("[a-fA-F0-9]{6}"))
        .map(array -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID_COLOR_FORMAT))
    ),

    TEMPERATURE_CELCIUS("thermometer", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(n -> n < -273.15)
        .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE)),
            new MetaItem(FORMAT, Values.create("%0.1f C")
        )
    ),

    TEMPERATURE_KELVIN("thermometer", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(n -> n < 0)
        .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE))
    ),

    TEMPERATURE_FAHRENHEIT("thermometer", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(n -> n < -459.67)
        .map(n -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_TEMPERATURE_OUT_OF_RANGE))
    ),

    RAINFALL("tint", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.1f mm/h"))
    ),

    BRIGHTNESS_LUX("sun-o", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%d lx"))
    ),

    DISTANCE_M("arrows-h", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f m"))
    ),

    DISTANCE_CM("arrows-h", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f cm"))
    ),

    DISTANCE_MM("arrows-h", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f mm"))
    ),

    DISTANCE_IN("arrows-h", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f \""))
    ),

    DISTANCE_FT("arrows-h", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f '"))
    ),

    DISTANCE_YARD("arrows-h", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f yd"))
    ),

    SPEED_MS("rocket", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.3f m/s"))
    ),

    SPEED_KPH("rocket", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.1f km/h"))
    ),

    SPEED_MPH("rocket", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.1f mi/h"))
    ),

    CO2_PPM("leaf", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%4d ppm"))
    ),

    HUMIDITY_PERCENTAGE("tint", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(number -> number < 0 || number > 100)
        .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
        new MetaItem(AssetMeta.RANGE_MIN, Values.create(0)),
        new MetaItem(AssetMeta.RANGE_MAX, Values.create(100)),
        new MetaItem(FORMAT, Values.create("%3d %%"))
    ),

    HUMIDITY_GPCM("tint", ValueType.NUMBER, value -> Optional.empty()),

    POWER_WATT("plug", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f W"))
    ),

    POWER_KILOWATT("plug", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f KW"))),

    POWER_MEGAWATT("plug", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f MW"))),

    CHARGE_PERCENTAGE("battery-full", ValueType.NUMBER, value -> Values.getNumber(value)
        .filter(number -> number < 0 || number > 100)
        .map(number -> new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_PERCENTAGE_OUT_OF_RANGE)),
        new MetaItem(AssetMeta.RANGE_MIN, Values.create(0)),
        new MetaItem(AssetMeta.RANGE_MAX, Values.create(100)),
        new MetaItem(FORMAT, Values.create("%3d %%"))
    ),

    CHARGE_KWH("battery-full", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f KWh"))),

    ENERGY_KWH("plug", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f KWh"))
    ),

    ENERGY_JOULE("plug", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f J"))
    ),

    ENERGY_MEGAJOULE("plug", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f MJ"))),

    FLOW_LPM("tachometer", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f l/m"))
    ),

    FLOW_CMPS("tachometer", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f m³/s"))
    ),

    FLOW_SCCM("tachometer", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f cm³/m"))
    ),

    FLOW_CFPS("tachometer", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f cfs"))
    ),

    FLOW_GPM("tachometer", ValueType.NUMBER, value -> Optional.empty(),
        new MetaItem(FORMAT, Values.create("%0.5f gpm"))
    );

    public static final String DEFAULT_ICON = "circle-thin";

    public enum AttributeTypeFailureReason implements ValidationFailure.Reason {
        ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH,
        ATTRIBUTE_TYPE_INVALID_TEMPLATE_FILTER
    }

    final protected String icon;
    final protected ValueType valueType;
    final protected Function<Value, Optional<ValidationFailure>> validator;
    final protected MetaItem[] defaultMetaItems;

    AttributeType(String icon, ValueType valueType, Function<Value, Optional<ValidationFailure>> validator, MetaItem... defaultMetaItems) {
        this.icon = icon;
        this.valueType = valueType;
        this.validator = value -> {
            // Always perform some basic validation
            if (value != null && getValueType() != value.getType())
                return Optional.of(new ValidationFailure(ATTRIBUTE_TYPE_VALUE_DOES_NOT_MATCH, getValueType().name()));

            // Custom attribute type validation
            return validator.apply(value);
        };
        this.defaultMetaItems = defaultMetaItems;
    }

    public String getIcon() {
        return icon;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public MetaItem[] getDefaultMetaItems() {
        return defaultMetaItems;
    }

    public Optional<ValidationFailure> isValidValue(Value value) {
        return validator.apply(value);
    }
}
