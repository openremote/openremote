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

import org.openremote.agent.protocol.tradfri.payload.DeviceRequest;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

/** The class that represents an IKEA TRÅDFRI plug */
public class Plug extends Device {

  /** The current properties of the plug */
  private PlugProperties properties;

  /** The new properties of the plug in the update queue */
  private PlugProperties newProperties;

  /**
   * Construct the Plug class
   *
   * @param name The name of the plug
   * @param creationDate The creation date of the plug
   * @param instanceId The instance id of the plug
   * @param deviceInfo The information of the device
   * @param properties The properties of the plug
   * @param coapClient A CoAP client that can be used to communicate with the plug using the IKEA
   *     TRÅDFRI gateway
   */
  public Plug(
      String name,
      Long creationDate,
      Integer instanceId,
      DeviceInfo deviceInfo,
      PlugProperties properties,
      CoapClient coapClient) {
    super(name, creationDate, instanceId, deviceInfo, coapClient);
    this.properties = properties;
    this.newProperties = new PlugProperties();
  }

  /**
   * Get the properties of the plug
   *
   * @return The properties of the plug
   */
  @Override
  public PlugProperties getProperties() {
    return this.properties;
  }

  /**
   * Set the properties of the plug
   *
   * @param properties The properties of the plug
   */
  @Override
  public void setProperties(DeviceProperties properties) {
    if (properties instanceof PlugProperties) this.properties = (PlugProperties) properties;
  }

  /**
   * Get the on / off state of the plug
   *
   * @return The on state of the plug (true for on, false for off)
   */
  public Boolean getOn() {
    return properties.getOn();
  }

  /**
   * Update the on / off state of the plug in the update queue
   *
   * @param on The new on / off state for the plug (true for on, false for off)
   */
  public void updateOn(Boolean on) {
    newProperties.setOn(on);
  }

  /**
   * Set the on / off state of the plug
   *
   * @param on The new on / off state for the plug (true for on, false for off)
   * @return True if successfully updated the plug, false if not
   */
  public boolean setOn(Boolean on) {
    PlugProperties newProperties = new PlugProperties();
    newProperties.setOn(on);
    return applyUpdate(newProperties);
  }

  /**
   * Apply updates to the plug
   *
   * @param newProperties The new properties to apply to the plug
   * @return True if successfully updated the plug, false if not
   */
  private boolean applyUpdate(PlugProperties newProperties) {
    DeviceRequest request = new DeviceRequest();
    request.setPlugProperties(new PlugProperties[] {newProperties});
    String response =
        coapClient.put(
            ApiEndpoint.getUri(ApiEndpoint.DEVICES, String.valueOf(getInstanceId())),
            request,
            String.class);
    return response != null;
  }

  /**
   * Apply the updates in the update queue to the plug
   *
   * @return True if successfully updated the plug, false if not
   */
  public boolean applyUpdates() {
    boolean success = applyUpdate(newProperties);
    newProperties = new PlugProperties();
    return success;
  }
}
