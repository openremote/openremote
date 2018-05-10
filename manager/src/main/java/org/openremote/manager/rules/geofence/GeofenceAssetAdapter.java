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
package org.openremote.manager.rules.geofence;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.rules.RulesEngine;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.attribute.AttributeValue;
import org.openremote.model.rules.geofence.GeofenceDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Defines an adapter that can take a collection of {@link BaseAssetQuery.LocationPredicate} that apply to a given
 * {@link Asset} and can convert the {@link BaseAssetQuery.LocationPredicate}s into Geofences that can be implemented by
 * the asset(s) itself.
 * <p>
 * The adapter is notified when the {@link Asset}s {@link RulesEngine.AssetStateLocationPredicates} change; if the
 * {@link RulesEngine.AssetStateLocationPredicates#getLocationPredicates()} is null or empty then it means that there
 * are no longer any associated with that {@link Asset} so the adapter should clear any that already exist on that
 * {@link Asset}, it is the adapters job to maintain state if required (see initialising flag on {@link
 * #processLocationPredicates}).
 * <p>
 * How the gefences are implemented and 'pushed' to the {@link Asset}s is up to the adapter but when a geofence is
 * triggered on an asset then the asset should update its own location by posting to the asset/location endpoint, the
 * location value sent should be as follows:
 * <ul>
 * <li>Geofence Enter - Send geofence centre point as location (centre point should have been provided in the geofence
 * definition retrieved from the backend)</li>
 * <li>Geofence Exit - Send null (this will clear the devices location and indicate that the asset has left the
 * geofence)</li>
 * </ul>
 */
public abstract class GeofenceAssetAdapter implements ContainerService {

    public static Optional<String> getAssetGeofenceAdapterName(Asset asset) {
        return asset.getAttribute(AttributeValue.CONSOLE_PROVIDER_GEOFENCE.getName())
            .flatMap(attr -> attr.getMetaItem(AssetMeta.GEOFENCE_ADAPTER))
            .flatMap(AbstractValueHolder::getValueAsString);
    }

    @Override
    public void init(Container container) throws Exception {

    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    /**
     * Get the name of this adapter
     */
    public abstract String getName();

    /**
     * The initialising flag is used to indicate that the system is just initialising and the adapter is being made
     * aware of existing {@link BaseAssetQuery.LocationPredicate}s; generally adapters should use this to initialise
     * their own state rather than 'pushing' geofences to the asset(s) it can be assumed that they were previously sent,
     * hence adapters should be more concerned with delta changes (but this is really up to the adapter).
     */
    public abstract void processLocationPredicates(List<RulesEngine.AssetStateLocationPredicates> modifiedAssetLocationPredicates,
                                                   boolean initialising);

    /**
     * Called to return the active geofences for the specified {@link Asset}.
     */
    public abstract GeofenceDefinition[] getAssetGeofences(String assetId);
}
