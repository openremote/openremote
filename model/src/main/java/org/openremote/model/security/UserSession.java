/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.model.security;

import com.fasterxml.jackson.annotation.JsonCreator;

public class UserSession {
  protected final String ID;
  protected final String username;
  protected final long startTimeMillis;
  protected final String remoteAddress;

  @JsonCreator
  public UserSession(String ID, String username, long startTimeMillis, String remoteAddress) {
    this.ID = ID;
    this.username = username;
    this.startTimeMillis = startTimeMillis;
    this.remoteAddress = remoteAddress;
  }

  public String getUsername() {
    return username;
  }

  public long getStartTimeMillis() {
    return startTimeMillis;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public String getID() {
    return ID;
  }
}
