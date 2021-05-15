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
package org.openremote.manager.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "USER_CONFIGURATION")
public class UserConfiguration {

    @Id
    @Column(name = "USER_ID", length = 36)
    protected String userId;

    @Column(name = "RESTRICTED", nullable = false)
    protected boolean restricted;

    public UserConfiguration() {
    }

    public UserConfiguration(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public UserConfiguration setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public UserConfiguration setRestricted(boolean restricted) {
        this.restricted = restricted;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "userId=" + userId +
            ", restricted=" + restricted +
            '}';
    }
}
