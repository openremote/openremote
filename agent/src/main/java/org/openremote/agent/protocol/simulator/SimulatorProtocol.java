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
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.AbstractNameValueHolder;

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

        // Purge previously configured predicted datapoints // TODO: does this always get triggered when changing agentLinks?
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

        Optional<SimulatorAgentLink.Schedule> schedule = agentLink.getSchedule();

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
            nextRun = agentLink.getDelay(nextRun, timeSinceOccurrenceStarted);
        } catch (Exception e) {
            LOG.warning(e.getMessage() + attributeRef);
            return null;
        }

        try {
            if (attribute.getMeta().get(HAS_PREDICTED_DATA_POINTS).flatMap(AbstractNameValueHolder::getValue).orElse(false)) {
                List<ValueDatapoint<?>> current = new ArrayList<>();
                List<ValueDatapoint<?>> next = new ArrayList<>();
                long nextOccurrenceDelay = 0;
                if (!singleOccurrence) {
                    nextOccurrenceDelay = agentLink.getTimeUntilNextOccurrence(timeSinceOccurrenceStarted);
                }
                for (SimulatorReplayDatapoint d : simulatorReplayDatapoints) {
                    long timestamp = agentLink.getDelay(d.timestamp, timeSinceOccurrenceStarted) + now;
                    current.add(new SimulatorReplayDatapoint(timestamp*1000, d.value).toValueDatapoint());
                    if (!singleOccurrence) {
                        next.add(new SimulatorReplayDatapoint((timestamp + nextOccurrenceDelay)*1000, d.value).toValueDatapoint());
                    }
                }
                current.addAll(next);
                updateLinkedAttributePredictedDataPoints(attributeRef, current);
            }
        // Error from getDelay can be ignored as this error should already be handled for 'nextRun'
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
}
