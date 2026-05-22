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
package org.openremote.manager.asset;

import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;

/**
 * This is an event for use by internal event subscribers to be notified of an outdated {@link
 * AttributeEvent} that has just been processed.
 */
public class OutdatedAttributeEvent extends Event {
  protected AttributeEvent event;

  public OutdatedAttributeEvent(AttributeEvent event) {
    super(event.getTimestamp());
    this.event = event;
  }

  public AttributeEvent getEvent() {
    return event;
  }
}
