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
 * asset subtree, and can only modify asset data in a particular asset subtree.
 */
@Entity
@Table(name = "ASSET_RULES")
public class AssetRulesDefinition extends RulesDefinition {

    @Column(name = "ASSET_ID", nullable = false)
    public String assetId;

    public AssetRulesDefinition() {
    }

    public AssetRulesDefinition(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String assetId) {
        super(id, version, createdOn, lastModified, name, enabled);
        this.assetId = assetId;
    }

    public AssetRulesDefinition(String name, String assetId, String rules) {
        super(name, rules);
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetId='" + assetId + '\'' +
            '}';
    }
}
