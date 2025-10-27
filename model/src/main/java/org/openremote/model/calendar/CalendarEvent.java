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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import net.fortuna.ical4j.model.Recur;
import org.openremote.model.asset.Asset;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.util.JSONSchemaUtil.*;
import org.openremote.model.util.Pair;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
 * <li><a href="https://datatracker.ietf.org/doc/draft-ietf-calext-jscalendarbis/">JSCalendar</a></li>
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
 * Example 3 (8 hours starting 2nd September 2015 08:00am; repeat every other day, 4 occurrences):
 * <blockquote><pre>{@code
{
    "start": 1441177200000,
    "end": 1441206000000,
    "recurrence": "RRULE:FREQ=DAILY;INTERVAL=2;COUNT=4"
}
 * }</pre></blockquote>
 */
public class CalendarEvent implements Serializable {

    //        @JsonPropertyDescription("Set a start date, if not provided, considers 00:00 of the current date." +
//                " When the replay datapoint timestamp is 0 it will insert it at 00:00, unless the recurrence rule" +
//                " specifies any of the following rule parts: FREQ=(HOURLY/MINUTELY/SECONDLY);" +
//                " BYSECOND=...;BYMINUTE=...;BYHOUR=...;BYSETPOS=... This will cause the datapoint timestamp to be" +
//                " relative to when the first occurrence is scheduled.")
//        @JsonDeserialize(using = LocalDateDeserializer.class)
//        @JsonSerialize(using = LocalDateSerializer.class)
//        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
//        @JsonSchemaFormat("date")
//        protected LocalDate start;
//
//        @JsonPropertyDescription("The recurrence schedule follows the RFC 5545 RRULE format.")
//        @JsonSchemaTypeRemap(type = String.class)
//        @JsonSerialize(converter = RecurStringConverter.class)
//        protected Recur<LocalDateTime> recurrence;
//
//            this.start = Optional.ofNullable(start).orElse(LocalDate.now());

    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)
    @JsonSerialize(using = DateSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSchemaFormat("date")
    protected Date start;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSchemaFormat("date")
    protected Date end;

    @JsonSchemaDescription("The recurrence schedule follows the RFC 5545 RRULE format.")
    @JsonSchemaTypeRemap(type = String.class)
    @JsonSerialize(converter = RecurStringConverter.class)
    protected Recur<LocalDateTime> recurrence;

    @JsonIgnore
    private LocalDateTime current;
    @JsonIgnore
    private LocalDateTime upcoming;

    public static class RecurStringConverter extends StdConverter<Recur<?>, String> {

        @Override
        public String convert(Recur<?> value) {
            return value.toString();
        }
    }

    @JsonCreator
    public CalendarEvent(@JsonProperty("start") Date start, @JsonProperty("end") Date end, @JsonProperty("recurrence") String recurrence) {
        Recur<LocalDateTime> recur = null;

        try {
            recur = new Recur<>(recurrence);
        } catch (Exception ignored) {}

        this.start = Optional.ofNullable(start).orElse(Date.from(Instant.now()));
        this.end = end;
        this.recurrence = recur;
        this.upcoming = LocalDateTime.ofInstant(this.start.toInstant(), ZoneOffset.UTC);
    }

    public CalendarEvent(Date start, Date end) {
        this(start, end, (Recur<LocalDateTime>)null);
    }

    public CalendarEvent(Date start, Date end, Recur<LocalDateTime> recurrence) {
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

    public Recur<LocalDateTime> getRecurrence() {
        return recurrence;
    }

    public LocalDateTime getCurrent() {
        return current;
    }

    public LocalDateTime getUpcoming() {
        return upcoming;
    }

    public Pair<Long, Long> getNextOrActiveFromTo(Date when) {

        if (getEnd() == null) {
            return new Pair<>(getStart().getTime(), Long.MAX_VALUE);
        }

        if (getStart().before(when) && getEnd().after(when) && (getEnd().getTime()-when.getTime() > 1000)) {
            return new Pair<>(getStart().getTime(), getEnd().getTime());
        }

        Recur<LocalDateTime> recurrence = getRecurrence();

        if (recurrence == null) {
            if (getEnd().before(when)) {
                return null;
            }
            return new Pair<>(getStart().getTime(), getEnd().getTime());
        }

        Instant whenMillis = when.toInstant().minus(getEnd().getTime() - getStart().getTime(), ChronoUnit.MILLIS);
        List<LocalDateTime> matches = recurrence.getDates(
				LocalDateTime.ofInstant(getStart().toInstant(), ZoneOffset.UTC),
		        LocalDateTime.ofInstant(whenMillis, ZoneOffset.UTC),
		        LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneOffset.UTC),
		        2);

		List<Instant> instants = matches.stream().map(d -> d.toInstant(ZoneOffset.UTC)).toList();

        if (matches.isEmpty()) {
            return null;
        }

        long endTime = instants.get(0).toEpochMilli() + (getEnd().getTime()- getStart().getTime());

        if (endTime <= when.getTime()) {
            if (instants.size() == 2) {
                return new Pair<>(instants.get(1).toEpochMilli(), instants.get(1).toEpochMilli() + (getEnd().getTime()- getStart().getTime()));
            }
            return null;
        }

        return new Pair<>(instants.get(0).toEpochMilli(), endTime);
    }

    /**
     * Try to advance to the next occurrence once {@code secondsSinceEpoch} surpasses previously active.
     * <p>
     * An occurrence can be a single event or part of a recurring event.
     * <p>
     * If a recurrence rule has been configured, the start of the current occurrence is used.
     *
     * @param secondsSinceEpoch Seconds since the epoch (1970-01-01T00:00:00Z)
     * @return The start time of the active occurrence. If not started, the start of the schedule is returned.
     */
    public OptionalLong tryAdvanceActive(long secondsSinceEpoch) {
        long startInSeconds = start.getTime() / 1000;

        if (recurrence == null) {
            current = LocalDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC);
            if (getEnd() == null) {
                return OptionalLong.of(startInSeconds);
            } else if (secondsSinceEpoch > getEnd().getTime() / 1000) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(startInSeconds);
        }

        // Preemptively get next to determine whether to advance the occurrence or to end the recurrence
        LocalDateTime start = LocalDateTime.ofEpochSecond(startInSeconds, 0, ZoneOffset.UTC);
        LocalDateTime now = LocalDateTime.ofEpochSecond(secondsSinceEpoch, 0, ZoneOffset.UTC);
        LocalDateTime next = recurrence.getNextDate(start, now);

        // Recurrence has ended
        if (next == null) {
            return OptionalLong.empty();
        }

        // Track active and upcoming occurrence
        if (upcoming.isBefore(next) && current != null) {
            current = upcoming;
        }
        upcoming = next;

        // Check if this is the first time
        if (current == null) {
            LocalDateTime epoch = LocalDateTime.ofEpochSecond(0,0, ZoneOffset.UTC);
            LocalDateTime firstOccurrence = recurrence.getNextDate(start, epoch);
            // If the first occurrence does not equal 'start' when using a BYxxx rule part
            if (startInSeconds != firstOccurrence.toEpochSecond(ZoneOffset.UTC)) {
                current = next;
                return OptionalLong.of(current.toEpochSecond(ZoneOffset.UTC));
            }
            // Get the previous (active) occ to catch up with the current occurrence for the first time this method is called
            List<LocalDateTime> dates = recurrence.getDates(start, start, now); // TODO: consider limiting number of occurrences
            if (!dates.isEmpty()) {
                current = dates.getLast();
                return OptionalLong.of(current.toEpochSecond(ZoneOffset.UTC));
            }
            current = start;
            return OptionalLong.of(startInSeconds);
        }

        return OptionalLong.of(current.toEpochSecond(ZoneOffset.UTC));
    }

    /**
     * Gets the time until the next occurrence starts.
     *
     * @param timeSinceOccurrenceStarted Seconds since the current occurrence started
     * @return Seconds until the next occurrence starts.
     * <p>
     * If this is a one-time event, or if the recurrence rule has ended returns {@code null} instead.
     */
    public OptionalLong getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted) {
        Recur<LocalDateTime> recurrence = getRecurrence();

        // Single event schedule has ended.
        if (recurrence == null || current == null || upcoming == null) {
            return OptionalLong.empty();
        }

        long duration = upcoming.toEpochSecond(ZoneOffset.UTC) - current.toEpochSecond(ZoneOffset.UTC);
        return OptionalLong.of(duration - timeSinceOccurrenceStarted);
    }

    public boolean getIsSingleOccurrence() {
        return Optional.ofNullable(start).map(s -> recurrence == null).orElse(false);
    }

    public boolean getHasRecurrenceEnded(long timestamp) {
        return recurrence.getNextDate(current, LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)) == null;
    }

    /**
     * Calculates the remaining occurrence delay in seconds relative to the current time.
     *
     * @param offset The offset from the current occurrence.
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The remaining occurrence delay in seconds relative to the current time.
     * <p>
     * If this is a one-time event, or if the recurrence rule has ended returns {@code null} instead.
     */
    public static OptionalLong getDelay(long offset, long timeSinceOccurrenceStarted, CalendarEvent calendarEvent) {
        if (offset <= timeSinceOccurrenceStarted) {
            return getTimeUntilNextOccurrence(timeSinceOccurrenceStarted, calendarEvent)
                    .stream()
                    .map(n -> offset + n)
                    .findFirst();
        }
        return OptionalLong.of(offset - timeSinceOccurrenceStarted);
    }

    /**
     * Calculates the time in seconds until the next occurrence starts.
     * <p>
     * If no schedule has been defined uses the default 1-day schedule.
     *
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The delay in seconds until the next occurrence.
     * <p>
     * If this is a one-time event, or if the recurrence rule has ended returns {@code null} instead.
     */
    public static OptionalLong getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted, CalendarEvent calendarEvent) {
        if (calendarEvent != null) {
            return calendarEvent.getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
        }
        return OptionalLong.of(86400L - timeSinceOccurrenceStarted);
    }
}
