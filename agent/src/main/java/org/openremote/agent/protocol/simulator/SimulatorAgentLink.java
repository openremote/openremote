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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import net.fortuna.ical4j.model.Recur;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.simulator.SimulatorReplayDatapoint;
import org.openremote.model.util.JSONSchemaUtil;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

public class SimulatorAgentLink extends AgentLink<SimulatorAgentLink> {

    @JsonPropertyDescription("Used to store a dataset of values that should be replayed (i.e. written to the" +
        " linked attribute) in a continuous loop based on a schedule (default replays every 24h).")
    protected SimulatorReplayDatapoint[] replayData;

    @JsonPropertyDescription("Set always a date, no time information, considered to be 00:00 on that day; if not provided, starts immediately")
    protected LocalDate startDate;

    @JsonPropertyDescription(" uses ISO 8601 duration format; if not provided, 24h; defines the length of the replay loop (and of the filled-in predicted data points if applicable), if replayData contains data points after this duration, those values are ignored and never used\n1")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(converter = TimeUtil.PeriodAndDurationConverter.class)
    // TODO: consider @JsonSchemaFormat("duration") requires new or-mwc-input type
    @JsonSchemaInject(merge = false, jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_STRING_TYPE)
    protected Duration duration;

    @JsonPropertyDescription(" recurrence rule, following RFC 5545 RRULE format; if not provided, repeats indefinitely daily")
    @JsonSerialize(converter = CalendarEvent.RecurStringConverter.class)
    protected Recur<LocalDateTime> recurrence;

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

    public Optional<LocalDate> getStartDate() {
        return Optional.ofNullable(startDate);
    }

    public SimulatorAgentLink setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public Optional<Duration> getDuration() {
        return Optional.ofNullable(duration);
    }

    public SimulatorAgentLink setDuration(Duration duration) {
        this.duration = duration;
        return this;
    }

    public Optional<Recur<LocalDateTime>> getRecurrence() {
        return Optional.ofNullable(recurrence);
    }

    public SimulatorAgentLink setRecurrence(Recur<LocalDateTime> recurrence) {
        this.recurrence = recurrence;
        return this;
    }
}
