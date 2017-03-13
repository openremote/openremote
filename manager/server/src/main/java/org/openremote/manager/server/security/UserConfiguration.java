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
package org.openremote.manager.server.security;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "USER_CONFIGURATION")
public class UserConfiguration {

    @EmbeddedId
    private UserKey userKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="DEVICE_ID")
    @Column(name="TOKEN")
    @CollectionTable(name="USER_CONFIGURATION_NOTIFICATION_TOKEN")
    private Map<String,String> notificationTokenForDeviceId = new HashMap<String, String>();

    private UserConfiguration() {};

    public UserConfiguration(UserKey userKey) {
        this.userKey = userKey;
    }
    public UserKey getUserKey() {
        return userKey;
    }

    public void setUserKey(UserKey userKey) {
        this.userKey = userKey;
    }

    public Map<String, String> getNotificationTokenForDeviceId() {
        return notificationTokenForDeviceId;
    }

    public void setNotificationTokenForDeviceId(Map<String, String> notificationTokenForDeviceId) {
        this.notificationTokenForDeviceId = notificationTokenForDeviceId;
    }
}

@Embeddable
class UserKey implements Serializable {

    @Column(name = "ID_REALM", nullable = false)
    private String realm;


    @Column(name = "ID_USER_ID", nullable = false)
    private String subject;

    private UserKey() {
    }

    public UserKey(String realm, String subject) {
        this.realm = realm;
        this.subject = subject;
    }

    public String getRealm() {
        return realm;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "UserKey{" +
                "realm='" + realm + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }

}