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
import javax.persistence.Transient;
import java.util.Date;

/**
 * Rules that can only be triggered by asset modifications in a particular
 * asset subtree, and can only modify asset data in a particular asset subtree.
 */
@Entity
@Table(name = "ASSET_RULESET")
public class AssetRuleset extends Ruleset {

    public static final String TYPE = "asset";

    @Column(name = "ASSET_ID", length = 22, nullable = false, columnDefinition = "char(22)")
    protected String assetId;

    @Transient
    protected String realmId;

    public AssetRuleset() {
    }

    public AssetRuleset(String assetId) {
        this.assetId = assetId;
    }

    public AssetRuleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String assetId, Lang lang) {
        this(id, version, createdOn, lastModified, name, enabled, null, lang, null, assetId);
    }

    public AssetRuleset(String name, String assetId, String rules, Lang lang) {
        super(name, rules, lang);
        this.assetId = assetId;
    }

    public AssetRuleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String rules, Lang lang, String assetId, String realmId) {
        super(id, version, createdOn, lastModified, name, enabled, rules, lang);
        this.assetId = assetId;
        this.realmId = realmId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
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
                ", lang='" + lang + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", lastModified='" + lastModified + '\'' +
                ", enabled='" + enabled + '\'' +
                ", assetId='" + assetId + '\'' +
                '}';
    }
}
