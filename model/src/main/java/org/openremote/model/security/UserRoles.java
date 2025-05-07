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
package org.openremote.model.security;

public class UserRoles {
    private String userId;
    private String[] clientRoles;
    private String[] realmRoles;
    private boolean restrictedUser;

    public UserRoles() {}

    public UserRoles(String userId, String[] clientRoles, String[] realmRoles, boolean restrictedUser) {
        this.userId = userId;
        this.clientRoles = clientRoles;
        this.realmRoles = realmRoles;
        this.restrictedUser = restrictedUser;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String[] getClientRoles() { return clientRoles; }
    public void setClientRoles(String[] clientRoles) { this.clientRoles = clientRoles; }

    public String[] getRealmRoles() { return realmRoles; }
    public void setRealmRoles(String[] realmRoles) { this.realmRoles = realmRoles; }

    public boolean isRestrictedUser() { return restrictedUser; }
    public void setRestrictedUser(boolean restrictedUser) { this.restrictedUser = restrictedUser; }
}
