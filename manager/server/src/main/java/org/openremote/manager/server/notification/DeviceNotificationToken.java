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
package org.openremote.manager.server.notification;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "DEVICE_NOTIFICATION_TOKEN")
public class DeviceNotificationToken {

    /**
     * The user can only store one token per device.
     */
    static public class Id implements Serializable {

        @Column(name = "DEVICE_ID")
        protected String deviceId;

        @Column(name = "USER_ID", length = 36)
        protected String userId;

        protected Id() {
        }

        public Id(String deviceId, String userId) {
            this.deviceId = deviceId;
            this.userId = userId;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getUserId() {
            return userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Id id = (Id) o;

            return deviceId.equals(id.deviceId) && userId.equals(id.userId);
        }

        @Override
        public int hashCode() {
            int result = deviceId.hashCode();
            result = 31 * result + userId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "deviceId='" + deviceId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
        }
    }

    @EmbeddedId
    protected Id id;

    @Column(name = "TOKEN", nullable = false, length = 4096)
    protected String token;

    @Column(name = "DEVICE_TYPE", nullable = true)
    protected String deviceType;

    public DeviceNotificationToken() {
    }

    public DeviceNotificationToken(Id id, String token, String deviceType) {
        this.id = id;
        this.token = token;
        this.deviceType = deviceType;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", token='" + token + '\'' +
            ", deviceType='" + deviceType + '\'' +
            '}';
    }
}
