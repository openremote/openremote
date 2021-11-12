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
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.NumberComparator;
import org.openremote.model.util.ValueUtil;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Predicate for number values; will match based on configured options.
 */
@JsonSchemaDescription("Predicate for number values; will match based on configured options.")
public class NumberPredicate extends ValuePredicate {

    public static final String name = "number";
    public Number value;
    public Number rangeValue; // Used as upper bound when Operator.BETWEEN
    public AssetQuery.Operator operator = AssetQuery.Operator.EQUALS;
    public boolean negate;
    protected static NumberComparator comparator = new NumberComparator();

    protected NumberPredicate() {
    }

    public NumberPredicate(Number value) {
        this.value = value;
    }

    public NumberPredicate(Number value, AssetQuery.Operator operator) {
        this.value = value;
        this.operator = operator;
    }

    public NumberPredicate predicate(double predicate) {
        this.value = predicate;
        return this;
    }

    public NumberPredicate numberMatch(AssetQuery.Operator operator) {
        this.operator = operator;
        return this;
    }

    public NumberPredicate rangeValue(double rangeValue) {
        this.operator = AssetQuery.Operator.BETWEEN;
        this.rangeValue = rangeValue;
        return this;
    }

    public NumberPredicate negate(boolean negate) {
        this.negate = negate;
        return this;
    }

    @Override
    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {
        return obj ->
            ValueUtil.getValueCoerced(obj, Number.class).map(number -> {
                boolean result = operator.compare(comparator, number, value, rangeValue);
                return negate != result;
            }).orElse(false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "value=" + value +
            ", rangeValue=" + rangeValue +
            ", operator=" + operator +
            ", negate=" + negate +
            '}';
    }
}
