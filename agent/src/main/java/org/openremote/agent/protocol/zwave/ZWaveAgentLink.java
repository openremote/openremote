/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.zwave;

import java.util.Optional;

import org.openremote.model.asset.agent.AgentLink;

public class ZWaveAgentLink extends AgentLink<ZWaveAgentLink> {

  protected Integer deviceNodeId;
  protected Integer deviceEndpoint;
  protected String deviceValue;

  // For Hydrators
  protected ZWaveAgentLink() {}

  public ZWaveAgentLink(
      String id, Integer deviceNodeId, Integer deviceEndpoint, String deviceValue) {
    super(id);
    this.deviceNodeId = deviceNodeId;
    this.deviceEndpoint = deviceEndpoint;
    this.deviceValue = deviceValue;
  }

  public Optional<Integer> getDeviceNodeId() {
    return Optional.ofNullable(deviceNodeId);
  }

  public Optional<Integer> getDeviceEndpoint() {
    return Optional.ofNullable(deviceEndpoint);
  }

  public Optional<String> getDeviceValue() {
    return Optional.ofNullable(deviceValue);
  }
}
