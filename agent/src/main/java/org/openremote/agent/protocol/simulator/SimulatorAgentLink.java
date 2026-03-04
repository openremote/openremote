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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.util.JSONSchemaUtil.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import java.util.TimeZone;

@JsonSchemaDefault("{\"timezone\":\"\"}")
public class SimulatorAgentLink extends AgentLink<SimulatorAgentLink> {

    @JsonSchemaDescription("Used to store a dataset of values that should be replayed (i.e. written to the" +
        " linked attribute) in a continuous loop based on a schedule (by default replays every 24h)." +
        " Predicted datapoints can be added by configuring 'Store predicted datapoints' which will insert the datapoints" +
        " immediately as determined by the schedule. Datapoints scheduled after the replay loop are ignored.")
    protected SimulatorReplayDatapoint[] replayData;

    @JsonSchemaTitle("Schedule")
    @JsonSchemaDescription("Overwrites the possible dataset length and when it is replayed." +
        " The dataset can be scheduled to stop based on the UNTIL rule part" +
        " or set to recur by a certain amount using the COUNT rule part. The schedule also allows adjusting the frequency" +
        " at which the dataset is replayed and at what times following the RFC 5545 RRULE format." +
        " If not provided defaults to 24 hours. If the replay data contains datapoints scheduled after the" +
        " default 24 hours or the recurrence rule the datapoints will be ignored.")
    @JsonSchemaFormat("simulator-schedule")
    protected SimulatorProtocol.Schedule schedule;

    @JsonSchemaDescription("The timezone the Simulator should follow to replay the dataset.")
    @JsonSchemaFormat("timezone")
    protected TimeZone timezone;

    // For Hydrators
    protected SimulatorAgentLink() {
    }

    public SimulatorAgentLink(String id) {
        this(id, null, null, null);
    }

    @JsonCreator
    public SimulatorAgentLink(
            @JsonProperty("id") String id,
            @JsonProperty("replayData") SimulatorReplayDatapoint[] replayData,
            @JsonProperty("schedule") SimulatorProtocol.Schedule schedule,
            @JsonProperty("timezone") TimeZone timezone
    ){
        super(id);

        this.replayData = replayData;
        this.schedule = schedule;
        this.timezone = timezone;

        if (schedule == null) {
            LocalDateTime start = LocalDate.now(ZoneId.of("UTC")).atStartOfDay();
            LocalDateTime end = getReplayData()
                    .flatMap(this::getLastReplayDatapointSeconds).map(start::plusSeconds).orElse(start.plusDays(1));
            this.schedule = new SimulatorProtocol.Schedule(start, end, null);
        }
    }

    public Optional<SimulatorReplayDatapoint[]> getReplayData() {
        return Optional.ofNullable(replayData);
    }

    public SimulatorAgentLink setReplayData(SimulatorReplayDatapoint[] replayData) {
        this.replayData = replayData;
        return this;
    }

    public SimulatorProtocol.Schedule getSchedule() {
        return schedule;
    }

    public SimulatorAgentLink setSchedule(SimulatorProtocol.Schedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public Optional<TimeZone> getTimezone() {
        return Optional.ofNullable(timezone);
    }

    public SimulatorAgentLink setTimezone(TimeZone timezone) {
        this.timezone = timezone;
        return this;
    }

    /**
     * Resolves the timestamp seconds of the last (max) {@link SimulatorReplayDatapoint}.
     * @param rd The {@link SimulatorReplayDatapoint}s to find the latest timestamp of.
     * @return The last (max) {@link SimulatorReplayDatapoint#timestamp} seconds.
     * If the resolved time is 0 an empty value is returned instead.
     */
    private Optional<Long> getLastReplayDatapointSeconds(SimulatorReplayDatapoint[] rd) {
        long time = Arrays.stream(rd).map(d -> d.timestamp).reduce(0L, Long::max);
        if (time == 0) return Optional.empty();
        return Optional.of(time);
    }
}
