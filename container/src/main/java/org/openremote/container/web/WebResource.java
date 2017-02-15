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
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.IDToken;
import org.openremote.container.Container;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class WebResource {

    @Context
    protected Application application;

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

    @SuppressWarnings("unchecked")
    public KeycloakPrincipal<KeycloakSecurityContext> getCallerPrincipal() {
        KeycloakPrincipal<KeycloakSecurityContext> principal =
            (KeycloakPrincipal<KeycloakSecurityContext>) securityContext.getUserPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Request is not authenticated, can't access user principal");
        }
        return principal;
    }

    /**
     * Will try to access the realm from request header. This works even if the
     * current caller is not authenticated, as the header will be set on
     * all API requests by the {@link WebService)}, extracted from a request path segment.
     */
    public String getRealm() {
        String realm = httpHeaders.getHeaderString(WebService.REQUEST_HEADER_REALM);
        if (realm == null || realm.length() == 0) {
            throw new WebApplicationException("Missing header '" + WebService.REQUEST_HEADER_REALM + "':", BAD_REQUEST);
        }
        return realm;
    }

    /**
     * This works only if the current caller is authenticated, we obtain the
     * realm from auth material.
     */
    public String getAuthenticatedRealm() {
        return getCallerPrincipal().getKeycloakSecurityContext().getRealm();
    }

    public IDToken getCallerToken() {
        return getCallerPrincipal().getKeycloakSecurityContext().getIdToken();
    }
}
