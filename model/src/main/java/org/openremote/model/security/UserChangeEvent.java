/*
 * Copyright 2018, OpenRemote Inc.
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

import org.openremote.model.event.shared.SharedEvent;

public class UserChangeEvent extends SharedEvent {

    public static final String USER_TOPIC = "seda://UserTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    protected String userId;
    protected String[] roles;
    protected UserEventType userEventType;

    public UserChangeEvent(String userId, String[] roles, UserEventType userEventType) {
        this.userId = userId;
        this.roles = roles;
        this.userEventType = userEventType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public UserEventType getUserEventType() {
        return userEventType;
    }

    public void setUserEventType(UserEventType userEventType) {
        this.userEventType = userEventType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "userId='" + userId + '\'' +
            "role='" + roles + '\'' +
            ", eventType=" + userEventType.toString() +
            '}';
    }
}
