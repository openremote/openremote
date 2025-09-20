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
    protected SimulatorProtocol.Schedule schedule;

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

    public Optional<SimulatorProtocol.Schedule> getSchedule() {
        return Optional.ofNullable(schedule);
    }

    public SimulatorAgentLink setSchedule(SimulatorProtocol.Schedule schedule) {
        this.schedule = schedule;
        return this;
    }
}
