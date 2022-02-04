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
import java.util.Arrays;
import java.util.Optional;

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
    }

    @JsonTypeName("pastOrPresent")
    public static class PastOrPresent extends ValueConstraint {

        public PastOrPresent setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    @JsonTypeName("future")
    public static class Future extends ValueConstraint {

        public Future setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    @JsonTypeName("futureOrPresent")
    public static class FutureOrPresent extends ValueConstraint {

        public FutureOrPresent setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    @JsonTypeName("notEmpty")
    public static class NotEmpty extends ValueConstraint {

        public NotEmpty setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    @JsonTypeName("notBlank")
    public static class NotBlank extends ValueConstraint {

        public NotBlank setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    @JsonTypeName("notNull")
    public static class NotNull extends ValueConstraint {

        public NotNull setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    protected String message;

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}
