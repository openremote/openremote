/*
 * Copyright 2017, OpenRemote Inc.
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

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

/**
 * A client sends this event to the server to request {@link SimulatorState} for the specified
 * {@link Agent} expecting the server to answer "soon". If the server decides that the client
 * doesn't have the right permissions, or if anything else is not in order (e.g. the agent doesn't
 * exist), the server might not react at all.
 */
public class RequestSimulatorState extends SharedEvent implements RespondableEvent {

  protected String agentId;
  @JsonIgnore protected Consumer<Event> responseConsumer;

  public RequestSimulatorState(String agentId) {
    this.agentId = agentId;
  }

  public String getAgentId() {
    return agentId;
  }

  @Override
  public Consumer<Event> getResponseConsumer() {
    return responseConsumer;
  }

  @Override
  public void setResponseConsumer(Consumer<Event> responseConsumer) {
    this.responseConsumer = responseConsumer;
  }
}
