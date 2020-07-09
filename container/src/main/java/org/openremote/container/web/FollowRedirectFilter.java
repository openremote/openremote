/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.web;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

/**
 * A filter for following 300 range response re-directions.
 */
public class FollowRedirectFilter implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
    {
        if (responseContext.getStatusInfo().getFamily() != Response.Status.Family.REDIRECTION)
            return;

        Response resp = requestContext.getClient()
            .target(responseContext.getLocation())
            .request()
            .headers(requestContext.getHeaders())
            .method(requestContext.getMethod());

        responseContext.setEntityStream((InputStream) resp.getEntity());
        responseContext.setStatusInfo(resp.getStatusInfo());
        responseContext.setStatus(resp.getStatus());
    }
}
