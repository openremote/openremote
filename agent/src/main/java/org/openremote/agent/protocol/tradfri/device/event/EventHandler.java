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

import java.lang.reflect.ParameterizedType;

/** The class that handles events for IKEA TRÅDFRI devices */
public abstract class EventHandler<T> {

  /** Construct the EventHandler class */
  public EventHandler() {}

  /**
   * Handle the event
   *
   * @param event The event that occurred
   */
  public abstract void handle(T event);

  /**
   * Get the class of the event that this event handler handles
   *
   * @return The class of the event that this event handler handles
   */
  @SuppressWarnings("unchecked")
  public Class<T> getEventType() {
    return (Class<T>)
        ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }
}
