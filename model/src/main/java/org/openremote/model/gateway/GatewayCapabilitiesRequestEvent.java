/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.model.gateway;

import java.util.Date;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

/** Request the gateway to return a {@link GatewayCapabilitiesResponseEvent} */
public class GatewayCapabilitiesRequestEvent extends SharedEvent implements RespondableEvent {

  public static final String TYPE = "gateway-capabilities-request";
  @JsonIgnore protected Consumer<Event> responseConsumer;

  @JsonCreator
  public GatewayCapabilitiesRequestEvent(@JsonProperty("timestamp") Date timestamp) {
    super(timestamp != null ? timestamp.getTime() : new Date().getTime());
  }

  public GatewayCapabilitiesRequestEvent() {}

  @Override
  public Consumer<Event> getResponseConsumer() {
    return responseConsumer;
  }

  @Override
  public void setResponseConsumer(Consumer<Event> responseConsumer) {
    this.responseConsumer = responseConsumer;
  }
}
