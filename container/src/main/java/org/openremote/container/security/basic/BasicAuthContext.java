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
package org.openremote.container.security.basic;

import org.openremote.container.security.AuthContext;

import java.security.Principal;

/**
 * Basic authorization, any user has all roles and is superuser.
 */
public class BasicAuthContext implements AuthContext, Principal {

    private final String authenticatedRealm;
    private final String userId;
    private final String username;

    public BasicAuthContext(String authenticatedRealm, String userId, String username) {
        this.authenticatedRealm = authenticatedRealm;
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String getName() {
        return username;
    }


    @Override
    public String getAuthenticatedRealmName() {
        return authenticatedRealm;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getClientId() {
        return null;
    }

    @Override
    public boolean hasRealmRole(String role) {
        return true;
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return true;
    }
}
