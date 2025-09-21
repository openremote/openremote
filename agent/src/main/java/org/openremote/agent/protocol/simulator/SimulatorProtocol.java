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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
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

                ScheduledFuture<?> updateValueFuture = scheduleReplay(attributeRef, simulatorReplayDatapoints);
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

        long timeSinceOccurrenceStarted = schedule.map(s -> s.getTimeSinceOccurrenceStarted(now))
            .orElse(now % defaultReplayLoopDuration); // Remainder since 00:00

        // Find datapoint with timestamp after the current occurrence
        SimulatorReplayDatapoint nextDatapoint = Arrays.stream(simulatorReplayDatapoints)
            .filter(replaySimulatorDatapoint -> replaySimulatorDatapoint.timestamp > timeSinceOccurrenceStarted)
            .findFirst()
            .orElse(simulatorReplayDatapoints[0]);

        boolean singleOccurrence = schedule
                .map(s -> s.getStart() != null && s.getRecurrence() == null)
                .orElse(false);
        boolean lastDatapoint = singleOccurrence && Arrays.stream(simulatorReplayDatapoints)
                .allMatch(n -> n.timestamp <= nextDatapoint.timestamp);
        if (nextDatapoint == null) {
            LOG.warning("Next datapoint not found so replay cancelled: " + attributeRef);
            return null;
        }

        long nextRun = nextDatapoint.timestamp;

        try {
            nextRun = getDelay(nextRun, timeSinceOccurrenceStarted, schedule.orElse(null));
        } catch (Exception e) {
            LOG.warning(e.getMessage() + attributeRef);
            return null;
        }

        try {
            if (attribute.getMeta().get(HAS_PREDICTED_DATA_POINTS).flatMap(AbstractNameValueHolder::getValue).orElse(false)) {
                List<ValueDatapoint<?>> current = new ArrayList<>();
                List<ValueDatapoint<?>> next = new ArrayList<>();
                long occurrenceDuration = 0;
                if (!singleOccurrence) {
                    occurrenceDuration = getOccurrenceDuration(schedule.orElse(null));
                }
                for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
                    long timestamp = getDelay(d.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null)) + now;
                    current.add(new SimulatorReplayDatapoint(timestamp*1000, d.value).toValueDatapoint());
                    if (!singleOccurrence) {
                        // TODO: until next startdate will cause this value to be way further into the future
                        next.add(new SimulatorReplayDatapoint((timestamp+occurrenceDuration)*1000, d.value).toValueDatapoint());
                    }
                }
                current.addAll(next);
                updateLinkedAttributePredictedDataPoints(attributeRef, current);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception thrown when updating value: %s", e);
        }

        LOG.fine("Next update for asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " in " + nextRun + " second(s)");
        return scheduledExecutorService.schedule(() -> {
            LOG.fine("Updating asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " with value " + nextDatapoint.value.toString());
            try {
                updateLinkedAttribute(attributeRef, nextDatapoint.value);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Exception thrown when updating value: %s", e);
            }

            if (lastDatapoint) {
                LOG.warning("Last datapoint in single occurrence event");
                replayMap.remove(attributeRef);
                return;
            }

            ScheduledFuture<?> future = scheduleReplay(attributeRef, simulatorReplayDatapoints);
            if (future != null) {
                replayMap.put(attributeRef, future);
            } else {
                replayMap.remove(attributeRef);
            }
        }, nextRun, TimeUnit.SECONDS);
    }

    public static class Schedule implements Serializable {

        @JsonPropertyDescription("Set a start date, if not provided, starts immediately." +
                " When the replay datapoint timestamp is 0 it will insert it at 00:00.")
        @JsonSchemaFormat("date-time")
        @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_STRING_TYPE)
        protected Date start;

        @JsonPropertyDescription("Not implemented, within the recurrence rule you can specify an end date.")
        @JsonSchemaFormat("date-time")
        @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_STRING_TYPE)
        protected Date end;

        @JsonPropertyDescription("The recurrence schedule follows the RFC 5545 RRULE format.")
        @JsonSchemaInject(merge = false, jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_STRING_TYPE)
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
        private long startTime;
        @JsonIgnore
        private long count;
        @JsonIgnore
        private long currentOccurrence;

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
         * @return Seconds until the next occurrence starts
         * @throws Exception If this is a one-time event or if the recurrence rule has ended
         */
        protected long getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted) throws Exception {
            Recur<LocalDateTime> recurrence = getRecurrence();

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

    /**
     * Calculates the delay in seconds until the {@link SimulatorReplayDatapoint} should be played.
     *
     * @param point The {@link SimulatorReplayDatapoint#timestamp} to calculate the delay for.
     * @param timeSinceOccurrenceStarted The time since the occurrence started in seconds.
     * @return The delay in seconds until the {@link SimulatorReplayDatapoint} should be replayed.
     * @throws Exception If the one-time occurrence or recurring schedule has ended.
     */
    public static long getDelay(long point, long timeSinceOccurrenceStarted, Schedule schedule) throws Exception {
        if (point <= timeSinceOccurrenceStarted) {
            return point + getTimeUntilNextOccurrence(timeSinceOccurrenceStarted, schedule);
        }
        return point - timeSinceOccurrenceStarted;
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
     * @throws Exception If the one-time occurrence or recurring schedule has ended.
     */
    public static long getTimeUntilNextOccurrence(long timeSinceOccurrenceStarted, Schedule schedule) throws Exception {
        if (schedule != null) {
            return schedule.getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
        }
        return 86400L - timeSinceOccurrenceStarted;
    }
}
