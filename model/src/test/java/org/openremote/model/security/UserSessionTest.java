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
package org.openremote.model.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class UserSessionTest {

  @Test
  public void jackson2DeserializesUserSessionArray() throws Exception {
    ObjectMapper objectMapper =
        new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

    UserSession[] sessions =
        objectMapper.readValue(
            """
                [{
                    "ID": "session-1",
                    "username": "testuser",
                    "startTimeMillis": 1234,
                    "remoteAddress": "127.0.0.1"
                }]
                """,
            UserSession[].class);

    assertEquals(1, sessions.length);
    assertEquals("session-1", sessions[0].getID());
    assertEquals("testuser", sessions[0].getUsername());
    assertEquals(1234L, sessions[0].getStartTimeMillis());
    assertEquals("127.0.0.1", sessions[0].getRemoteAddress());
  }
}
