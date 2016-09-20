/*
 * Copyright 2016, OpenRemote Inc.
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

import org.apache.camel.Predicate;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetType;

public class AssetPredicates {

    public static Predicate isPersistenceEventForEntityType(Class<?> type) {
        return exchange -> {
            Class<?> entityType = exchange.getIn().getHeader(PersistenceEvent.HEADER_ENTITY_TYPE, Class.class);
            return type.isAssignableFrom(entityType);
        };
    }

    public static Predicate isPersistenceEventForAssetType(AssetType assetType) {
        return exchange -> {
            if (!(exchange.getIn().getBody() instanceof PersistenceEvent))
                return false;
            PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
            Asset asset = (Asset) persistenceEvent.getEntity();
            return assetType.equals(asset.getWellKnownType());
        };
    }
}
