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
package org.openremote.model.asset;

import javax.persistence.*;
import java.io.Serializable;

/**
 * An asset can be linked to many users, and a user can have links to many assets.
 * <p>
 * If a user has linked assets, it's a <em>restricted</em> user. Such a user can only
 * access its assigned assets and their protected data, and it has a limited
 * set of client operations available:
 * <ul>
 * <li>
 * When a restricted user client loads asset data, only protected asset details are included.
 * </li>
 * <li>
 * When a restricted user client updates asset data, only a subset of protected data can be changed.
 * </li>
 * </ul>
 * Asset attribute data can be protected with {@link AssetMeta#PROTECTED} and {@link AssetMeta.Access},
 * it is filtered in {@link AbstractAssetAttributes#filterProtected}.
 */
@Entity
@Table(name = "USER_ASSET")
@IdClass(UserAsset.class)
public class UserAsset implements Serializable {

    @Id
    @Column(name = "USER_ID", length = 36)
    protected String userId;

    @Id
    @Column(name = "ASSET_ID", length = 27)
    protected String assetId;

    protected UserAsset() {
    }

    public UserAsset(String userId, String assetId) {
        this.userId = userId;
        this.assetId = assetId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAssetId() {
        return assetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAsset userAsset = (UserAsset) o;

        if (!userId.equals(userAsset.userId)) return false;
        return assetId.equals(userAsset.assetId);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + assetId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "userId='" + userId + '\'' +
            ", assetId='" + assetId + '\'' +
            '}';
    }
}
