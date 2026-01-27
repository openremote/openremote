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

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartRequestEvent extends SharedEvent implements RespondableEvent {

  protected String sshHostname;
  protected int sshPort;
  protected GatewayTunnelInfo info;
  @JsonIgnore protected Consumer<Event> responseConsumer;

  @JsonCreator
  public GatewayTunnelStartRequestEvent(String sshHostname, int sshPort, GatewayTunnelInfo info) {
    this.sshHostname = sshHostname;
    this.sshPort = sshPort;
    this.info = info;
  }

  public GatewayTunnelInfo getInfo() {
    return info;
  }

  public String getSshHostname() {
    return sshHostname;
  }

  public int getSshPort() {
    return sshPort;
  }

  @Override
  public Consumer<Event> getResponseConsumer() {
    return responseConsumer;
  }

  @Override
  public void setResponseConsumer(Consumer<Event> responseConsumer) {
    this.responseConsumer = responseConsumer;
  }

  @Override
  public String toString() {
    return GatewayTunnelStartRequestEvent.class.getSimpleName()
        + "{"
        + "sshHostname='"
        + sshHostname
        + '\''
        + ", sshPort="
        + sshPort
        + ", info="
        + info
        + '}';
  }
}
