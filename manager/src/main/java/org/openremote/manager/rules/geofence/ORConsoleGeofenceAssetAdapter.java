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
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.RulesEngine;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.attribute.AttributeValue;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.geofence.GeofenceDefinition;

import java.util.*;

/**
 * This implementation is for handling geofences on console assets that support the OR Console Geofence Provider (i.e.
 * Android and iOS Consoles):
 * <ul>
 * <li>Asset type: {@link AssetType#CONSOLE}</li>
 * <li>Has the required {@link AttributeValue#CONSOLE_PROVIDER_GEOFENCE} attribute with an
 * {@link AssetMeta#GEOFENCE_ADAPTER} with a value of "ORConsole"</li>
 * </ul>
 * <p>
 * This adapter utilises push notifications to notify assets when their geofences change; a data only (silent) push
 * notification is sent to affected consoles/assets. Consoles can also manually request their geofences (e.g. on
 * startup)
 */
public class ORConsoleGeofenceAssetAdapter extends GeofenceAssetAdapter {

    public static final String NAME = "ORConsole";
    public static final String LOCATION_POST_URL_FORMAT_TEMPLATE = "/%1$s/asset/public/%2$s/updateLocation";
    protected Map<String, RulesEngine.AssetStateLocationPredicates> assetLocationPredicatesMap = new HashMap<>();
    protected NotificationService notificationService;

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        this.notificationService = container.getService(NotificationService.class);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void processLocationPredicates(List<RulesEngine.AssetStateLocationPredicates> modifiedAssetLocationPredicates, boolean initialising) {

        modifiedAssetLocationPredicates.forEach(assetStateLocationPredicates -> {
            assetStateLocationPredicates
                .getLocationPredicates()
                .removeIf(locationPredicate ->
                              !(locationPredicate instanceof BaseAssetQuery.RadialLocationPredicate));

            if (!initialising) {
                Set<AssetState> notifyAssets = new HashSet<>(modifiedAssetLocationPredicates.size());

                RulesEngine.AssetStateLocationPredicates existingPredicates = assetLocationPredicatesMap.get(
                    assetStateLocationPredicates.getAssetState().getId());
                if ((existingPredicates == null || existingPredicates.getLocationPredicates().isEmpty()) && !assetStateLocationPredicates.getLocationPredicates().isEmpty()) {
                    // We're not comparing before and after state as RulesService has done that although it could be
                    // that rectangular location predicates have changed but this will do for now
                    notifyAssets.add(assetStateLocationPredicates.getAssetState());
                }

                notifyAssets.forEach(assetState -> notifyAssetGeofencesChanged(assetState.getId()));
            }

            if (assetStateLocationPredicates.getLocationPredicates().isEmpty()) {
                assetLocationPredicatesMap.remove(assetStateLocationPredicates.getAssetState().getId());
            } else {
                assetLocationPredicatesMap.put(assetStateLocationPredicates.getAssetState().getId(),
                                               assetStateLocationPredicates);
            }
        });
    }

    @Override
    public GeofenceDefinition[] getAssetGeofences(String assetId) {
        RulesEngine.AssetStateLocationPredicates assetStateLocationPredicates = assetLocationPredicatesMap.get(assetId);
        if (assetStateLocationPredicates == null) {
            return new GeofenceDefinition[0];
        }

        return assetStateLocationPredicates.getLocationPredicates().stream()
            .map(locationPredicate ->
                     locationPredicateToGeofenceDefinition(assetStateLocationPredicates.getAssetState(),
                                                           locationPredicate))
            .toArray(GeofenceDefinition[]::new);
    }

    protected GeofenceDefinition locationPredicateToGeofenceDefinition(AssetState assetState, BaseAssetQuery.LocationPredicate locationPredicate) {
        BaseAssetQuery.RadialLocationPredicate radialLocationPredicate = (BaseAssetQuery.RadialLocationPredicate) locationPredicate;
        String id = assetState.getId() + "_" + Integer.toString(radialLocationPredicate.hashCode());
        String postUrl = String.format(LOCATION_POST_URL_FORMAT_TEMPLATE, assetState.getTenantRealm(), assetState.getId());
        return new GeofenceDefinition(id,
                                      radialLocationPredicate.getLat(),
                                      radialLocationPredicate.getLng(),
                                      radialLocationPredicate.getRadius(),
                                      postUrl);
    }

    protected void notifyAssetGeofencesChanged(String assetId) {
        // TODO: implement geofence push notification
//        notificationService.findDeviceToken(assetId);
    }
}
