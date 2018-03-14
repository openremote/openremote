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
package org.openremote.app.client.admin.users;

import org.openremote.app.client.admin.AdminPlace;

public abstract class AbstractAdminUsersPlace extends AdminPlace {

    final String realm;
    final String userId;

    public AbstractAdminUsersPlace() {
        this(null);
    }

    protected AbstractAdminUsersPlace(String realm) {
        this.realm = realm;
        this.userId = null;
    }

    protected AbstractAdminUsersPlace(String realm, String userId) {
        this.realm = realm;
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realm='" + realm + '\'' +
            ", userId='" + userId + '\'' +
            "}";
    }
}
