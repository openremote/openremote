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

/** The class that contains the properties of an IKEA TRÅDFRI plug */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlugProperties extends DeviceProperties {

  /** The on state of the plug (true for on, false for off) */
  @JsonProperty(ApiCode.ON_OFF)
  private Integer on;

  /** Construct the PlugProperties class */
  public PlugProperties() {}

  /**
   * Get the on state of the plug
   *
   * @return The on state of the plug (true for on, false for off)
   */
  public Boolean getOn() {
    return this.on != null && this.on.equals(1);
  }

  /**
   * Set the on state of the plug within the PlugProperties class<br>
   * <i>Note: This does not change the actual plug</i>
   *
   * @param on The new on state for the plug (true for on, false for off)
   */
  public void setOn(Boolean on) {
    this.on = on != null ? on ? 1 : 0 : null;
  }
}
