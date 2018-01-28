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
package org.openremote.model.calendar;

import org.openremote.model.util.EnumUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Date;
import java.util.Optional;

public class RecurrenceRule {

    public enum Frequency {
        YEARLY,
        MONTHLY,
        WEEKLY,
        DAILY
    }

    protected Frequency frequency;
    protected Integer interval;
    protected Integer count;
    protected Date until;

    protected RecurrenceRule() {
    }

    public RecurrenceRule(Frequency frequency) {
        this.frequency = frequency;
    }

    public RecurrenceRule(Frequency frequency, int interval, int count) {
        this.frequency = frequency;
        this.interval = interval;
        this.count = count;
    }

    public RecurrenceRule(Frequency frequency, int interval, Date until) {
        this.frequency = frequency;
        this.interval = interval;
        this.until = until;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Integer getInterval() {
        return interval;
    }

    public Integer getCount() {
        return count;
    }

    public Date getUntil() {
        return until;
    }

    public static Optional<RecurrenceRule> fromValue(Value value) {
        if (value == null || value.getType() != ValueType.OBJECT) {
            return Optional.empty();
        }

        ObjectValue objectValue = (ObjectValue) value;
        Optional<Frequency> frequency = objectValue.getString("frequency")
            .flatMap(s -> EnumUtil.enumFromString(Frequency.class, s));
        Integer interval = objectValue.get("interval").flatMap(Values::getIntegerCoerced).orElse(null);
        Integer count = objectValue.get("count").flatMap(Values::getIntegerCoerced).orElse(null);
        Optional<Long> until = objectValue.get("until").flatMap(Values::getLongCoerced);

        if (!frequency.isPresent()) {
            return Optional.empty();
        }

        RecurrenceRule rRule = new RecurrenceRule(frequency.get());
        rRule.interval = interval;
        rRule.count = count;
        until.ifPresent(l -> rRule.until = new Date(1000L * l));
        return Optional.of(rRule);
    }

    public Value toValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("frequency", Values.create(frequency.name()));
        if (interval != null) {
            objectValue.put("interval", Values.create(interval));
        }
        if (count != null) {
            objectValue.put("count", Values.create(count));
        }
        if (until != null) {
            objectValue.put("until", Values.create(until.getTime()/1000));
        }

        return objectValue;
    }
}
