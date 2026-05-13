/*
 * Copyright 2024, OpenRemote Inc.
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

import org.hibernate.annotations.Formula;


import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ALARM_ASSET_LINK")
public class AlarmAssetLink {
    public static class Id implements Serializable {
        @Column(name = "REALM", nullable = false, length = 36)
        protected String realm;

        @Column(name = "SENTALARM_ID", nullable = false)
        protected Long sentalarmId;


        @Column(name = "ASSET_ID", nullable = false)
        protected String assetId;

        protected Id() {
        }

        public Id(String realm, Long alarmId, String assetId) {
            this.realm = realm;
            this.sentalarmId = alarmId;
            this.assetId = assetId;
        }

        public String getRealm() {
            return realm;
        }

        public Long getAlarmId() {
            return sentalarmId;
        }

        public String getAssetId() {
            return assetId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Id id = (Id) o;

            if (!realm.equals(id.realm)) return false;
            if (!sentalarmId.equals(id.sentalarmId)) return false;
            return assetId.equals(id.assetId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realm, sentalarmId, assetId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "realm='" + realm + '\'' +
                    ", alarmId='" + sentalarmId + '\'' +
                    ", assetId='" + assetId + '\'' +
                    '}';
        }
    }

    @EmbeddedId
    protected Id id;

    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Instant createdOn;

    @Formula("(select a.NAME from ASSET a where a.ID = ASSET_ID)")
    protected String assetName;

    @Formula("(select pa.NAME from ASSET a left outer join ASSET pa on a.PARENT_ID = pa.ID where a.ID = ASSET_ID)")
    protected String parentAssetName;

    protected AlarmAssetLink() {
    }

    public AlarmAssetLink(Id id) {
        this.id = id;
    }

    public AlarmAssetLink(String realm, Long alarmId, String assetId) {
        this(new AlarmAssetLink.Id(realm, alarmId, assetId));
    }

    public Id getId() {
        return id;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getParentAssetName() {
        return parentAssetName;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlarmAssetLink that = (AlarmAssetLink) o;
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
                ", assetName='" + assetName + '\'' +
                ", parentAssetName='" + parentAssetName + '\'' +
                '}';
    }

}
