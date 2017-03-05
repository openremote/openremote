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
import org.keycloak.representations.AccessToken;
import org.openremote.container.Container;
import org.openremote.model.Constants;
import org.openremote.model.asset.ProtectedUserAssets;

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

    @SuppressWarnings("unchecked")
    public KeycloakPrincipal<KeycloakSecurityContext> getCallerPrincipal() {
        KeycloakPrincipal<KeycloakSecurityContext> principal =
            (KeycloakPrincipal<KeycloakSecurityContext>) securityContext.getUserPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Request is not authenticated, can't access user principal");
        }
        return principal;
    }

    public AccessToken getAccessToken() {
        return getCallerPrincipal().getKeycloakSecurityContext().getToken();
    }

    public String getUsername() {
        return getAccessToken().getPreferredUsername();
    }

    /**
     * This works only if the current caller is authenticated, we obtain the
     * realm from auth material.
     */
    public String getAuthenticatedRealm() {
        return getCallerPrincipal().getKeycloakSecurityContext().getRealm();
    }

    /**
     * @return <code>true</code> if the user is authenticated in the "master" realm and has the realm role "admin".
     */
    public boolean isSuperUser() {
        return getRealm().equals(Constants.MASTER_REALM) && hasRealmRole(Constants.REALM_ADMIN_ROLE);
    }

    public boolean hasRealmRole(String role) {
        return getAccessToken().getRealmAccess().isUserInRole(role);
    }

    public boolean hasResourceRole(String role, String resource) {
        return getAccessToken().getResourceAccess().containsKey(resource)
            && getAccessToken().getResourceAccess().get(resource).isUserInRole(role);
    }

    public boolean hasResourceRoleOrIsSuperUser(String role, String resource) {
        return hasResourceRole(role, resource) || isSuperUser();
    }

    /**
     * @return <code>true</code> if the user is authenticated in the same realm or if the user is the superuser (admin).
     */
    public boolean isRealmAccessibleByUser(String realm) {
        return realm != null && realm.length() > 0 && (realm.equals(getAuthenticatedRealm()) || isSuperUser());
    }

    /**
     * @return <code>true</code> if the authenticated user is a restricted user (access token claim check).
     */
    public boolean isRestrictedUser() {
        return ProtectedUserAssets.isRestrictedUser(getAccessToken().getOtherClaims());
    }
}
