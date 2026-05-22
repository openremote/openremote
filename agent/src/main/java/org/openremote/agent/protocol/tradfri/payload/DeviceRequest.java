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
package org.openremote.agent.protocol.tradfri.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.device.PlugProperties;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/** The class that contains the payload for a request to update a IKEA TRÅDFRI device */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceRequest {

  /** The new properties of the light (if the device is a light) */
  @JsonProperty(ApiCode.LIGHT)
  private LightProperties[] lightProperties;

  /** The new properties of the plug (if the device is a plug) */
  @JsonProperty(ApiCode.PLUG)
  private PlugProperties[] plugProperties;

  /** Construct the DeviceRequest class */
  public DeviceRequest() {}

  /**
   * Get the new properties of the light (if the device is a light)
   *
   * @return The new properties of the light
   */
  public LightProperties[] getLightProperties() {
    return this.lightProperties;
  }

  /**
   * Get the new properties of the plug (if the device is a plug)
   *
   * @return The new properties of the plug
   */
  public PlugProperties[] getPlugProperties() {
    return this.plugProperties;
  }

  /**
   * Set the new properties of the light (if the device is a light)
   *
   * @param lightProperties The new properties of the light
   */
  public void setLightProperties(LightProperties[] lightProperties) {
    this.lightProperties = lightProperties;
  }

  /**
   * Set the new properties of the plug (if the device is a plug)
   *
   * @param plugProperties The new properties of the plug
   */
  public void setPlugProperties(PlugProperties[] plugProperties) {
    this.plugProperties = plugProperties;
  }
}
