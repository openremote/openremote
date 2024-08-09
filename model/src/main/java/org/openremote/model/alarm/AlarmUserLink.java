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
package org.openremote.model.alarm;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Entity;

import org.hibernate.annotations.Formula;

@Entity
@Table(name = "ALARM_USER_LINK")
public class AlarmUserLink {
    public static class Id implements Serializable {
        @Column(name = "REALM", length = 36)
        protected String realm;

        @Column(name = "ALARM_ID")
        protected Long alarmId;


        @Column(name = "USER_ID")
        protected String userId;

        protected Id() {
        }

        public Id(String realm, Long alarmId, String userId) {
            this.realm = realm;
            this.alarmId = alarmId;
            this.userId = userId;
        }

        public String getRealm() {
            return realm;
        }

        public Long getAlarmId() {
            return alarmId;
        }

        public String getUserId() {
            return userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Id id = (Id) o;

            if (!realm.equals(id.realm)) return false;
            if (!alarmId.equals(id.alarmId)) return false;
            return userId.equals(id.userId);
        }

        @Override
        public int hashCode() {
            int result = realm.hashCode();
            result = 31 * result + alarmId.hashCode();
            result = 31 * result + userId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "realm='" + realm + '\'' +
                ", alarmId='" + alarmId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
        }
    }

    @EmbeddedId
    protected Id id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Date createdOn;

    @Formula("(select u.USERNAME || CASE WHEN COALESCE(NULLIF(u.FIRST_NAME, ''), NULLIF(u.LAST_NAME, '')) IS NULL THEN '' ELSE ' (' END || COALESCE(u.FIRST_NAME, '') || CASE WHEN NULLIF(u.FIRST_NAME, '') IS NOT NULL AND NULLIF(u.LAST_NAME, '') IS NOT NULL THEN ' ' ELSE '' END || COALESCE(u.LAST_NAME, '') || CASE WHEN COALESCE(NULLIF(u.FIRST_NAME, ''), NULLIF(u.LAST_NAME, '')) IS NULL THEN '' ELSE ')' END from PUBLIC.USER_ENTITY u where u.ID = USER_ID)")
    protected String userFullName;

    protected AlarmUserLink() {
    }

    public AlarmUserLink(Id id) {
        this.id = id;
    }

    public AlarmUserLink(String realm, Long alarmId, String assetId) {
        this(new AlarmUserLink.Id(realm, alarmId, assetId));
    }

    public Id getId() {
        return id;
    }

    public String getUserFullName() {
        return this.userFullName;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlarmUserLink that = (AlarmUserLink) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", createdOn=" + createdOn +
            ", userFullName='" + userFullName + '\'' +
            '}';
    }
}
