/*
 * Copyright 2021, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.*;
import org.openremote.model.util.ValueUtil;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

// TODO: Switch to JSONSchema with a validator that supports POJOs (something like
//  https://github.com/java-json-tools/json-schema-validator which is no longer maintained) or find a JSR-380
//  implementation that supports dynamic validators
/**
 * Represents a constraint to apply to a value; these are based on JSR-380 validation.
 */
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(ValueConstraint.Size.class),
    @JsonSubTypes.Type(ValueConstraint.Pattern.class),
    @JsonSubTypes.Type(ValueConstraint.Min.class),
    @JsonSubTypes.Type(ValueConstraint.Max.class),
    @JsonSubTypes.Type(ValueConstraint.AllowedValues.class),
    @JsonSubTypes.Type(ValueConstraint.Past.class),
    @JsonSubTypes.Type(ValueConstraint.PastOrPresent.class),
    @JsonSubTypes.Type(ValueConstraint.Future.class),
    @JsonSubTypes.Type(ValueConstraint.FutureOrPresent.class),
    @JsonSubTypes.Type(ValueConstraint.NotEmpty.class),
    @JsonSubTypes.Type(ValueConstraint.NotBlank.class),
    @JsonSubTypes.Type(ValueConstraint.NotNull.class)
})
public abstract class ValueConstraint implements Serializable {

    public static ValueConstraint[] constraints(ValueConstraint...constraints) {
        return constraints;
    }

    @JsonTypeName("size")
    public static class Size extends ValueConstraint {

        protected Integer min;
        protected Integer max;

        @JsonCreator
        public Size(@JsonProperty("min") Integer min, @JsonProperty("max") Integer max) {
            this.min = min;
            this.max = max;
        }

        public Optional<Integer> getMin() {
            return Optional.ofNullable(min);
        }

        public Optional<Integer> getMax() {
            return Optional.ofNullable(max);
        }

        public Size setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("min")
    public static class Min extends ValueConstraint {
        protected Number min;

        @JsonCreator
        public Min(@JsonProperty("min") Number min) {
            this.min = min;
        }

        public Number getMin() {
            return min;
        }

        public Min setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("max")
    public static class Max extends ValueConstraint {
        protected Number max;

        @JsonCreator
        public Max(@JsonProperty("max") Number max) {
            this.max = max;
        }

        public Number getMax() {
            return max;
        }

        public Max setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("pattern")
    public static class Pattern extends ValueConstraint {
        protected String regexp;

        @JsonCreator
        public Pattern(@JsonProperty("regexp") String regexp) {
            this.regexp = regexp;
        }

        public String getRegexp() {
            return regexp;
        }

        public Pattern setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("allowedValues")
    public static class AllowedValues extends ValueConstraint {
        Object[] allowedValues;
        String[] allowedValueNames;

        @JsonCreator
        public AllowedValues(@JsonProperty("allowedValueNames") String[] allowedValueNames, @JsonProperty("allowedValues") Object...allowedValues) {
            this.allowedValueNames = allowedValueNames;
            this.allowedValues = allowedValues;
        }

        public AllowedValues(@JsonProperty("allowedValues") Object...allowedValues) {
            this.allowedValues = allowedValues;
        }

        public Object[] getAllowedValues() {
            return allowedValues;
        }

        public String[] getAllowedValueNames() {
            return allowedValueNames;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }

        public static AllowedValues fromEnum(Class<Enum<?>> enumClass) {
            Object[] allowedValues = Arrays.stream(enumClass.getEnumConstants()).map(enm -> ValueUtil.getStringCoerced(enm).orElse(null)).toArray(String[]::new);
            return new AllowedValues(allowedValues);
        }
    }

    @JsonTypeName("past")
    public static class Past extends ValueConstraint {

        public Past setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("pastOrPresent")
    public static class PastOrPresent extends ValueConstraint {

        public PastOrPresent setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("future")
    public static class Future extends ValueConstraint {

        public Future setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("futureOrPresent")
    public static class FutureOrPresent extends ValueConstraint {

        public FutureOrPresent setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();
            Instant valueInstant = ValueUtil.getValueCoerced(value, Instant.class).orElse(null);

            if (valueInstant != null) {
                int result = valueInstant.compareTo(now);
                return result >= 0;
            } else if (Map.class.isAssignableFrom(clazz)) {

            } else if (Collection.class.isAssignableFrom(clazz)) {

            } else if (ValueUtil.isArray(clazz)) {

            } else if (ValueUtil.isString(clazz)) {

            } else if (ValueUtil.isNumber(clazz)) {

            } else if (ValueUtil.isBoolean(clazz)) {

            }

            return false;
        }
    }

    @JsonTypeName("notEmpty")
    public static class NotEmpty extends ValueConstraint {

        public NotEmpty setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return false;
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {
                return !((Map<?, ?>)value).isEmpty();
            } else if (Collection.class.isAssignableFrom(clazz)) {
                return !((Collection<?>) value).isEmpty();
            } else if (ValueUtil.isArray(clazz)) {
                return Array.getLength(value) > 0;
            } else if (ValueUtil.isString(clazz)) {
                return !((CharSequence) value).isEmpty();
            }

            return false;
        }

        @Override
        public String toString() {
            return NotEmpty.class.getSimpleName() + "{}";
        }
    }

    @JsonTypeName("notBlank")
    public static class NotBlank extends ValueConstraint {

        public NotBlank setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            return value instanceof CharSequence && !value.toString().trim().isEmpty();
        }

        @Override
        public String toString() {
            return NotBlank.class.getSimpleName();
        }
    }

    @JsonTypeName("notNull")
    public static class NotNull extends ValueConstraint {

        public NotNull setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            return value != null;
        }

        @Override
        public String toString() {
            return NotNull.class.getSimpleName();
        }
    }

    protected String message;

    public abstract boolean evaluate(Object value, Instant now);

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}
