/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.notification;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Notification {

  public enum Source {
    INTERNAL,
    CLIENT,
    GLOBAL_RULESET,
    REALM_RULESET,
    ASSET_RULESET
  }

  public enum TargetType {
    REALM,
    USER,
    ASSET,
    CUSTOM
  }

  public static class Target {

    protected TargetType type;
    protected String id;
    protected String locale;
    protected Set<String> allowedLocales = new HashSet<>();
    protected Object data; // Handlers can store temporary data here

    @JsonCreator
    public Target(@JsonProperty("type") TargetType type, @JsonProperty("id") String id) {
      this.type = type;
      this.id = id;
    }

    /**
     * Manual constructor for the Target class
     *
     * @param locale ISO 639-1 code; for example "nl" or "en"
     */
    public Target(TargetType type, String id, String locale) {
      this.type = type;
      this.id = id;
      this.locale = locale;
    }

    public TargetType getType() {
      return type;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public void setLocale(String locale) {
      this.locale = locale;
    }

    public String getLocale() {
      return locale;
    }

    public Target setAllowedLocales(Set<String> allowedLocales) {
      this.allowedLocales = allowedLocales;
      return this;
    }

    public Target addAllowedLocale(String locale) {
      this.allowedLocales.add(locale);
      return this;
    }

    public Target addAllowedLocales(Set<String> allowedLocales) {
      this.allowedLocales.addAll(allowedLocales);
      return this;
    }

    public Set<String> getAllowedLocales() {
      return allowedLocales;
    }

    public Target setData(Object data) {
      this.data = data;
      return this;
    }

    public Object getData() {
      return data;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" + "type=" + type + ", id=" + id + '}';
    }
  }

  public static final String HEADER_SOURCE = Notification.class.getName() + ".SOURCE";
  public static final String HEADER_SOURCE_ID = Notification.class.getName() + ".SOURCEID";

  protected String name;
  protected AbstractNotificationMessage message;
  protected RepeatFrequency repeatFrequency;
  protected String repeatInterval;
  protected List<Target> targets;

  public Notification() {}

  @JsonCreator
  public Notification(
      @JsonProperty("name") String name,
      @JsonProperty("message") AbstractNotificationMessage message,
      @JsonProperty("targets") List<Target> targets,
      @JsonProperty("repeatFrequency") RepeatFrequency repeatFrequency,
      @JsonProperty("repeatInterval") String repeatInterval) {
    this.name = name;
    this.message = message;
    this.targets = targets;
    this.repeatFrequency = repeatFrequency;
    this.repeatInterval = repeatInterval;
  }

  public String getName() {
    return name;
  }

  public Notification setName(String name) {
    this.name = name;
    return this;
  }

  public AbstractNotificationMessage getMessage() {
    return message;
  }

  public Notification setMessage(AbstractNotificationMessage message) {
    this.message = message;
    return this;
  }

  public List<Target> getTargets() {
    return targets;
  }

  public void setTargets(Target... targets) {
    if (targets == null || targets.length == 0) {
      this.targets = null;
    } else {
      this.targets = Arrays.asList(targets);
    }
  }

  public void setTargets(List<Target> targets) {
    this.targets = targets;
  }

  public RepeatFrequency getRepeatFrequency() {
    return repeatFrequency;
  }

  /**
   * This applies a fixed time window to a notification i.e. {@link RepeatFrequency#HOURLY} would
   * mean that a notification sent at 10:59 can then be resent at 11:00.
   */
  public Notification setRepeatFrequency(RepeatFrequency repeatFrequency) {
    this.repeatInterval = null;
    this.repeatFrequency = repeatFrequency;
    return this;
  }

  public String getRepeatInterval() {
    return repeatInterval;
  }

  /**
   * This applies a sliding time window to a notification using an ISO8601 duration string i.e. a
   * value of PT1H would mean has it been at least 1hr since the notification was last sent.
   */
  public Notification setRepeatInterval(String repeatInterval) {
    this.repeatFrequency = null;
    this.repeatInterval = repeatInterval;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "name='"
        + name
        + '\''
        + ", message="
        + message
        + ", repeatFrequency="
        + repeatFrequency
        + ", repeatInterval='"
        + repeatInterval
        + '\''
        + ", targets="
        + targets
        + '}';
  }
}
