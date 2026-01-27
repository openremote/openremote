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
package org.openremote.container.web;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Response;

/** A filter for following 300 range response re-directions. */
public class FollowRedirectFilter implements ClientResponseFilter {
  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    if (responseContext.getStatusInfo().getFamily() != Response.Status.Family.REDIRECTION) return;

    Response resp =
        requestContext
            .getClient()
            .target(responseContext.getLocation())
            .request()
            .headers(requestContext.getHeaders())
            .method(requestContext.getMethod());

    responseContext.setEntityStream((InputStream) resp.getEntity());
    responseContext.setStatusInfo(resp.getStatusInfo());
    responseContext.setStatus(resp.getStatus());
  }
}
