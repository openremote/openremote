/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.StdConverter;
import jakarta.validation.constraints.Pattern;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.TsIgnoreTypeParams;
import org.openremote.model.util.ValueUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * A simple wrapper around a {@link Class} that describes a value that can be used by {@link Attribute}s and
 * {@link MetaItem}s; it can also store {@link ValueConstraint} and {@link ValueFormat} information.
 * <p>
 * The {@link ValueDescriptor} applies to the {@link Asset} type it is associated with and all subtypes of this type (i.e. a
 * {@link ValueDescriptor} associated with the base {@link Asset} type will be available to all {@link  Asset} types (e.g.
 * {@link ValueType#NUMBER} can be applied to any {@link org.openremote.model.asset.Asset}'s {@link Attribute} and/or
 * {@link MetaItemDescriptor}).
 * <p>
 * {@link ValueDescriptor}s for arrays don't need to be explicitly defined but can be obtained at the point of consumption
 * by simply calling {@link ValueDescriptor#asArray}.
 * <p>
 * {@link ValueDescriptor#getName} must be globally unique within the context of the manager it is registered with.
 */
@TsIgnoreTypeParams
public class ValueDescriptor<T> implements NameHolder, Serializable {

    /**
     * This class handles serialising {@link ValueDescriptor}s as strings
     */
    public static class ValueDescriptorStringConverter extends StdConverter<ValueDescriptor<?>, String> {

        @Override
        public String convert(ValueDescriptor<?> value) {
            return value.getName();
        }
    }

    /**
     * This class handles deserialising value descriptor names to {@link ValueDescriptor}s
     */
    public static class StringValueDescriptorConverter extends StdConverter<String, ValueDescriptor<?>> {

        @Override
        public ValueDescriptor<?> convert(String value) {
            return ValueUtil.getValueDescriptor(value).orElse(null);
        }
    }

    /**
     * Just outputs simple class name for compactness but can still prove useful for discrimination purposes on UIs
     */
    public static class ClassStringConverter extends StdConverter<Class<?>, String> {
        
        @Override
        public String convert(Class<?> clazz) {
            return clazz.getSimpleName();
        }
    }

    @Pattern(regexp = "^\\w+(\\[\\])?$")
    protected String name;
    protected Class<T> type;
    protected Integer arrayDimensions;
    protected ValueConstraint[] constraints;
    protected ValueFormat format;
    protected String[] units;
    /**
     * A flag to indicate that this {@link ValueDescriptor} is intended for use in a {@link MetaItem}; this can be used
     * for UI purposes but nothing actually stops the values being used in an {@link Attribute} also.
     */
    protected Boolean metaUseOnly;

    @SuppressWarnings("unchecked")
    public ValueDescriptor(String name, Class<T> type, ValueConstraint...constraints) {
        if (type.isArray()) {
            throw new IllegalArgumentException("Value descriptor type should be the inner array type");
        }
        this.name = name;
        this.type = type;
        if (type.isEnum() && (constraints == null || Arrays.stream(constraints).noneMatch(c -> c instanceof ValueConstraint.AllowedValues))) {
            ValueConstraint.AllowedValues allowedValues = ValueConstraint.AllowedValues.fromEnumValues((Enum[])type.getEnumConstants());
            if (constraints != null) {
                constraints = Arrays.copyOf(constraints, constraints.length+1);
                constraints[constraints.length-1] = allowedValues;
            } else {
                constraints = new ValueConstraint[] {allowedValues};
            }
        }
        this.constraints = constraints;
    }

    @JsonCreator
    protected ValueDescriptor(String name, Class<T> type, ValueConstraint[] constraints, ValueFormat format, String[] units, Integer arrayDimensions) {
        this.name = name;
        this.type = type;
        this.arrayDimensions = arrayDimensions;
        this.constraints = constraints;
        this.format = format;
        this.units = units;
    }

    @Pattern(regexp = "^\\w+(\\[\\])?$")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return type != null ? type : (Class<T>)Object.class;
    }

    /**
     * If this descriptor is an array type then this will get the inner type of the array(s)
     */
    public Class<?> getBaseType() {
        Class<?> clazz = getType();
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    /**
     * Will get the JSON type of the inner most type; can be used in combination with {@link #arrayDimensions} to
     * know the structure of this value descriptor
     */
    @JsonProperty
    public String getJsonType() {
        Class<?> type = getBaseType();

        if (ValueUtil.isBoolean(type)) {
            return "boolean";
        }
        if (ValueUtil.isNumber(type)) {
            if (BigInteger.class.isAssignableFrom(type)) {
                return "bigint";
            }
            return "number";
        }
        if (ValueUtil.isString(type) || type.isEnum()) {
            return "string";
        }
        if (ValueUtil.isArray(type)) {
            return "array";
        }
        if (Date.class.isAssignableFrom(type)) {
            return "date";
        }
        if (type == Object.class) {
            return "unknown";
        }
        return "object";
    }

    public ValueDescriptor<T> withFormat(ValueFormat format) {
        return new ValueDescriptor<>(name, type, constraints, format, units, arrayDimensions);
    }

    public ValueDescriptor<T> withConstraints(ValueConstraint...constraints) {
        return new ValueDescriptor<>(name, type, constraints, format, units, arrayDimensions);
    }

    public ValueDescriptor<T> withUnits(String...units) {
        return new ValueDescriptor<>(name, type, constraints, format, units, arrayDimensions);
    }

    public ValueDescriptor<T> forMetaUseOnly() {
        ValueDescriptor<T> newValueDescriptor = new ValueDescriptor<>(name, type, constraints, format, units, arrayDimensions);
        newValueDescriptor.metaUseOnly = true;
        return newValueDescriptor;
    }

    public void updateConstraints(ValueConstraint...valueConstraints) {
        constraints = valueConstraints;
    }

    public Integer getArrayDimensions() {
        return arrayDimensions;
    }

    public ValueConstraint[] getConstraints() {
        return constraints;
    }

    public ValueFormat getFormat() {
        return format;
    }

    public String[] getUnits() {
        return units;
    }

    public boolean isArray() {
        return arrayDimensions != null && arrayDimensions > 0;
    }

    public boolean isMetaUseOnly() {
        return metaUseOnly != null ? metaUseOnly : false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(ValueDescriptor.class.isAssignableFrom(obj.getClass()))) return false;
        ValueDescriptor<?> that = (ValueDescriptor<?>)obj;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    /**
     * Returns an instance of this {@link ValueDescriptor} where the {@link #arrayDimensions} are incremented by one
     * and the type becomes an array type of the original type (e.g. String -> String[] -> String[][])
     */
    @SuppressWarnings("unchecked")
    public ValueDescriptor<T[]> asArray() {
        try {
            Class<T[]> arrayClass = (Class<T[]>) ValueUtil.getArrayClass(type);
            return  new ValueDescriptor<>(name + "[]", arrayClass, constraints, format, units, arrayDimensions == null ? 1 : arrayDimensions+1);
        } catch (ClassNotFoundException ignored) {
            // Can't happen as we have the source class already
        }

        return null;
    }

    public ValueDescriptor<?> asNonArray() {
        if (arrayDimensions == null || arrayDimensions == 0) {
            return this;
        }
        return new ValueDescriptor<>(name.replaceAll("\\[\\]", ""), type, constraints, format, units, null);
    }

    @Override
    public String toString() {
        return ValueDescriptor.class.getSimpleName() + "{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", arrayDimensions=" + arrayDimensions +
            ", constraints=" + Arrays.toString(constraints) +
            ", format=" + format +
            ", metaUseOnly=" + isMetaUseOnly() +
            ", units=" + Arrays.toString(units) +
            '}';
    }
}
