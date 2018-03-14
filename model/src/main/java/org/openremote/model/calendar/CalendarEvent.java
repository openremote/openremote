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

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Date;
import java.util.Optional;

/**
 * Represents an event that occurs at a point in time with a duration and optional recurrence. This is inspired by the
 * following standards:
 * <ul>
 * <li><a href="https://icalendar.org/RFC-Specifications/iCalendar-RFC-5545/">iCalendar RFC-5545</a></li>
 * <li><a href="https://tools.ietf.org/id/draft-jenkins-jscalendar-01.html">JSCalendar</a></li>
 * </ul>
 * <p>
 * Example 1:
 * <blockquote><pre>{@code
{
    "start": 1517011200,
    "end": 1517014800
}
 * }</pre></blockquote>
 * Example 2 (Repeat every other week, 3 occurrences):
 * <blockquote><pre>{@code
{
    "start": 1517011200,
    "end": 1517014800,
    "recurrence": {
        "frequency": "WEEKLY",
        "interval": 2,
        "count": 3
    }
}
 * }</pre></blockquote>
 * Example 3 (Repeat every day until 1st Feb 2018):
 * <blockquote><pre>{@code
{
    "start": 1517011200,
    "end": 1517014800,
    "recurrence": {
        "frequency": "WEEKLY",
        "interval": 2,
        "until": 1517443200
    }
}
 * }</pre></blockquote>
 * <b>
 * <h1>NOTES</h1>
 * <ul>
 * <li>date and time values must be in Unix timestamp seconds</li>
 * </ul>
 * </b>
 */
public class CalendarEvent {
    protected Date start;
    protected Date end;
    protected RecurrenceRule recurrence;

    protected CalendarEvent() {
    }

    public CalendarEvent(Date start, Date end, RecurrenceRule recurrence) {
        this.start = start;
        this.end = end;
        this.recurrence = recurrence;
    }

    public CalendarEvent(Date start, Date end) {
        this.start = start;
        this.end = end;
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

        if (!start.isPresent() || !end.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(
            new CalendarEvent(new Date(1000L*start.get()),
                              new Date(1000L*end.get()), recurrence.orElse(null))
        );
    }

    public Value toValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("start", start.getTime() / 1000);
        objectValue.put("end", end.getTime() / 1000);
        if (recurrence != null) {
            objectValue.put("recurrence", recurrence.toValue());
        }

        return objectValue;
    }
}
