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
package org.openremote.container.security.keycloak;

import org.keycloak.representations.AccessToken;
import org.openremote.container.security.AuthContext;

/**
 * Keycloak-based authorization.
 */
public class AccessTokenAuthContext implements AuthContext {

    final protected String authenticatedRealm;
    final protected AccessToken accessToken;

    public AccessTokenAuthContext(String authenticatedRealm, AccessToken accessToken) {
        this.authenticatedRealm = authenticatedRealm;
        this.accessToken = accessToken;
    }

    @Override
    public String getAuthenticatedRealm() {
        return authenticatedRealm;
    }

    @Override
    public String getUsername() {
        return accessToken.getPreferredUsername();
    }

    @Override
    public String getUserId() {
        return accessToken.getSubject();
    }

    @Override
    public String getClientId() {
        return accessToken.getIssuedFor();
    }

    @Override
    public boolean  hasRealmRole(String role) {
        return accessToken.getRealmAccess() != null && accessToken.getRealmAccess().isUserInRole(role);
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return accessToken.getResourceAccess().containsKey(resource)
            && accessToken.getResourceAccess().get(resource).isUserInRole(role);
    }
}
