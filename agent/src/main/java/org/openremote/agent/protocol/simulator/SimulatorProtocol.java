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
import org.openremote.model.value.AbstractNameValueHolder;

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

        Optional<CalendarEvent> schedule = agentLink.getSchedule();

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

        OptionalLong nextRun = CalendarEvent.getDelay(nextDatapoint.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null));
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
            Optional<CalendarEvent> schedule,
            PredictedDatapointWindow status,
            long timeSinceOccurrenceStarted,
            long now
    ) {
        List<ValueDatapoint<?>> predictedDatapoints = new ArrayList<>();
        if (status.equals(PredictedDatapointWindow.NONE)) return predictedDatapoints;

        if (status.equals(PredictedDatapointWindow.BOTH)) {
            if (schedule.map(s -> s.getCurrent() == null).orElse(false)) return predictedDatapoints;
            for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
                OptionalLong delay = CalendarEvent.getDelay(d.timestamp, timeSinceOccurrenceStarted, schedule.orElse(null));
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

        if (!schedule.map(CalendarEvent::getIsSingleOccurrence).orElse(false)) {
            if (status.equals(PredictedDatapointWindow.BOTH) || status.equals(PredictedDatapointWindow.NEXT)) {
                if (schedule.map(s -> s.getUpcoming() == null).orElse(false)) return predictedDatapoints;
                for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
                    long timeSinceUpcoming = schedule
                            .map(s -> now - s.getUpcoming().toEpochSecond(ZoneOffset.UTC))
                            .orElse(timeSinceOccurrenceStarted - 86400L);
                    OptionalLong delay = CalendarEvent.getDelay(d.timestamp, timeSinceUpcoming, schedule.orElse(null));
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
}
