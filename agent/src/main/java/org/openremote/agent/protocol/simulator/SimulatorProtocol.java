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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import net.fortuna.ical4j.model.Recur;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.JSONSchemaUtil;
import org.openremote.model.util.JSONSchemaUtil.*;
import org.openremote.model.value.AbstractNameValueHolder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
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

        Optional<SimulatorProtocol.Schedule> schedule = agentLink.getSchedule();

        long defaultReplayLoopDuration = 86400; // 1 day in seconds
        long now = timerService.getNow().getEpochSecond(); // UTC

        long timeSinceOccurrenceStarted = schedule.map(s -> s.advanceToNextOccurrence(now))
            .orElse(now % defaultReplayLoopDuration); // Remainder since 00:00

        // Find datapoint with timestamp after the current occurrence
        SimulatorReplayDatapoint nextDatapoint = Arrays.stream(simulatorReplayDatapoints)
            .filter(replaySimulatorDatapoint -> replaySimulatorDatapoint.timestamp > timeSinceOccurrenceStarted)
            .findFirst()
            .orElse(simulatorReplayDatapoints[0]);

        if (nextDatapoint == null) {
            LOG.warning("Next datapoint not found so replay cancelled: " + attributeRef);
            return null;
        }

        OptionalLong nextRun = getDelay(nextDatapoint.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null));
        if (nextRun.isEmpty()) {
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

        LOG.fine("Next update for asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " in " + nextRun + " second(s)");
        return scheduledExecutorService.schedule(() -> {
            LOG.fine("Updating asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " with value " + nextDatapoint.value.toString());
            try {
                updateLinkedAttribute(attributeRef, nextDatapoint.value);
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

        long occurrenceDuration = 0;
        boolean isSingleOccurrence = schedule.map(Schedule::getIsSingleOccurrence).orElse(false);
        if (!isSingleOccurrence) occurrenceDuration = getOccurrenceDuration(schedule.orElse(null));

        for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
            OptionalLong delay = getDelay(d.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null));
            if (delay.isEmpty()) return predictedDatapoints;
            long timestamp = delay.getAsLong() + now;

            if (status.equals(PredictedDatapointWindow.BOTH)) {
                predictedDatapoints.add(new SimulatorReplayDatapoint(timestamp*1000, d.value).toValueDatapoint());
            }
            if (!isSingleOccurrence
                    && (status.equals(PredictedDatapointWindow.BOTH)
                    || status.equals(PredictedDatapointWindow.NEXT))
            ) {
                predictedDatapoints.add(new SimulatorReplayDatapoint((timestamp+occurrenceDuration)*1000, d.value).toValueDatapoint());
            }
        }
        return predictedDatapoints;
    }

    public static class Schedule implements Serializable {

        @JsonPropertyDescription("Set a start date, if not provided, starts immediately." +
                " When the replay datapoint timestamp is 0 it will insert it at 00:00.")
        @JsonSchemaFormat("date-time")
        @JsonSchemaTypeRemap(type = String.class)
        protected Date start;

        @JsonPropertyDescription("Not implemented, within the recurrence rule you can specify an end date.")
        @JsonSchemaFormat("date-time")
        @JsonSchemaTypeRemap(type = String.class)
        protected Date end;

        @JsonPropertyDescription("The recurrence schedule follows the RFC 5545 RRULE format.")
        @JsonSchemaTypeRemap(type = String.class)
        @JsonSerialize(converter = RecurStringConverter.class)
        protected Recur<LocalDateTime> recurrence;

        public static class RecurStringConverter extends StdConverter<Recur<?>, String> {

            @Override
            public String convert(Recur<?> value) {
                return value.toString();
            }
        }

        @JsonCreator
        public Schedule(@JsonProperty("start") Date start, @JsonProperty("end") Date end, @JsonProperty("recurrence") String recurrence) {
            Recur<LocalDateTime> recur = null;

            try {
                recur = new Recur<>(recurrence);
            } catch (Exception ignored) {}

            this.start = start;
            this.end = end;
            this.recurrence = recur;
            this.startTime = start.getTime() / 1000;
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

        @JsonIgnore
        private final long startTime;
        @JsonIgnore
        private long count;
        @JsonIgnore
        private long currentOccurrence;

        /**
         * Advance to next occurrence relative to the current epoch time.
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
        public long advanceToNextOccurrence(long epoch) {
            Recur<LocalDateTime> recurrence = getRecurrence();

            if (recurrence == null || epoch < startTime) {
                currentOccurrence = startTime;
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
         * @return Seconds until the next occurrence starts.
         * <p>
         * If this is a one-time event, or if the recurrence rule has ended returns {@code null} instead.
         */
        protected OptionalLong getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted) {
            Recur<LocalDateTime> recurrence = getRecurrence();

            // Single event schedule has ended
            if (recurrence == null) {
                return OptionalLong.empty();
            }

            LocalDateTime start = LocalDateTime.ofEpochSecond(startTime, 0, ZoneOffset.UTC);
            LocalDateTime current = LocalDateTime.ofEpochSecond(currentOccurrence, 0, ZoneOffset.UTC);
            LocalDateTime next = recurrence.getNextDate(start, current);

            // Recurring event schedule has ended
            if (next == null) {
                return OptionalLong.empty();
            }

            long duration = next.toEpochSecond(ZoneOffset.UTC) - currentOccurrence;
            return OptionalLong.of(duration - timeSinceOccurrenceStarted);
        }

        public boolean getIsSingleOccurrence() {
            return Optional.ofNullable(start).map(s -> recurrence == null).orElse(false);
        }
    }

    /**
     * Calculates the delay in seconds until the {@link SimulatorReplayDatapoint} should be played.
     *
     * @param point The {@link SimulatorReplayDatapoint#timestamp} to calculate the delay for.
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The delay in seconds until the {@link SimulatorReplayDatapoint} should be replayed.
     * <p>
     * If this is a one-time event, or if the recurrence rule has ended returns {@code null} instead.
     */
    public static OptionalLong getDelay(long point, long timeSinceOccurrenceStarted, Schedule schedule) {
        if (point <= timeSinceOccurrenceStarted) {
            return getTimeUntilNextOccurrence(timeSinceOccurrenceStarted, schedule)
                    .stream()
                    .map(n -> point + n)
                    .findFirst();
        }
        return OptionalLong.of(point - timeSinceOccurrenceStarted);
    }

    public static long getOccurrenceDuration(Schedule schedule) {
        if (schedule == null) {
            return 86400;
        }

        LocalDateTime start = LocalDateTime.ofEpochSecond(schedule.startTime, 0, ZoneOffset.UTC);
        LocalDateTime current = LocalDateTime.ofEpochSecond(schedule.currentOccurrence, 0, ZoneOffset.UTC);
        LocalDateTime next = schedule.getRecurrence().getNextDate(start, current);

        return next.toEpochSecond(ZoneOffset.UTC) - schedule.currentOccurrence;
    }

    /**
     * Calculates the time in seconds until the next occurrence starts.
     * <p>
     * If no schedule has been defined uses the default 1-day schedule.
     *
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The delay in seconds until the {@link SimulatorReplayDatapoint} should be replayed.
     * <p>
     * If this is a one-time event, or if the recurrence rule has ended returns {@code null} instead.
     */
    public static OptionalLong getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted,  Schedule schedule) {
        if (schedule != null) {
            return schedule.getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
        }
        return OptionalLong.of(86400L - timeSinceOccurrenceStarted);
    }

    /**
     * Determines for what occurrence window to create predicted datapoints
     */
    public enum PredictedDatapointWindow {
        BOTH,
        NONE,
        NEXT
    }
}
