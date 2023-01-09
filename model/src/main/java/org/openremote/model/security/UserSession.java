/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSession {
    protected final String username;
    @JsonIgnore
    protected final long startTimeMillis;
    protected final String remoteAddress;

    @JsonCreator
    public UserSession(String username, long startTime, String remoteAddress) {
        this.username = username;
        this.startTimeMillis = startTime;
        this.remoteAddress = remoteAddress;
    }

    public String getUsername() {
        return username;
    }

    @JsonProperty("durationMins")
    public long getDuration() {
        return startTimeMillis;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }
}
