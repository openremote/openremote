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

import net.fortuna.ical4j.model.Recur;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.syslog.SyslogCategory;

import java.time.LocalDateTime;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class SimulatorProtocol extends AbstractProtocol<SimulatorAgent, SimulatorAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SimulatorProtocol.class);
    public static final String PROTOCOL_DISPLAY_NAME = "Simulator";

    protected final Map<AttributeRef, ScheduledFuture<?>> replayMap = new ConcurrentHashMap<>();
    protected final Map<AttributeRef, LocalDateTime> linkedAtMap = new ConcurrentHashMap<>();

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
                // Seed date for recurrence rule for when startDate is not specified
                LocalDateTime linkedAt = linkedAtMap.put(
                        attributeRef, LocalDateTime.ofInstant(timerService.getNow(), ZoneId.of("UTC"))
                );
                ScheduledFuture<?> updateValueFuture = scheduleReplay(attributeRef, simulatorReplayDatapoints, linkedAt);
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

    public Map<AttributeRef, ScheduledFuture<?>> getReplayMap() {
        return replayMap;
    }

    protected ScheduledFuture<?> scheduleReplay(AttributeRef attributeRef, SimulatorReplayDatapoint[] simulatorReplayDatapoints, LocalDateTime linkedAt) {
        Attribute<?> attribute = linkedAttributes.get(attributeRef);
        SimulatorAgentLink agentLink = this.agent.getAgentLink(attribute);

        LOG.finest("Scheduling linked attribute replay update");

        long now = timerService.getNow().getEpochSecond();
        long duration = agentLink.getDuration().map(d ->
                Optional.ofNullable(d.getDuration()).map(Duration::getSeconds).orElse(
                        Optional.ofNullable(d.getPeriod()).map(p -> d.durationFromPeriod(p).getSeconds()).orElse(86400L)
                )
        ).orElse(86400L); // defaults to day in seconds

        long timeSinceCycleStarted = now % duration;
        long timeUntilNextCycle = duration - timeSinceCycleStarted;

        // Get the next datapoint by checking if its timestamp is after the duration remainder of the current time
        // For duration 1 hour if the remainder of the current time is 20 minutes then get the datapoint after
        SimulatorReplayDatapoint nextDatapoint = Arrays.stream(simulatorReplayDatapoints)
            .filter(replaySimulatorDatapoint -> replaySimulatorDatapoint.timestamp > timeSinceCycleStarted)
            .findFirst()
            .orElse(simulatorReplayDatapoints[0]);

        if (nextDatapoint == null) {
            LOG.warning("Next datapoint not found so replay cancelled: " + attributeRef);
            return null;
        }

        long nextRun = nextDatapoint.timestamp;

        if (agentLink.getRecurrence().isPresent()) {
            Recur<LocalDateTime> recur = agentLink.getRecurrence().get();
            LocalDateTime seedDate = agentLink.getStartDate().map(LocalDate::atStartOfDay).orElse(linkedAt);

            LocalDateTime firstOccurrenceStart = recur.getNextDate(seedDate, linkedAt);
            LocalDateTime currentOccurrenceStart = recur.getNextDate(seedDate,
                // Minus duration to ensure that we get the current occurrence start time
                LocalDateTime.ofEpochSecond(now, 0, ZoneOffset.UTC).minusSeconds(duration)
            );
            LocalDateTime nextOccurrenceStart = recur.getNextDate(seedDate,
                LocalDateTime.ofEpochSecond(now, 0, ZoneOffset.UTC)
            );

            if (nextOccurrenceStart == null) {
                LOG.fine("Next recurrence not found so replay cancelled: " + attributeRef);
                return null;
            }

            // Add time until next occurrence if current cycle has ended
            if (now >= currentOccurrenceStart.toEpochSecond(ZoneOffset.UTC) + duration) {
                nextRun += nextOccurrenceStart.toEpochSecond(ZoneOffset.UTC) - now;
            // Or when the first occurrence is after the seed date and now is before the first occurrence
            } else if (
                firstOccurrenceStart.toEpochSecond(ZoneOffset.UTC) > linkedAt.toEpochSecond(ZoneOffset.UTC)
                && now < firstOccurrenceStart.toEpochSecond(ZoneOffset.UTC)
            ) {
                nextRun += currentOccurrenceStart.toEpochSecond(ZoneOffset.UTC) - now;
            }
        }

        // Add remaining cycle time
        if (nextRun <= timeSinceCycleStarted) {
            nextRun += timeUntilNextCycle;
        } else {
            nextRun -= timeSinceCycleStarted;
        }

        // TODO: think through what happens when startDate and recur are configured
        // If the current time is before the start date compute how many additional seconds to add until the next run
        long timeUntilStartDate = agentLink
            .getStartDate()
            .map(t -> t.toEpochSecond(LocalTime.of(0, 0), ZoneOffset.UTC) - now)
            .orElse(0L);
        if (timeUntilStartDate > 0) {
            nextRun += timeUntilStartDate;
        }

        LOG.fine("Next update for asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " in " + nextRun + " second(s)");
        return scheduledExecutorService.schedule(() -> {
            LOG.fine("Updating asset " + attributeRef.getId() + " for attribute " + attributeRef.getName() + " with value " + nextDatapoint.value.toString());
            try {
                updateLinkedAttribute(attributeRef, nextDatapoint.value);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Exception thrown when updating value: %s", e);
            } finally {
                replayMap.put(attributeRef, scheduleReplay(attributeRef, simulatorReplayDatapoints, linkedAt));
            }
        }, nextRun, TimeUnit.SECONDS);
    }
}
