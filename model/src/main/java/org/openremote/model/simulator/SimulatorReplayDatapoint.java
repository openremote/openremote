/*
 * Copyright 2020, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.simulator;

import java.io.Serializable;
import java.util.Optional;

import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.util.JSONSchemaUtil.JsonSchemaTitle;

/**
 * Represents a value at a point in time defined as the number of seconds relative to the occurrence
 * start.
 */
@JsonSchemaTitle("Data point")
public class SimulatorReplayDatapoint implements Serializable {

  /** Seconds relative to occurrence start */
  public long timestamp;

  public Object value;

  protected SimulatorReplayDatapoint() {}

  public SimulatorReplayDatapoint(long timestamp, Object value) {
    this.timestamp = timestamp;
    this.value = value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<Object> getValue() {
    return Optional.ofNullable(value);
  }

  public ValueDatapoint<Object> toValueDatapoint() {
    return new ValueDatapoint<>(timestamp, value);
  }
}
