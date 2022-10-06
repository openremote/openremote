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
package org.openremote.container.security;

import org.openremote.model.Constants;

/**
 * Services should use this interface to access a user's identity and perform authorization checks.
 */
// TODO: Remove this and just use AccessToken from Subject's KeycloakPrincipal
public interface AuthContext {

    String getAuthenticatedRealmName();

    String getUsername();

    String getUserId();

    String getClientId();

    /**
     * @return <code>true</code> if the user is authenticated in the "master" realm and has the realm role "admin".
     */
    default boolean isSuperUser() {
        return Constants.MASTER_REALM.equals(getAuthenticatedRealmName()) && hasRealmRole(Constants.REALM_ADMIN_ROLE);
    }

    boolean hasRealmRole(String role);

    boolean hasResourceRole(String role, String resource);

    default boolean hasResourceRoleOrIsSuperUser(String role, String resource) {
        return hasResourceRole(role, resource) || isSuperUser();
    }

    /**
     * @return <code>true</code> if the user is authenticated in the same realm or if the user is the superuser (admin).
     */
    default boolean isRealmAccessibleByUser(String realm) {
        return realm != null && realm.length() > 0 && (realm.equals(getAuthenticatedRealmName()) || isSuperUser());
    }
}
