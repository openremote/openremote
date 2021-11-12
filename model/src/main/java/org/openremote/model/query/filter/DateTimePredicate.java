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
import org.openremote.model.util.Pair;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.util.ValueUtil;

import java.util.Date;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Predicate for date time values; provided values should be valid ISO 8601 datetime strings
 * (e.g. yyyy-MM-dd'T'HH:mm:ssZ or yyyy-MM-dd'T'HH:mm:ss\u00b1HH:mm), offset and time are optional, if no offset
 * information is supplied then UTC is assumed.
 */
@JsonSchemaDescription("Predicate for date time values; provided values should be valid ISO 8601 datetime strings (e.g. yyyy-MM-dd'T'HH:mm:ssZ or yyyy-MM-dd'T'HH:mm:ss±HH:mm), offset and time are optional, if no offset information is supplied then UTC is assumed.")
public class DateTimePredicate extends ValuePredicate {

    public static final String name = "datetime";
    public String value; // Sliding window value as ISO8601 duration e.g. PT1H or fixed date time in ISO 8601
    public String rangeValue; // Sliding window value as ISO8601 duration e.g. PT1H or fixed date time (used as upper bound when Operator.BETWEEN)
    public AssetQuery.Operator operator = AssetQuery.Operator.EQUALS;
    public boolean negate;

    public DateTimePredicate() {
    }

    public DateTimePredicate(AssetQuery.Operator operator, String value) {
        this.operator = operator;
        this.value = value;
    }

    public DateTimePredicate(String rangeStart, String rangeEnd) {
        this.operator = AssetQuery.Operator.BETWEEN;
        this.value = rangeStart;
        this.rangeValue = rangeEnd;
    }

    public DateTimePredicate operator(AssetQuery.Operator dateMatch) {
        this.operator = dateMatch;
        return this;
    }

    public DateTimePredicate value(String value) {
        this.value = value;
        return this;
    }

    public DateTimePredicate rangeValue(String rangeEnd) {
        this.operator = AssetQuery.Operator.BETWEEN;
        this.rangeValue = rangeEnd;
        return this;
    }

    public DateTimePredicate negate(boolean negate) {
        this.negate = negate;
        return this;
    }

    @Override
    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {
        return obj ->
            ValueUtil.getValueCoerced(obj, Date.class).map(date -> {
                Pair<Long, Long> fromAndTo = asFromAndTo(currentMillisSupplier.get());
                Long from = fromAndTo.key;
                Long to = fromAndTo.value;
                long timestamp = date.getTime();

                boolean result = operator.compare(Long::compare, timestamp, from, to);
                return negate != result;
            }).orElse(false);
    }

    public Pair<Long, Long> asFromAndTo(long currentMillis) {

        Long from;
        Long to = null;

        try {
            if (TimeUtil.isTimeDuration(value)) {
                from = currentMillis + TimeUtil.parseTimeDuration(value);
            } else {
                from = TimeUtil.parseTimeIso8601(value);
            }

            if (operator == AssetQuery.Operator.BETWEEN) {
                if (TimeUtil.isTimeDuration(rangeValue)) {
                    to = currentMillis + TimeUtil.parseTimeDuration(rangeValue);
                } else {
                    to = TimeUtil.parseTimeIso8601(rangeValue);
                }
            }
        } catch (IllegalArgumentException e) {
            from = null;
            to = null;
        }

        return new Pair<>(from, to);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "value='" + value + '\'' +
                ", rangeValue='" + rangeValue + '\'' +
                ", operator =" + operator +
                ", negate =" + negate +
                '}';
    }
}
