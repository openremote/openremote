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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the payload of a response to the authenticate request from the IKEA
 * TRÅDFRI gateway
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateResponse {

  /** The preshared key that can be used to authenticate to the IKEA TRÅDFRI gateway */
  @JsonProperty(ApiCode.PRESHARED_KEY)
  private String presharedKey;

  /** The firmware version of the IKEA TRÅDFRI gateway */
  @JsonProperty(ApiCode.GATEWAY_FIRMWARE_VERSION)
  private String gatewayFirmwareVersion;

  /** Construct the AuthenticateResponse class */
  public AuthenticateResponse() {}

  /**
   * Get the preshared key that can be used to authenticate to the IKEA TRÅDFRI gateway
   *
   * @return The preshared key that can be used to authenticate to the IKEA TRÅDFRI gateway
   */
  public String getPresharedKey() {
    return this.presharedKey;
  }

  /**
   * Get the firmware version of the IKEA TRÅDFRI gateway
   *
   * @return The firmware version of the IKEA TRÅDFRI gateway
   */
  public String getGatewayFirmwareVersion() {
    return this.gatewayFirmwareVersion;
  }

  /**
   * Set the preshared key that can be used to authenticate to the IKEA TRÅDFRI gateway
   *
   * @param presharedKey The new preshared key that can be used to authenticate to the IKEA TRÅDFRI
   *     gateway
   */
  public void setPresharedKey(String presharedKey) {
    this.presharedKey = presharedKey;
  }

  /**
   * Set the firmware version of the IKEA TRÅDFRI gateway
   *
   * @param gatewayFirmwareVersion The new firmware version of the IKEA TRÅDFRI gateway
   */
  public void setGatewayFirmwareVersion(String gatewayFirmwareVersion) {
    this.gatewayFirmwareVersion = gatewayFirmwareVersion;
  }
}
