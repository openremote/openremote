/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.manager.event;

import static org.openremote.model.event.shared.EventSubscription.SUBSCRIBED_MESSAGE_PREFIX;
import static org.openremote.model.event.shared.EventSubscription.SUBSCRIBE_MESSAGE_PREFIX;

import java.util.logging.Logger;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.*;
import org.openremote.model.util.ValueUtil;

public class EventTypeConverters implements TypeConverters {

  private static final Logger LOG = Logger.getLogger(EventTypeConverters.class.getName());

  @Converter
  public String writeEvent(SharedEvent event, Exchange exchange) throws Exception {
    return SharedEvent.MESSAGE_PREFIX + ValueUtil.JSON.writeValueAsString(event);
  }

  @Converter
  public String writeEventArray(SharedEvent[] event, Exchange exchange) throws Exception {
    return SharedEvent.MESSAGE_PREFIX + ValueUtil.JSON.writeValueAsString(event);
  }

  @Converter
  public String writeTriggeredEventSubscription(
      TriggeredEventSubscription triggeredEventSubscription, Exchange exchange) throws Exception {
    return TriggeredEventSubscription.MESSAGE_PREFIX
        + ValueUtil.JSON.writeValueAsString(triggeredEventSubscription);
  }

  @Converter
  public SharedEvent readEvent(String string, Exchange exchange) throws Exception {
    if (!string.startsWith(SharedEvent.MESSAGE_PREFIX)) return null;
    string = string.substring(SharedEvent.MESSAGE_PREFIX.length());
    return ValueUtil.JSON.readValue(string, SharedEvent.class);
  }

  @Converter
  public String writeEventSubscription(EventSubscription eventSubscription, Exchange exchange)
      throws Exception {
    return (eventSubscription.isSubscribed() ? SUBSCRIBED_MESSAGE_PREFIX : SUBSCRIBE_MESSAGE_PREFIX)
        + ValueUtil.JSON.writeValueAsString(eventSubscription);
  }

  @Converter
  public EventSubscription readEventSubscription(String string, Exchange exchange)
      throws Exception {
    if (!string.startsWith(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX)) return null;
    string = string.substring(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX.length());
    return ValueUtil.JSON.readValue(string, EventSubscription.class);
  }

  @Converter
  public CancelEventSubscription readCancelEventSubscription(String string, Exchange exchange)
      throws Exception {
    if (!string.startsWith(CancelEventSubscription.MESSAGE_PREFIX)) return null;
    string = string.substring(CancelEventSubscription.MESSAGE_PREFIX.length());
    return ValueUtil.JSON.readValue(string, CancelEventSubscription.class);
  }

  @Converter
  public String writeUnauthorizedEventSubscription(
      UnauthorizedEventSubscription unauthorizedEventSubscription, Exchange exchange)
      throws Exception {
    return UnauthorizedEventSubscription.MESSAGE_PREFIX
        + ValueUtil.JSON.writeValueAsString(unauthorizedEventSubscription);
  }
}
