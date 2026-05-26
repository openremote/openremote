/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.container.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

/** A filter for updating response headers. */
public class ResponseHeaderUpdateFilter implements ClientResponseFilter {

  protected Set<Map.Entry<String, List<String>>> headers;

  public ResponseHeaderUpdateFilter(Map<String, List<String>> headers) {
    this.headers = headers.entrySet();
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    headers.forEach(
        headerAndValues -> {
          responseContext.getHeaders().remove(headerAndValues.getKey());
          if (!headerAndValues.getValue().isEmpty()) {
            responseContext
                .getHeaders()
                .addAll(headerAndValues.getKey(), headerAndValues.getValue());
          }
        });
  }
}
