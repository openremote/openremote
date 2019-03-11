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

import javax.persistence.*;
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

    @Column(name = "ACCESS_PUBLIC_READ", nullable = false)
    protected boolean accessPublicRead;

    @Transient
    protected String realm;

    public AssetRuleset() {
    }

    public AssetRuleset(String name, Lang lang, String rules, String assetId, boolean accessPublicRead) {
        super(name, rules, lang);
        this.assetId = assetId;
        this.accessPublicRead = accessPublicRead;
    }

    public AssetRuleset(long id, long version, Date createdOn, Date lastModified, boolean enabled, String name, Lang lang, String rules, String realm, String assetId, boolean accessPublicRead) {
        super(id, version, createdOn, lastModified, name, enabled, rules, lang);
        this.assetId = assetId;
        this.realm = realm;
        this.accessPublicRead = accessPublicRead;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isAccessPublicRead() {
        return accessPublicRead;
    }

    public void setAccessPublicRead(boolean accessPublicRead) {
        this.accessPublicRead = accessPublicRead;
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
                ", assetId='" + assetId + '\'' +
                ", accessPublicRead='" + accessPublicRead + '\'' +
                '}';
    }
}
