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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.TokenPrincipal;
import org.openremote.container.security.basic.BasicAuthContext;

import java.security.Principal;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.openremote.model.Constants.REALM_PARAM_NAME;

public class WebResource implements AuthContext {

    @Context
    protected Application application;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpHeaders httpHeaders;

    @Context
    protected SecurityContext securityContext;

    public String getClientRemoteAddress() {
        return request.getRemoteAddr();
    }

    public String getRequestRealmName() {
        return request.getHeader(REALM_PARAM_NAME);
    }

    public boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    public AuthContext getAuthContext() {
        // The securityContext is a thread-local proxy, careful when/how you call it
        Principal principal = securityContext.getUserPrincipal();
        return switch (principal) {
            case null -> null;
            case TokenPrincipal tokenPrincipal -> tokenPrincipal;
            case BasicAuthContext basicAuthContext -> basicAuthContext;
            default ->
                    throw new WebApplicationException("Unsupported user principal type: " + principal, INTERNAL_SERVER_ERROR);
        };

    }

    // Convenience methods

    @Override
    public String getAuthenticatedRealmName() {
        return isAuthenticated() ? getAuthContext().getAuthenticatedRealmName() : null;
    }

    @Override
    public String getUsername() {
        return isAuthenticated() ? getAuthContext().getUsername() : null;
    }

    @Override
    public String getUserId() {
        return isAuthenticated() ? getAuthContext().getUserId() : null;
    }

    @Override
    public String getClientId() {
        return isAuthenticated() ? getAuthContext().getClientId() : null;
    }

    @Override
    public boolean hasRealmRole(String role) {
        return isAuthenticated() && getAuthContext().hasRealmRole(role);
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return isAuthenticated() && getAuthContext().hasResourceRole(role, resource);
    }
}
