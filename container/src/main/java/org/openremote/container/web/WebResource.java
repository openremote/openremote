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

import org.keycloak.KeycloakPrincipal;
import org.openremote.model.Container;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.basic.BasicAuthContext;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.security.Principal;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
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

    public WebApplication getApplication() {
        return (WebApplication) application;
    }

    public Container getContainer() {
        return getApplication().getContainer();
    }

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
        if (principal == null) {
            return null;
        }

        if (principal instanceof KeycloakPrincipal) {
            KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) principal;
            return new AccessTokenAuthContext(
                keycloakPrincipal.getKeycloakSecurityContext().getRealm(),
                keycloakPrincipal.getKeycloakSecurityContext().getToken()
            );
        } else if (principal instanceof BasicAuthContext) {
            return (BasicAuthContext) principal;
        } else {
            throw new WebApplicationException("Unsupported user principal type: " + principal, INTERNAL_SERVER_ERROR);
        }
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
