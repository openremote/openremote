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
package org.openremote.container.web.socket;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.openremote.model.Constants;

import javax.ws.rs.WebApplicationException;

import java.security.Principal;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

public abstract class WebsocketAuth {

    final protected Principal principal;

    public WebsocketAuth(Principal principal) {
        this.principal = principal;
    }

    @SuppressWarnings("unchecked")
    public KeycloakPrincipal<KeycloakSecurityContext> getCallerPrincipal() {
        KeycloakPrincipal<KeycloakSecurityContext> keycloakPrincipal = (KeycloakPrincipal<KeycloakSecurityContext>) principal;
        if (keycloakPrincipal == null) {
            throw new WebApplicationException("Websocket session is not authenticated, can't access user principal", FORBIDDEN);
        }
        return keycloakPrincipal;
    }

    public AccessToken getAccessToken() {
        return getCallerPrincipal().getKeycloakSecurityContext().getToken();
    }

    public String getUsername() {
        return getAccessToken().getPreferredUsername();
    }

    public String getUserId() {
        return getAccessToken().getSubject();
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
        return getAuthenticatedRealm().equals(Constants.MASTER_REALM) && hasRealmRole(Constants.REALM_ADMIN_ROLE);
    }

    public boolean hasRealmRole(String role) {
        return getAccessToken().getRealmAccess().isUserInRole(role);
    }

    public abstract boolean isUserInRole(String role);
}
