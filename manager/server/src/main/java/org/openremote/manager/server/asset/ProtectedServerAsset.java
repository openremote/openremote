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
package org.openremote.manager.server.asset;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import elemental.json.JsonObject;
import org.openremote.model.asset.ProtectedAsset;

import java.util.Date;

/**
 * Provides constructors we can use in database query result marshaling.
 */
public class ProtectedServerAsset extends ProtectedAsset {

    public ProtectedServerAsset(String id, long version, Date createdOn, String name, String type,
                                String parentId, String parentName, String parentType,
                                String realmId, String tenantRealm, String tenantDisplayName,
                                Geometry location) {
        this(
            id, version, createdOn, name, type,
            parentId, parentName, parentType, null,
            realmId, tenantRealm, tenantDisplayName,
            location,
            null
        );
    }

    public ProtectedServerAsset(String id, long version, Date createdOn, String name, String type,
                                String parentId, String parentName, String parentType, String[] path,
                                String realmId, String tenantRealm, String tenantDisplayName,
                                Geometry location,
                                JsonObject attributes) {
        super(
            id, version, createdOn, name, type,
            parentId, parentName, parentType, path,
            realmId, tenantRealm, tenantDisplayName,
            attributes
        );
        ServerAsset.setCoordinates(this, (Point) location);
    }
}
