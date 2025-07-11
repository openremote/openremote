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
package org.openremote.container.security;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.openremote.container.web.WebClient.REQUEST_PROPERTY_CLIENT_ID;
import static org.openremote.container.web.WebClient.REQUEST_PROPERTY_CLIENT_SECRET;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Add Basic authentication header if request context is configured with clientId and clientSecret.
 */
public class ClientSecretRequestFilter implements ClientRequestFilter {

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    String clientId =
        (String) requestContext.getConfiguration().getProperty(REQUEST_PROPERTY_CLIENT_ID);
    String clientSecret =
        (String) requestContext.getConfiguration().getProperty(REQUEST_PROPERTY_CLIENT_SECRET);
    if (clientSecret != null) {
      try {
        String authorization =
            "Basic "
                + Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes("utf-8"));
        requestContext.getHeaders().add(AUTHORIZATION, authorization);
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
