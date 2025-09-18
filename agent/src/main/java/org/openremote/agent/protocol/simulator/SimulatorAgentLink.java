/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.simulator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import net.fortuna.ical4j.model.Recur;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.simulator.SimulatorReplayDatapoint;

import java.time.*;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class SimulatorAgentLink extends AgentLink<SimulatorAgentLink> {

    @JsonPropertyDescription("Used to store a dataset of values that should be replayed (i.e. written to the" +
        " linked attribute) in a continuous loop based on a schedule (by default replays every 24h)." +
        " Predicted datapoints can be added by configuring 'Store predicted datapoints' which will insert the datapoints" +
        " immediately as determined by the schedule.")
    protected SimulatorReplayDatapoint[] replayData;

    // TODO: consider implementing @JsonSchemaFormat("calendar-event") to reuse the `or-rule-validity` component.
    // Current generator cannot handle injecting custom type and description at the same time
    @JsonPropertyDescription("When defined overwrites the possible dataset length and when it is replayed." +
        " This could be once when only a start- (and end) date are defined," +
        " or a recurring event following the RFC 5545 RRULE format." +
        " If not provided defaults to 24 hours. If the replay data contains datapoints scheduled after the" +
        " default 24 hours or the recurrence rule the datapoints will be ignored.")
    protected Schedule schedule;

    // For Hydrators
    protected SimulatorAgentLink() {
    }

    public SimulatorAgentLink(String id) {
        super(id);
    }

    public Optional<SimulatorReplayDatapoint[]> getReplayData() {
        return Optional.ofNullable(replayData);
    }

    public SimulatorAgentLink setReplayData(SimulatorReplayDatapoint[] replayData) {
        this.replayData = replayData;
        return this;
    }

    public Optional<Schedule> getSchedule() {
        return Optional.ofNullable(schedule);
    }

    public SimulatorAgentLink setSchedule(Schedule schedule) {
        this.schedule = schedule;
        return this;
    }

    /**
     * Calculates the delay in seconds until the {@link SimulatorReplayDatapoint} should be played.
     *
     * @param point The {@link SimulatorReplayDatapoint#timestamp} to calculate the delay for.
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The delay in seconds until the {@link SimulatorReplayDatapoint} should be replayed.
     * @throws Exception If the one-time occurrence or recurring schedule has ended.
     */
    public long getDelay(long point, long timeSinceOccurrenceStarted) throws Exception {
        if (point <= timeSinceOccurrenceStarted) {
            return point + getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
        }
        return point - timeSinceOccurrenceStarted;
    }

    /**
     * Calculates the time in seconds until the next occurrence starts.
     * <p>
     * If no schedule has been defined uses the default 1-day schedule.
     *
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The delay in seconds until the {@link SimulatorReplayDatapoint} should be replayed.
     * @throws Exception If the one-time occurrence or recurring schedule has ended.
     */
    public long getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted) throws Exception {
        if (getSchedule().isPresent()) {
            return getSchedule().get().getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
        }
        return 86400L - timeSinceOccurrenceStarted;
    }

    public static class Schedule extends CalendarEvent {

        @JsonIgnore
        private final long startTime = super.start.getTime() / 1000;
        @JsonIgnore
        private long count;
        @JsonIgnore
        private long currentOccurrence;

        public Schedule(Date start, Date end, String recurrence) {
            super(start, end, recurrence);
        }

        /**
         * Gets the time in seconds from when the occurrence starts relative to the current epoch time.
         * <p>
         * An occurrence can be a single event or part of a recurring event. If no recurrence rule has been configured,
         * the start time is used, or if the occurrence hasn't started yet.
         * <p>
         * If a recurrence rule has been configured, the start of the current occurrence is used. If the recurrence rule
         * specifies {@code UNTIL} or {@code COUNT} the previous occurrence start time is used.
         *
         * @param epoch Seconds since the epoch
         * @return Seconds since the occurrence started
         */
        public long getTimeSinceOccurrenceStarted(long epoch) {
            Recur<LocalDateTime> recurrence = super.getRecurrence();

            if (recurrence == null || epoch < startTime) {
                return epoch - startTime;
            }

            LocalDateTime start = LocalDateTime.ofEpochSecond(startTime, 0, ZoneOffset.UTC);
            LocalDateTime prev = LocalDateTime.ofEpochSecond(currentOccurrence, 0, ZoneOffset.UTC);
            LocalDateTime now = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);

            List<LocalDateTime> dates = recurrence.getDates(start, prev, now);
            if ((recurrence.getUntil() != null && now.isAfter(ChronoLocalDateTime.from(recurrence.getUntil()))) || count == recurrence.getCount()) {
                return epoch - currentOccurrence;
            }
            if (dates.size() > 1 || count == 0) count++;

            currentOccurrence = dates.getLast().toEpochSecond(ZoneOffset.UTC);
            return epoch - currentOccurrence;
        }

        /**
         * Gets the time until the next occurrence starts.
         *
         * @param timeSinceOccurrenceStarted Seconds since the current occurrence started
         * @return Seconds until the next occurrence starts
         * @throws Exception If this is a one-time event or if the recurrence rule has ended
         */
        protected long getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted) throws Exception {
            Recur<LocalDateTime> recurrence = super.getRecurrence();

            if (recurrence == null) {
                throw new Exception("Single event schedule has ended");
            }

            LocalDateTime start = LocalDateTime.ofEpochSecond(startTime, 0, ZoneOffset.UTC);
            LocalDateTime current = LocalDateTime.ofEpochSecond(currentOccurrence, 0, ZoneOffset.UTC);
            LocalDateTime next = recurrence.getNextDate(start, current);

            if (next == null) {
                throw new Exception("Recurring event schedule has ended");
            }

            long duration = next.toEpochSecond(ZoneOffset.UTC) - currentOccurrence;
            return duration - timeSinceOccurrenceStarted;
        }
    }
}
