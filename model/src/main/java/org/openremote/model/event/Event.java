/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.event;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openremote.model.util.TextUtil;

import jakarta.persistence.*;

/**
 * A timestamped event.
 *
 * <p>The event type string is a lowercase, dash-separated name of the event class without the
 * "event" suffix. For example, the type string of <code>AssetTreeModifiedEvent</code> is <code>
 * asset-tree-modified</code>. The event type is therefore usable in JavaScript frameworks, e.g.
 * when declaring Polymer event listeners.
 */
@MappedSuperclass
public abstract class Event {

  @Temporal(TemporalType.TIMESTAMP)
  @Column(
      name = "TIMESTAMP",
      updatable = false,
      nullable = false,
      columnDefinition = "TIMESTAMP WITH TIME ZONE")
  @JsonIgnore
  protected Date timestamp;

  @Transient protected String messageID;

  protected Event(Long timestamp) {
    if (timestamp != null) {
      this.timestamp = new Date(timestamp);
    }
  }

  protected Event() {}

  public static String getEventType(String simpleClassName) {
    String type = TextUtil.toLowerCaseDash(simpleClassName);
    if (type.length() > 6 && type.endsWith("-event")) type = type.substring(0, type.length() - 6);
    return type;
  }

  public static String getEventType(Class<? extends Event> eventClass) {
    return getEventType(eventClass.getSimpleName());
  }

  @JsonIgnore
  public final String getEventType() {
    return getEventType(getClass());
  }

  @JsonProperty
  public long getTimestamp() {
    return timestamp != null ? timestamp.getTime() : 0L;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = new Date(timestamp);
  }

  public String getMessageID() {
    return messageID;
  }

  public void setMessageID(String messageID) {
    this.messageID = messageID;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "timestamp=" + timestamp + '}';
  }
}
