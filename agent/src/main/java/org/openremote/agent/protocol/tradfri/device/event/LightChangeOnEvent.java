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
import org.openremote.agent.protocol.tradfri.device.LightProperties;

/**
 * The class that represents a light on / off state changed event that occurred to an IKEA TRÅDFRI
 * light
 */
public class LightChangeOnEvent extends LightChangeEvent {

  /**
   * Construct the LightChangeOnEvent class
   *
   * @param light The light for which the event occurred
   * @param oldProperties The old properties of the light (from before the event occurred)
   * @param newProperties The new properties of the light (from after the event occurred)
   */
  public LightChangeOnEvent(
      Light light, LightProperties oldProperties, LightProperties newProperties) {
    super(light, oldProperties, newProperties);
  }

  /**
   * Get the old on / off state of the light (from before the event occurred)
   *
   * @return The old on / off state of the light (true for on, false for off)
   */
  public boolean getOldOn() {
    return getOldProperties().getOn();
  }

  /**
   * Get the new on / off state of the light (from after the event occurred)
   *
   * @return The new on / off state of the light (true for on, false for off)
   */
  public boolean getNewOn() {
    return getNewProperties().getOn();
  }
}
