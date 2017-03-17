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

            if (!deviceId.equals(id.deviceId)) return false;
            return userId.equals(id.userId);
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

    public DeviceNotificationToken() {
    }

    public DeviceNotificationToken(Id id, String token) {
        this.id = id;
        this.token = token;
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", token='" + token + '\'' +
            '}';
    }
}
