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
package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Light;

/** The class that represents a light event that occurred to an IKEA TRÅDFRI light */
public class LightEvent extends DeviceEvent {

  /**
   * Construct the LightEvent class
   *
   * @param light The light for which the event occurred
   */
  public LightEvent(Light light) {
    super(light);
  }

  /**
   * Get the light for which the event occurred
   *
   * @return The light for which the event occurred
   */
  public Light getLight() {
    return (Light) getDevice();
  }
}
