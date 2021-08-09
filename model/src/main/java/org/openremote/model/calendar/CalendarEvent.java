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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import org.openremote.model.asset.Asset;
import org.openremote.model.util.Pair;

import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
    "recurrence": "RRULE:FREQ=DAILY;INTERVAL=2;COUNT=4"
}
 * }</pre></blockquote>
 */
public class CalendarEvent implements Serializable {
    protected Date start;
    protected Date end;
    @JsonSerialize(converter = RecurStringConverter.class)
    protected Recur recurrence;

    public static class RecurStringConverter extends StdConverter<Recur, String> {

        @Override
        public String convert(Recur value) {
            return value.toString();
        }
    }

    @JsonCreator
    public CalendarEvent(@JsonProperty("start") Date start, @JsonProperty("end") Date end, @JsonProperty("recurrence") String recurrence) {
        Recur recur = null;

        try {
            recur = new Recur(recurrence);
        } catch (Exception e) {}

        this.start = start;
        this.end = end;
        this.recurrence = recur;
    }

    public CalendarEvent(Date start, Date end) {
        this(start, end, (Recur)null);
    }

    public CalendarEvent(Date start, Date end, Recur recurrence) {
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

    public Recur getRecurrence() {
        return recurrence;
    }

    public Pair<Long, Long> getNextOrActiveFromTo(Date when) {

        if (getEnd() == null) {
            return new Pair<>(getStart().getTime(), Long.MAX_VALUE);
        }

        if (getStart().before(when) && getEnd().after(when) && (getEnd().getTime()-when.getTime() > 1000)) {
            return new Pair<>(getStart().getTime(), getEnd().getTime());
        }

        Recur recurrence = getRecurrence();

        if (recurrence == null) {
            if (getEnd().before(when)) {
                return null;
            }
            return new Pair<>(getStart().getTime(), getEnd().getTime());
        }

        long whenMillis = when.toInstant().minus(getEnd().getTime() - getStart().getTime(), ChronoUnit.MILLIS).toEpochMilli();
        DateList matches = recurrence.getDates(new net.fortuna.ical4j.model.DateTime(getStart()), new net.fortuna.ical4j.model.DateTime(whenMillis), new net.fortuna.ical4j.model.DateTime(Long.MAX_VALUE), net.fortuna.ical4j.model.parameter.Value.DATE_TIME, 2);

        if (matches.isEmpty()) {
            return null;
        }

        long endTime = matches.get(0).getTime() + (getEnd().getTime()- getStart().getTime());

        if (endTime <= when.getTime()) {
            if (matches.size() == 2) {
                return new Pair<>(matches.get(1).getTime(), matches.get(1).getTime() + (getEnd().getTime()- getStart().getTime()));
            }
            return null;
        }

        return new Pair<>(matches.get(0).getTime(), endTime);
    }
}
