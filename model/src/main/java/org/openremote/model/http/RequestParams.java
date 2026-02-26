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

import jakarta.validation.constraints.NotNull;
import org.openremote.model.util.TextUtil;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.*;
import java.net.URI;

public class RequestParams {

    public static final String BEARER_AUTH_PREFIX = "Bearer ";

    @Context
    public HttpHeaders headers;

    @HeaderParam(HttpHeaders.AUTHORIZATION)
    public String authorization;

    @HeaderParam("X-Forwarded-Proto")
    public String forwardedProtoHeader;

    @HeaderParam("X-Forwarded-Host")
    public String forwardedHostHeader;

    @Context
    public UriInfo uriInfo;

    public String getBearerAuth() {
        if (authorization == null || !authorization.startsWith(BEARER_AUTH_PREFIX))
            return null;
        return getBearerAuth(authorization);
    }

    public URI getExternalSchemeHostAndPort() {
        return getExternalBaseUriBuilder().replacePath("").build();
    }

    /**
     * Handles reverse proxying and returns the request base URI
     */
    public UriBuilder getExternalBaseUriBuilder() {
        URI uri = this.uriInfo.getBaseUri();
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

    public static String getBearerAuth(@NotNull String authorizationHeader) {
        return authorizationHeader.substring(BEARER_AUTH_PREFIX.length()).trim();
    }
}
