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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.asset.Asset;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Date;
import java.util.Optional;

/**
 * Represents an event that occurs at a point in time with a {@link #start}, {#link #end} and optional
 * {@link #recurrence} and loosely follows the following standard for JSEvent. If {@link #end} is not specified then it
 * is assumed the event never ends and so only the {@link #start} is important.
 * <p>
 * CRON expressions were ruled out because intervals are not possible when using days
 * of the week (e.g. every other Thursday).
 * <p>
 * Note that unix timestamp milliseconds is used everywhere as time zone information can
 * be obtained from the associated {@link Asset}; {@link #start} and {@link #end} are required but {@link #recurrence}
 * is optional:
 * <ul>
 * <li><a href="https://tools.ietf.org/id/draft-jenkins-jscalendar-01.html">JSCalendar</a></li>
 * </ul>
 * <p>
 * Example 1 (One day, one time only):
 * <blockquote><pre>{@code
{
    "start": 1441148400000,
    "end": 1441234800000
}
 * }</pre></blockquote>
 * Example 2 (2 days starting 2nd September 2015; repeat every other week, 3 occurrences):
 * <blockquote><pre>{@code
{
    "start": 1441148400000,
    "end": 1441321200000,
    "recurrence": {
        "frequency": "WEEKLY",
        "interval": 2,
        "count": 3
    }
}
 * }</pre></blockquote>
 * Example 3 (8 hours starting 2nd September 2015 08:00am, repeat every day until 1st Feb 2018):
 * <blockquote><pre>{@code
{
    "start": 1441177200000,
    "ends": 1441206000000,
    "recurrence": {
        "frequency": "DAILY",
        "interval": 1,
        "until": 1517443200000
    }
}
 * }</pre></blockquote>
 */
public class CalendarEvent {
    protected Date start;
    protected Date end;
    protected RecurrenceRule recurrence;

    @JsonCreator
    public CalendarEvent(Date start, Date end, RecurrenceRule recurrence) {
        this.start = start;
        this.end = end;
        this.recurrence = recurrence;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public RecurrenceRule getRecurrence() {
        return recurrence;
    }

    public static Optional<CalendarEvent> fromValue(Value value) {
        if (value == null || value.getType() != ValueType.OBJECT) {
            return Optional.empty();
        }

        ObjectValue objectValue = (ObjectValue) value;
        Optional<Long> start = objectValue.get("start").flatMap(Values::getLongCoerced);
        Optional<Long> end = objectValue.get("end").flatMap(Values::getLongCoerced);
        Optional<RecurrenceRule> recurrence = RecurrenceRule.fromValue(objectValue.getObject("recurrence").orElse(null));

        return start.map(aLong -> new CalendarEvent(new Date(aLong),
            end.map(Date::new).orElse(null), recurrence.orElse(null)));
    }

    public Value toValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("start", start.getTime());
        if (end != null) {
            objectValue.put("end", end.getTime());
        }
        if (recurrence != null) {
            objectValue.put("recurrence", recurrence.toValue());
        }

        return objectValue;
    }
}
