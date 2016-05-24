/*
 * Copyright 2016, OpenRemote Inc.
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
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

/**
 * Add X-Forwarded-For and X-Forwarded-Proto headers if request context is configured with values.
 */
public class ProxyClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String forwardedFor = (String) requestContext.getConfiguration().getProperty("forwardedFor");
        if (forwardedFor != null) {
            requestContext.getHeaders().add("X-Forwarded-For", forwardedFor);
        }
        String forwardedProto = (String) requestContext.getConfiguration().getProperty("forwardedProto");
        if (forwardedProto != null) {
            requestContext.getHeaders().add("X-Forwarded-Proto", forwardedProto);
        }
    }
}

