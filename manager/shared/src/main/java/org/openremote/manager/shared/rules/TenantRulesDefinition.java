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
package org.openremote.manager.shared.rules;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that can only be triggered by asset modifications in a particular
 * realm, and can only modify asset data in a particular realm.
 */
@Entity
@Table(name = "TENANT_RULES")
public class TenantRulesDefinition extends RulesDefinition {

    @Column(name = "REALM_ID", nullable = false)
    public String realmId;

    public TenantRulesDefinition() {
    }

    public TenantRulesDefinition(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String realmId) {
        super(id, version, createdOn, lastModified, name, enabled);
        this.realmId = realmId;
    }

    public TenantRulesDefinition(String realmId) {
        this(null, realmId, null);

    }

    public TenantRulesDefinition(String name, String realmId, String rules) {
        super(name, rules);
        this.realmId = realmId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", version='" + version + '\'' +
            ", name='" + name + '\'' +
            ", createdOn='" + createdOn + '\'' +
            ", lastModified='" + lastModified + '\'' +
            ", enabled='" + enabled + '\'' +
            ", realmId='" + realmId + '\'' +
            '}';
    }
}
