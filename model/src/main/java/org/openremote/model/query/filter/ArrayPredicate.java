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
package org.openremote.model.query.filter;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.util.ValueUtil;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Determines if the value is an array and meets the following:
 * <ul>
 * <li>{@link #value} - Does array contain this value</li>
 * <li>{@link #index} - If specified will check that the value at this index equals the specified {@link #value}</li>
 * <li>{@link #lengthEquals} - Is length of array equal to the specified value</li>
 * <li>{@link #lengthGreaterThan} - Is length of array greater than the specified value</li>
 * <li>{@link #lengthLessThan} - Is length of array less than the specified value</li>
 * </ul>
 * There is an implicit 'and' between each specified condition; the negation is applied once all the criteria are
 * evaluated.
 */
@JsonSchemaDescription("Predicate for array values; will match based on configured options.")
public class ArrayPredicate extends ValuePredicate {

    public static final String name = "array";
    public boolean negated;
    public Object value;
    public Integer index;
    public Integer lengthEquals;
    public Integer lengthGreaterThan;
    public Integer lengthLessThan;

    @JsonCreator
    public ArrayPredicate(@JsonProperty("value") Object value,
                          @JsonProperty("index") Integer index,
                          @JsonProperty("lengthEquals") Integer lengthEquals,
                          @JsonProperty("lengthGreaterThan") Integer lengthGreaterThan,
                          @JsonProperty("lengthLessThan") Integer lengthLessThan,
                          @JsonProperty("negated") boolean negate) {
        this.value = value;
        this.index = index;
        this.lengthEquals = lengthEquals;
        this.lengthGreaterThan = lengthGreaterThan;
        this.lengthLessThan = lengthLessThan;
        negated = negate;
    }

    public ArrayPredicate negate() {
        negated = !negated;
        return this;
    }

    public ArrayPredicate value(Object value) {
        this.value = value;
        return this;
    }

    public ArrayPredicate index(int index) {
        this.index = index;
        return this;
    }

    public ArrayPredicate lengthEquals(int lengthEquals) {
        this.lengthEquals = lengthEquals;
        return this;
    }

    public ArrayPredicate lengthGreaterThan(int lengthGreaterThan) {
        this.lengthGreaterThan = lengthGreaterThan;
        return this;
    }

    public ArrayPredicate lengthLessThan(int lengthLessThan) {
        this.lengthLessThan = lengthLessThan;
        return this;
    }

    @Override
    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {
        return obj -> {
            if (obj == null || !ValueUtil.isArray(obj.getClass())) {
                return false;
            }

            @SuppressWarnings("OptionalGetWithoutIsPresent")
            Object[] arrayValue = ValueUtil.getValueCoerced(obj, Object[].class).get();
            boolean result = true;

            if (value != null) {
                if (index != null) {
                    result = arrayValue.length >= index && Objects.equals(arrayValue[index], value);
                } else {
                    result = Arrays.stream(arrayValue).anyMatch(av -> Objects.equals(av, value));
                }
            }

            if (result && lengthEquals != null) {
                result = arrayValue.length == lengthEquals;
            }
            if (result && lengthGreaterThan != null) {
                result = arrayValue.length > lengthGreaterThan;
            }
            if (result && lengthLessThan != null) {
                result = arrayValue.length < lengthLessThan;
            }
            if (negated) {
                return !result;
            }
            return result;
        };
    }
}
