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
package org.openremote.model.rules;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that can only be triggered by asset modifications in a particular
 * realm, and can only modify asset data in a particular realm.
 */
@Entity
@Table(name = "TENANT_RULESET")
public class TenantRuleset extends Ruleset {

    public static final String TYPE = "tenant";

    @Column(name = "REALM", nullable = false)
    protected String realm;

    public TenantRuleset() {
    }

    public TenantRuleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String realm, Lang lang) {
        super(id, version, createdOn, lastModified, name, enabled, lang);
        this.realm = realm;
    }

    public TenantRuleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String rules, Lang lang, String realm) {
        super(id, version, createdOn, lastModified, name, enabled, rules, lang);
        this.realm = realm;
    }

    public TenantRuleset(String realm) {
        this(null, realm, null, Lang.GROOVY);

    }

    public TenantRuleset(String name, String realm, String rules, Lang lang) {
        super(name, rules, lang);
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", version='" + version + '\'' +
            ", name='" + name + '\'' +
            ", lang='" + lang + '\'' +
            ", createdOn='" + createdOn + '\'' +
            ", lastModified='" + lastModified + '\'' +
            ", enabled='" + enabled + '\'' +
            ", realm='" + realm + '\'' +
            '}';
    }
}
