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
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

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

    public static final String VALUE_CONSTRAINT_INVALID = "{ValueConstraint.Invalid}";
    public static final String SIZE_MESSAGE_TEMPLATE = "{ValueConstraint.Size.message}";
    public static final String MIN_MESSAGE_TEMPLATE = "{ValueConstraint.Min.message}";
    public static final String MAX_MESSAGE_TEMPLATE = "{ValueConstraint.Max.message}";
    public static final String PATTERN_MESSAGE_TEMPLATE = "{ValueConstraint.Pattern.message}";
    public static final String ALLOWED_VALUES_MESSAGE_TEMPLATE = "{ValueConstraint.AllowedValues.message}";
    public static final String PAST_MESSAGE_TEMPLATE = "{ValueConstraint.Past.message}";
    public static final String PAST_OR_PRESENT_MESSAGE_TEMPLATE = "{ValueConstraint.PastOrPresent.message}";
    public static final String FUTURE_MESSAGE_TEMPLATE = "{ValueConstraint.Future.message}";
    public static final String FUTURE_OR_PRESENT_MESSAGE_TEMPLATE = "{ValueConstraint.FutureOrPresent.message}";
    public static final String NOT_EMPTY_MESSAGE_TEMPLATE = "{ValueConstraint.NotEmpty.message}";
    public static final String NOT_BLANK_MESSAGE_TEMPLATE = "{ValueConstraint.NotBlank.message}";
    public static final String NOT_NULL_MESSAGE_TEMPLATE = "{ValueConstraint.NotNull.message}";

    @JsonTypeName("size")
    public static class Size extends ValueConstraint {

        protected Integer min;
        protected Integer max;

        @JsonCreator
        public Size(@JsonProperty("min") Integer min, @JsonProperty("max") Integer max) {
            super(SIZE_MESSAGE_TEMPLATE);
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
        public Map<String, Object> getParameters() {
            return Map.of(
                "min", min,
                "max", max);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true; // Same behaviour as JSR-380
            }

            Class<?> clazz = value.getClass();

            if (Map.class.isAssignableFrom(clazz)) {
                int size = ((Map)value).size();
                return size >= min && size <= max;
            } else if (Collection.class.isAssignableFrom(clazz)) {
                int size = ((Collection)value).size();
                return size >= min && size <= max;
            } else if (ValueUtil.isArray(clazz)) {
                int size = Array.getLength(value);
                return size >= min && size <= max;
            } else if (ValueUtil.isString(clazz)) {
                int size = ((CharSequence)value).length();
                return size >= min && size <= max;
            }

            return false;
        }
    }

    @JsonTypeName("min")
    public static class Min extends ValueConstraint {
        protected Number min;

        @JsonCreator
        public Min(@JsonProperty("min") Number min) {
            super(MIN_MESSAGE_TEMPLATE);
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
        public Map<String, Object> getParameters() {
            return Map.of("value", min);
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Class<?> clazz = value.getClass();

            if (!ValueUtil.isNumber(clazz)) {
                return false;
            }

            if (min instanceof Integer minInt) {
                if (value instanceof Long longValue) {
                    return Long.valueOf(minInt.longValue()).compareTo(longValue) <= 0;
                }
                if (value instanceof Double doubleValue) {
                    return minInt.compareTo(doubleValue.intValue()) <= 0;
                }
                if (value instanceof Integer intValue) {
                    return minInt.compareTo(intValue) <= 0;
                }
                return false;
            }
            if (min instanceof Double minDouble) {
                if (value instanceof Long longValue) {
                    return minDouble.compareTo(longValue.doubleValue()) <= 0;
                }
                if (value instanceof Double doubleValue) {
                    return minDouble.compareTo(doubleValue) <= 0;
                }
                if (value instanceof Integer intValue) {
                    return minDouble.compareTo(intValue.doubleValue()) <= 0;
                }
                return false;
            }
            if (min instanceof Long minLong) {
                if (value instanceof Long longValue) {
                    return minLong.compareTo(longValue) <= 0;
                }
                if (value instanceof Double doubleValue) {
                    return minLong.compareTo(doubleValue.longValue()) <= 0;
                }
                if (value instanceof Integer intValue) {
                    return minLong.compareTo(intValue.longValue()) <= 0;
                }
            }

            return false;
        }
    }

    @JsonTypeName("max")
    public static class Max extends ValueConstraint {
        protected Number max;

        @JsonCreator
        public Max(@JsonProperty("max") Number max) {
            super(MAX_MESSAGE_TEMPLATE);
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
        public Map<String, Object> getParameters() {
            return Map.of("value", max);
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Class<?> clazz = value.getClass();

            if (!ValueUtil.isNumber(clazz)) {
                return false;
            }

            if (max instanceof Integer maxInt) {
                if (value instanceof Long longValue) {
                    return Long.valueOf(maxInt.longValue()).compareTo(longValue) >= 0;
                }
                if (value instanceof Double doubleValue) {
                    return maxInt.compareTo(doubleValue.intValue()) >= 0;
                }
                if (value instanceof Integer intValue) {
                    return maxInt.compareTo(intValue) >= 0;
                }
                return false;
            }
            if (max instanceof Double maxDouble) {
                if (value instanceof Long longValue) {
                    return maxDouble.compareTo(longValue.doubleValue()) >= 0;
                }
                if (value instanceof Double doubleValue) {
                    return maxDouble.compareTo(doubleValue) >= 0;
                }
                if (value instanceof Integer intValue) {
                    return maxDouble.compareTo(intValue.doubleValue()) >= 0;
                }
                return false;
            }
            if (max instanceof Long maxLong) {
                if (value instanceof Long longValue) {
                    return maxLong.compareTo(longValue) >= 0;
                }
                if (value instanceof Double doubleValue) {
                    return maxLong.compareTo(doubleValue.longValue()) >= 0;
                }
                if (value instanceof Integer intValue) {
                    return maxLong.compareTo(intValue.longValue()) >= 0;
                }
            }

            return false;
        }
    }

    @JsonTypeName("pattern")
    public static class Pattern extends ValueConstraint {
        private static final java.util.regex.Pattern ESCAPE_MESSAGE_PARAMETER_PATTERN = java.util.regex.Pattern.compile("([\\\\{}$])");
        protected String regexp;
        protected jakarta.validation.constraints.Pattern.Flag[] flags;

        @JsonCreator
        public Pattern(String regexp, jakarta.validation.constraints.Pattern.Flag[] flags) {
            super(PATTERN_MESSAGE_TEMPLATE);
            this.regexp = regexp;
            this.flags = flags;
        }

        public Pattern(String regexp) {
            this(regexp, null);
        }

        public String getRegexp() {
            return regexp;
        }

        public jakarta.validation.constraints.Pattern.Flag[] getFlags() {
            return flags;
        }

        public Pattern setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, Object> getParameters() {
            String escapedRegexp = regexp != null ? ESCAPE_MESSAGE_PARAMETER_PATTERN.matcher(regexp).replaceAll( Matcher.quoteReplacement("\\") + "$1" ) : "null";
            return Map.of("value", escapedRegexp);
        }

        @SuppressWarnings("MagicConstant")
        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Class<?> clazz = value.getClass();

            if (CharSequence.class.isAssignableFrom(clazz)) {
                int intFlag = 0;
                if (flags != null) {
                    for (jakarta.validation.constraints.Pattern.Flag flag : flags) {
                        intFlag = intFlag | flag.getValue();
                    }
                }

                try {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexp, intFlag);
                    Matcher m = pattern.matcher((CharSequence)value);
                    return m.matches();
                }
                catch (PatternSyntaxException ignored) {}
            }

            return false;
        }
    }

    @JsonTypeName("allowedValues")
    public static class AllowedValues extends ValueConstraint {
        Object[] allowedValues;
        String[] allowedValueNames;

        @JsonCreator
        public AllowedValues(@JsonProperty("allowedValueNames") String[] allowedValueNames, @JsonProperty("allowedValues") Object[] allowedValues) {
            super(ALLOWED_VALUES_MESSAGE_TEMPLATE);
            this.allowedValueNames = allowedValueNames;
            this.allowedValues = allowedValues;
        }

        public AllowedValues(Object...allowedValues) {
            super(ALLOWED_VALUES_MESSAGE_TEMPLATE);
            this.allowedValues = allowedValues;
        }

        public Object[] getAllowedValues() {
            return allowedValues;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of("values", Arrays.toString(allowedValues));
        }

        public String[] getAllowedValueNames() {
            return allowedValueNames;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }
            if (allowedValues == null || allowedValues.length == 0) {
                return false;
            }

            Class<?> clazz = value.getClass();
            Object compareValue = null;

            if (Enum.class.isAssignableFrom(clazz)) {
                // We can skip this check as the value is already a concrete type so must be valid - allowed values is for informational purposes
                return true;
            } else if (ValueUtil.isString(clazz)) {
                compareValue = ((CharSequence)value).toString();
            } else if (ValueUtil.isNumber(clazz)) {
                compareValue = value;
            }

            return Arrays.asList(allowedValues).contains(compareValue);
        }

        public static <T extends Enum<T>> AllowedValues fromEnumValues(T[] enumValues) {
            Object[] allowedValues = Arrays.stream(enumValues).map(enm -> ValueUtil.getStringCoerced(enm).orElse(null)).toArray(String[]::new);
            return new AllowedValues(allowedValues);
        }
    }

    @JsonTypeName("past")
    public static class Past extends ValueConstraint {

        public Past() {
            super(PAST_MESSAGE_TEMPLATE);
        }

        public Past setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Instant valueInstant = ValueUtil.getValueCoerced(value, Instant.class).orElse(null);

            if (valueInstant != null) {
                int result = valueInstant.compareTo(now);
                return result < 0;
            }

            return false;
        }
    }

    @JsonTypeName("pastOrPresent")
    public static class PastOrPresent extends ValueConstraint {

        public PastOrPresent() {
            super(PAST_OR_PRESENT_MESSAGE_TEMPLATE);
        }

        public PastOrPresent setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Instant valueInstant = ValueUtil.getValueCoerced(value, Instant.class).orElse(null);

            if (valueInstant != null) {
                int result = valueInstant.compareTo(now);
                return result <= 0;
            }

            return false;
        }
    }

    @JsonTypeName("future")
    public static class Future extends ValueConstraint {

        public Future() {
            super(FUTURE_MESSAGE_TEMPLATE);
        }

        public Future setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Instant valueInstant = ValueUtil.getValueCoerced(value, Instant.class).orElse(null);

            if (valueInstant != null) {
                int result = valueInstant.compareTo(now);
                return result > 0;
            }

            return false;
        }
    }

    @JsonTypeName("futureOrPresent")
    public static class FutureOrPresent extends ValueConstraint {

        public FutureOrPresent() {
            super(FUTURE_OR_PRESENT_MESSAGE_TEMPLATE);
        }

        public FutureOrPresent setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            if (value == null) {
                return true;
            }

            Instant valueInstant = ValueUtil.getValueCoerced(value, Instant.class).orElse(null);

            if (valueInstant != null) {
                int result = valueInstant.compareTo(now);
                return result >= 0;
            }

            return false;
        }
    }

    @JsonTypeName("notEmpty")
    public static class NotEmpty extends ValueConstraint {

        public NotEmpty() {
            super(NOT_EMPTY_MESSAGE_TEMPLATE);
        }

        public NotEmpty setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
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
        public NotBlank() {
            super(NOT_BLANK_MESSAGE_TEMPLATE);
        }

        public NotBlank setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            return value instanceof CharSequence && !value.toString().trim().isEmpty();
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        @Override
        public String toString() {
            return NotBlank.class.getSimpleName();
        }
    }

    @JsonTypeName("notNull")
    public static class NotNull extends ValueConstraint {

        public NotNull() {
            super(NOT_NULL_MESSAGE_TEMPLATE);
        }

        public NotNull setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean evaluate(Object value, Instant now) {
            return value != null;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        @Override
        public String toString() {
            return NotNull.class.getSimpleName();
        }
    }

    protected String message;

    protected ValueConstraint(String message) {
        this.message = message;
    }

    public abstract boolean evaluate(Object value, Instant now);

    public abstract Map<String, Object> getParameters();

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public static ValueConstraint[] constraints(ValueConstraint...constraints) {
        return constraints;
    }
}
