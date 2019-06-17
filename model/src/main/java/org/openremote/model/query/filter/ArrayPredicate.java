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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

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
public class ArrayPredicate implements ValuePredicate {

    public static final String name = "array";
    public boolean negated;
    public Value value;
    public Integer index;
    public Integer lengthEquals;
    public Integer lengthGreaterThan;
    public Integer lengthLessThan;

    @JsonCreator
    public ArrayPredicate(@JsonProperty("value") Value value,
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

    public ArrayPredicate value(Value value) {
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
    public ObjectValue toModelValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("predicateType", name);
        objectValue.put("negated", Values.create(negated));
        objectValue.put("value", value);
        if (index != null) {
            objectValue.put("index", Values.create(index));
        }
        if (lengthEquals != null) {
            objectValue.put("lengthEquals", Values.create(lengthEquals));
        }
        if (lengthGreaterThan != null) {
            objectValue.put("lengthGreaterThan", Values.create(lengthGreaterThan));
        }
        if (lengthLessThan != null) {
            objectValue.put("lengthLessTHan", Values.create(lengthLessThan));
        }
        return objectValue;
    }
}
