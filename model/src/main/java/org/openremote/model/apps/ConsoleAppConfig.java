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
package org.openremote.model.apps;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsoleAppConfig {

  public static class AppLink implements Serializable {
    protected String displayText;
    protected String pageLink;

    protected AppLink() {}

    public AppLink(String displayText, String pageLink) {
      this.displayText = displayText;
      this.pageLink = pageLink;
    }

    public String getDisplayText() {
      return displayText;
    }

    public String getPageLink() {
      return pageLink;
    }
  }

  public enum MenuPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT
  }

  public ConsoleAppConfig() {}

  public ConsoleAppConfig(
      String realm,
      String initialUrl,
      String url,
      Boolean menuEnabled,
      MenuPosition menuPosition,
      String primaryColor,
      String secondaryColor,
      AppLink[] links) {
    this.realm = realm;
    this.initialUrl = initialUrl;
    this.url = url;
    this.menuEnabled = menuEnabled;
    this.menuPosition = menuPosition;
    this.primaryColor = primaryColor;
    this.secondaryColor = secondaryColor;
    this.links = links;
  }

  @JsonProperty protected Long id;

  @JsonProperty protected String realm;

  @JsonProperty protected String initialUrl;

  @JsonProperty protected String url;

  @JsonProperty protected Boolean menuEnabled;

  @JsonProperty protected MenuPosition menuPosition;

  @JsonProperty protected String primaryColor;

  @JsonProperty protected String secondaryColor;

  @JsonProperty protected AppLink[] links;

  public String getRealm() {
    return realm;
  }
}
