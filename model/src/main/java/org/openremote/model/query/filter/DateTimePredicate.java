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

import org.openremote.model.query.AssetQuery;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

/**
 * Predicate for date time values; provided values should be valid ISO 8601 datetime strings (e.g. yyyy-MM-dd'T'HH:mm:ssZ
 * or yyyy-MM-dd'T'HH:mm:ss\u00b1HH:mm), it is recommended to include time zone information to avoid any issues working
 * across timezones.
 */
public class DateTimePredicate implements ValuePredicate {

    public static final String name = "datetime";
    public String value; // Sliding window value e.g. 1h or fixed date time
    public String rangeValue; // Sliding window value e.g. 1h or fixed date time (used as upper bound when Operator.BETWEEN)
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

    public static DateTimePredicate fromObjectValue(ObjectValue objectValue) {
        DateTimePredicate dateTimePredicate = new DateTimePredicate();

        objectValue.getString("value").ifPresent(value -> {
            dateTimePredicate.value = value;
        });
        objectValue.getString("rangeValue").ifPresent(rangeValue -> {
            dateTimePredicate.rangeValue = rangeValue;
        });
        objectValue.getString("operator").ifPresent(operator -> {
            dateTimePredicate.operator = AssetQuery.Operator.valueOf(operator);
        });
        objectValue.getBoolean("negate").ifPresent(negate -> {
            dateTimePredicate.negate = negate;
        });
        return dateTimePredicate;
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

    public ObjectValue toModelValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("predicateType", name);
        objectValue.put("value", Values.create(value));
        objectValue.put("rangeValue", Values.create(rangeValue));
        objectValue.put("negate", Values.create(negate));
        objectValue.put("operator", Values.create(operator.toString()));
        return objectValue;
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
