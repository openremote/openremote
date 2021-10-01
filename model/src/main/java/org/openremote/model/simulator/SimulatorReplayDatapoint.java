/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.simulator;

import java.io.Serializable;
import java.util.Optional;

/**
 * Represents a value at a point in time defined as the number of seconds of the day.
 */
public class SimulatorReplayDatapoint implements Serializable {

    /**
     * Seconds of the day
     */
    public long timestamp;
    public Object value;

    protected SimulatorReplayDatapoint() {}

    public SimulatorReplayDatapoint(long timestamp, Object value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public Optional<Long> getTimestamp() {
        return Optional.of(timestamp);
    }

    public Optional<Object> getValue() {
        return Optional.ofNullable(value);
    }
}
