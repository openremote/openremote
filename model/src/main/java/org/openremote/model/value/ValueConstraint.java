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
    public static ValueConstraint[] constraints(ValueConstraint...constraints) {
        return constraints;
    }

    @JsonTypeName("size")
    public static class Size extends ValueConstraint {

        protected Integer min;
        protected Integer max;

        @JsonCreator
        public Size(@JsonProperty("min") Integer min, @JsonProperty("max") Integer max) {
            super("{org.openremote.model.value.ValueConstraint.Size.message}");
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
            super("{org.openremote.model.value.ValueConstraint.Min.message}");
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
                    return minInt.compareTo(longValue.intValue()) <= 0;
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
            super("{org.openremote.model.value.ValueConstraint.Max.message}");
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
                    return maxInt.compareTo(longValue.intValue()) >= 0;
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
            super("{org.openremote.model.value.ValueConstraint.Pattern.message}");
            this.regexp = regexp;
            this.flags = flags;
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

        @JsonCreator
        public AllowedValues(@JsonProperty("allowedValues") Object...allowedValues) {
            super("{org.openremote.model.value.ValueConstraint.AllowedValues.message}");
            this.allowedValues = allowedValues;
        }

        public Object[] getAllowedValues() {
            return allowedValues;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of("values", Arrays.toString(allowedValues));
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
            String valueStr = null;

            if (Enum.class.isAssignableFrom(clazz)) {
                valueStr = ((Enum<?>) value).name();
            } else if (ValueUtil.isString(clazz)) {
                valueStr = ((CharSequence)value).toString();
            } else if (ValueUtil.isNumber(clazz)) {
                valueStr = value.toString();
            }

            return Arrays.asList(allowedValues).contains(valueStr);
        }

        public static AllowedValues fromEnum(Class<Enum<?>> enumClass) {
            Object[] allowedValues = Arrays.stream(enumClass.getEnumConstants()).map(enm -> ValueUtil.getStringCoerced(enm).orElse(null)).toArray(String[]::new);
            return new AllowedValues(allowedValues);
        }
    }

    @JsonTypeName("past")
    public static class Past extends ValueConstraint {

        public Past() {
            super("{org.openremote.model.value.ValueConstraint.Past.message}");
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
            super("{org.openremote.model.value.ValueConstraint.PastOrPresent.message}");
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
            super("{org.openremote.model.value.ValueConstraint.Future.message}");
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
            super("{org.openremote.model.value.ValueConstraint.FutureOrPresent.message}");
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
            super("{org.openremote.model.value.ValueConstraint.NotEmpty.message}");
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
            super("{org.openremote.model.value.ValueConstraint.NotBlank.message}");
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
            super("{org.openremote.model.value.ValueConstraint.NotNull.message}");
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
}
