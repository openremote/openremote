/*
 * Copyright 2016, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import net.fortuna.ical4j.model.Recur;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.JSONSchemaUtil.*;
import org.openremote.model.value.AbstractNameValueHolder;

import java.io.Serializable;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.value.MetaItemType.HAS_PREDICTED_DATA_POINTS;

public class SimulatorProtocol extends AbstractProtocol<SimulatorAgent, SimulatorAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SimulatorProtocol.class);
    public static final String PROTOCOL_DISPLAY_NAME = "Simulator";

    protected final Map<AttributeRef, ScheduledFuture<?>> replayMap = new ConcurrentHashMap<>();
    protected final Map<AttributeRef, PredictedDatapointWindow> predictedDatapointWindowMap = new ConcurrentHashMap<>();

    public SimulatorProtocol(SimulatorAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "simulator://" + agent.getId();
    }

    @Override
    protected void doStart(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        replayMap.values().forEach(scheduledFuture -> {
            try {
                scheduledFuture.cancel(true);
            } catch (Exception ignored) {

            }
        });
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, SimulatorAgentLink agentLink) {

        // Look for replay data
        agentLink.getReplayData()
            .ifPresent(simulatorReplayDatapoints -> {
                LOG.info("Simulator replay data found for linked attribute: " + attribute);
                AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
                predictedDatapointWindowMap.put(attributeRef, PredictedDatapointWindow.BOTH);

                // Sorting so we can assume positioning
                SimulatorReplayDatapoint[] sorted = Arrays.stream(simulatorReplayDatapoints)
                        .sorted(Comparator.comparingLong(SimulatorReplayDatapoint::getTimestamp))
                        .toArray(SimulatorReplayDatapoint[]::new);
                ScheduledFuture<?> updateValueFuture = scheduleReplay(attributeRef, sorted);
                if (updateValueFuture != null) {
                    replayMap.put(attributeRef, updateValueFuture);
                } else {
                    LOG.warning("Failed to schedule replay update value for simulator replay attribute: " + attribute);
                    replayMap.put(attributeRef, null);
                }
            });
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, SimulatorAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        ScheduledFuture<?> updateValueFuture = replayMap.remove(attributeRef);

        if (updateValueFuture != null) {
            updateValueFuture.cancel(true);
        }

        // Purge previously configured predicted datapoints
        predictedDatapointService.purgeValues(attributeRef.getId(), attribute.getName());
    }

    @Override
    protected void doLinkedAttributeWrite(SimulatorAgentLink agentLink, AttributeEvent event, Object processedValue) {
        if (replayMap.containsKey(event.getRef())) {
            LOG.info("Attempt to write to linked attribute that is configured for value replay so ignoring: " + event.getRef());
            return;
        }

        // Assume write through and endpoint returned the value we sent
        LOG.finest("Write to linked attribute so simulating that the endpoint returned the written value");
        updateSensor(event.getRef(), processedValue);
    }

    /**
     * Call this to simulate a sensor update
     */
    public void updateSensor(AttributeEvent attributeEvent) {
        updateSensor(attributeEvent.getRef(), attributeEvent.getValue().orElse(null), attributeEvent.getTimestamp());
    }

    public void updateSensor(AttributeRef attributeRef, Object value) {
        updateSensor(attributeRef, value, timerService.getCurrentTimeMillis());
    }

    /**
     * Call this to simulate a sensor update using the specified timestamp
     */
    public void updateSensor(AttributeRef attributeRef, Object value, long timestamp) {
        updateLinkedAttribute(attributeRef, value, timestamp);
    }

    public void updateLinkedAttributePredictedDataPoints(AttributeRef attributeRef, List<ValueDatapoint<?>> values) {
        Attribute<?> attribute = linkedAttributes.get(attributeRef);

        if (attribute == null) {
            LOG.log(Level.WARNING, () -> "Update linked attribute predicted data points called for un-linked attribute: " + attributeRef);
            return;
        }

        predictedDatapointService.updateValues(attributeRef.getId(), attribute.getName(), values);
    }

    public Map<AttributeRef, ScheduledFuture<?>> getReplayMap() {
        return replayMap;
    }

    protected ScheduledFuture<?> scheduleReplay(AttributeRef attributeRef, SimulatorReplayDatapoint[] simulatorReplayDatapoints) {
        Attribute<?> attribute = linkedAttributes.get(attributeRef);
        SimulatorAgentLink agentLink = this.agent.getAgentLink(attribute);

        LOG.finest("Scheduling linked attribute replay update");

        Optional<Schedule> schedule = agentLink.getSchedule();

        long defaultReplayLoopDuration = 86400; // 1 day in seconds
        long now = timerService.getNow().getEpochSecond(); // UTC

        long timeSinceOccurrenceStarted;
        if (schedule.isEmpty()) {
            timeSinceOccurrenceStarted = now % defaultReplayLoopDuration; // Remainder since 00:00
        } else {
            OptionalLong active = schedule.get().tryAdvanceActive(now);
            if (active.isEmpty()) {
                LOG.warning("Replay schedule has ended for: " + attributeRef);
                return null;
            }
            timeSinceOccurrenceStarted = now - active.getAsLong();
        }

        // Find datapoint with timestamp after the current occurrence
        SimulatorReplayDatapoint nextDatapoint = Arrays.stream(simulatorReplayDatapoints)
            .filter(replaySimulatorDatapoint -> replaySimulatorDatapoint.timestamp > timeSinceOccurrenceStarted)
            .findFirst()
            .orElse(simulatorReplayDatapoints[0]);

        if (nextDatapoint == null) {
            LOG.warning("Next datapoint not found so replay cancelled: " + attributeRef);
            return null;
        }

        OptionalLong nextRun = Schedule.getDelay(nextDatapoint.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null));
        if (nextRun.isEmpty() || nextRun.getAsLong() < 0) {
            LOG.warning("Replay schedule has ended for: " + attributeRef);
            return null;
        }

        if (attribute.getMeta().get(HAS_PREDICTED_DATA_POINTS).flatMap(AbstractNameValueHolder::getValue).orElse(false)) {
            PredictedDatapointWindow status = predictedDatapointWindowMap.get(attributeRef);
            List<ValueDatapoint<?>> predictedDatapoints =
                    calculatePredictedDatapoints(simulatorReplayDatapoints, schedule, status, timeSinceOccurrenceStarted, now);
            try {
                updateLinkedAttributePredictedDataPoints(attributeRef, predictedDatapoints);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Exception thrown when updating value: %s", e);
            }
            if (status.equals(PredictedDatapointWindow.BOTH) || status.equals(PredictedDatapointWindow.NEXT)) {
                predictedDatapointWindowMap.put(attributeRef, PredictedDatapointWindow.NONE);
            } else if (simulatorReplayDatapoints[simulatorReplayDatapoints.length - 1] == nextDatapoint) {
                predictedDatapointWindowMap.put(attributeRef, PredictedDatapointWindow.NEXT);
            }
        }

        LOG.fine("Next update for asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " in " + nextRun.getAsLong() + " second(s)");
        return scheduledExecutorService.schedule(() -> {
            LOG.fine("Updating asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " with value " + nextDatapoint.value.toString());
            try {
                updateLinkedAttribute(attributeRef, nextDatapoint.value);
                Instant before = Instant.ofEpochSecond(now + nextRun.getAsLong());
                predictedDatapointService.purgeValuesBefore(attributeRef.getId(), attributeRef.getName(), before);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Exception thrown when updating value: %s", e);
            }

            ScheduledFuture<?> future = scheduleReplay(attributeRef, simulatorReplayDatapoints);
            if (future != null) {
                replayMap.put(attributeRef, future);
            } else {
                replayMap.remove(attributeRef);
            }
        }, nextRun.getAsLong(), TimeUnit.SECONDS);
    }

    public List<ValueDatapoint<?>> calculatePredictedDatapoints(
            SimulatorReplayDatapoint[] simulatorReplayDatapoints,
            Optional<Schedule> schedule,
            PredictedDatapointWindow status,
            long timeSinceOccurrenceStarted,
            long now
    ) {
        List<ValueDatapoint<?>> predictedDatapoints = new ArrayList<>();
        if (status.equals(PredictedDatapointWindow.NONE)) return predictedDatapoints;

        if (status.equals(PredictedDatapointWindow.BOTH)) {
            if (schedule.map(s -> s.getCurrent() == null).orElse(false)) return predictedDatapoints;
            for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
                OptionalLong delay = Schedule.getDelay(d.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null));
                if (delay.isEmpty()) {
                    return predictedDatapoints;
                }
                if (now + delay.getAsLong() > now) { // Delay can be negative
                    long timestamp = (now + delay.getAsLong()) * 1000;
                    if (schedule.map(s -> Optional.ofNullable(s.getUpcoming()).map(u -> timestamp > u.toInstant(ZoneOffset.UTC).toEpochMilli()).orElse(false)).orElse(false)) {
                        continue;
                    }
                    predictedDatapoints.add(new SimulatorReplayDatapoint(timestamp, d.value).toValueDatapoint());
                }
            }
        }

        if (!schedule.map(Schedule::getIsSingleOccurrence).orElse(false)) {
            if (status.equals(PredictedDatapointWindow.BOTH) || status.equals(PredictedDatapointWindow.NEXT)) {
                if (schedule.map(s -> s.getUpcoming() == null).orElse(false)) return predictedDatapoints;
                for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
                    long timeSinceUpcoming = schedule
                            .map(s -> now - s.getUpcoming().toEpochSecond(ZoneOffset.UTC))
                            .orElse(timeSinceOccurrenceStarted - 86400L);
                    OptionalLong delay = Schedule.getDelay(d.timestamp, timeSinceUpcoming, schedule.orElse(null));
                    if (delay.isEmpty()) {
                        return predictedDatapoints;
                    }
                    long timestamp = (now + delay.getAsLong()) * 1000;
                    if (schedule.map(s -> s.getHasRecurrenceEnded(timestamp-1)).orElse(false)) {
                        return predictedDatapoints;
                    }
                    predictedDatapoints.add(new SimulatorReplayDatapoint(timestamp, d.value).toValueDatapoint());
                }
            }
        }

        return predictedDatapoints;
    }

    /**
     * Determines for what occurrence window to create predicted datapoints
     */
    public enum PredictedDatapointWindow {
        BOTH,
        NONE,
        NEXT
    }

    public static class Schedule implements Serializable {

        @JsonSchemaDescription("Set a start date, if not provided, considers 00:00 of the current date." +
                " When the replay datapoint timestamp is 0 it will insert it at 00:00, unless the recurrence rule" +
                " specifies any of the following rule parts: FREQ=(HOURLY/MINUTELY/SECONDLY);" +
                " BYSECOND=...;BYMINUTE=...;BYHOUR=...;BYSETPOS=... This will cause the datapoint timestamp to be" +
                " relative to when the first occurrence is scheduled.")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonSchemaFormat("date-time")
        protected LocalDateTime start;

        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonSchemaFormat("date-time")
        protected LocalDateTime end;

        @JsonSchemaDescription("The recurrence schedule follows the RFC 5545 RRULE format.")
        @JsonSchemaTypeRemap(type = String.class)
        @JsonSerialize(converter = CalendarEvent.RecurStringConverter.class)
        protected Recur<LocalDateTime> recurrence;

        @JsonIgnore
        private LocalDateTime current;
        @JsonIgnore
        private LocalDateTime upcoming;

        @JsonCreator
        public Schedule(@JsonProperty("start") LocalDateTime start, @JsonProperty("end") LocalDateTime end, @JsonProperty("recurrence") String recurrence) {
            Recur<LocalDateTime> recur = null;

            try {
                recur = new Recur<>(recurrence);
            } catch (Exception ignored) {}

            this.start = Optional.ofNullable(start).orElse(LocalDate.now().atStartOfDay());
            this.end = end;
            this.recurrence = recur;
            this.upcoming = this.start;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }

        public Recur<LocalDateTime> getRecurrence() {
            return recurrence;
        }

        protected LocalDateTime getCurrent() {
            return current;
        }

        protected LocalDateTime getUpcoming() {
            return upcoming;
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
            long startInSeconds = start.toEpochSecond(ZoneOffset.UTC);

            if (recurrence == null) {
                current = start;
                if (end == null) {
                    return OptionalLong.of(startInSeconds);
                } else if (secondsSinceEpoch > end.toEpochSecond(ZoneOffset.UTC)) {
                    return OptionalLong.empty();
                }
                return OptionalLong.of(startInSeconds);
            }

            // Preemptively get next to determine whether to advance the occurrence or to end the recurrence
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
        public static OptionalLong getDelay(long offset, long timeSinceOccurrenceStarted, Schedule schedule) {
            if (offset <= timeSinceOccurrenceStarted) {
                return getTimeUntilNextOccurrence(timeSinceOccurrenceStarted, schedule)
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
        public static OptionalLong getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted, Schedule schedule) {
            if (schedule != null) {
                return schedule.getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
            }
            return OptionalLong.of(86400L - timeSinceOccurrenceStarted);
        }
    }
}
