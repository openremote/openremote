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
package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openremote.agent.protocol.tradfri.util.ApiCode;

/** The class that contains the properties of an IKEA TRÅDFRI device */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceProperties {

  /** The instance id of the device */
  @JsonProperty(ApiCode.INSTANCE_ID)
  private Integer instanceId;

  /** Construct the DeviceProperties class */
  public DeviceProperties() {}

  /**
   * Get the instance id of the device
   *
   * @return The instance id of the device
   */
  public Integer getInstanceId() {
    return this.instanceId;
  }

  /**
   * Set the instance id of the device
   *
   * @param instanceId The instance id of the device
   */
  public void setInstanceId(Integer instanceId) {
    this.instanceId = instanceId;
  }
}
