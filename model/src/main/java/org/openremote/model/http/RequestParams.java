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
package org.openremote.model.http;

import org.openremote.model.util.TextUtil;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.*;
import java.net.URI;

public class RequestParams {

    @HeaderParam(HttpHeaders.AUTHORIZATION)
    public String authorization;

    @HeaderParam("X-Forwarded-Proto")
    public String forwardedProtoHeader;

    @HeaderParam("X-Forwarded-Host")
    public String forwardedHostHeader;

    @Context
    public UriInfo uriInfo;

    public String getBearerAuth() {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.split(" ").length != 2)
            return null;
        return authorization.split(" ")[1];
    }

    /**
     * Handles reverse proxying and returns the request base URI
     */
    public UriBuilder getExternalRequestBaseUri() {
        URI uri = this.uriInfo.getRequestUri();
        String scheme = TextUtil.isNullOrEmpty(this.forwardedProtoHeader) ? uri.getScheme() : this.forwardedProtoHeader;
        int port = uri.getPort();
        String host = uri.getHost();

        if (this.forwardedHostHeader != null) {
            String[] hostAndPort = this.forwardedHostHeader.split(":");
            if (hostAndPort.length == 1) {
                host = hostAndPort[0];
            } else if (hostAndPort.length == 2) {
                host = hostAndPort[0];
                port = Integer.parseInt(hostAndPort[1]);
            }
        }

        return this.uriInfo.getBaseUriBuilder().scheme(scheme).host(host).port(port);
    }
}
