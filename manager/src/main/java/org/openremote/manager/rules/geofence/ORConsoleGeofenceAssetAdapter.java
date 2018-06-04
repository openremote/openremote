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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.RulesEngine;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.console.ConsoleConfiguration;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.rules.geofence.GeofenceDefinition;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.persistence.PersistenceEvent.*;
import static org.openremote.model.asset.AssetType.CONSOLE;

/**
 * This implementation is for handling geofences on console assets that support the OR Console Geofence Provider (i.e.
 * Android and iOS Consoles):
 * <p>
 * This adapter utilises push notifications to notify assets when their geofences change; a data only (silent) push
 * notification is sent to affected consoles/assets. Consoles can also manually request their geofences (e.g. on
 * startup)
 */
public class ORConsoleGeofenceAssetAdapter extends RouteBuilder implements GeofenceAssetAdapter, ContainerService {

    private static final Logger LOG = Logger.getLogger(ORConsoleGeofenceAssetAdapter.class.getName());
    public static final String NAME = "ORConsole";
    public static final String LOCATION_URL_FORMAT_TEMPLATE = "/asset/%1$s/location";
    protected Map<String, RulesEngine.AssetStateLocationPredicates> assetLocationPredicatesMap = new HashMap<>();
    protected NotificationService notificationService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected Map<String, String> consoleIdRealmMap;


    @Override
    public void init(Container container) throws Exception {
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.notificationService = container.getService(NotificationService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {

        // Find all console assets that use this adapter
        consoleIdRealmMap = assetStorageService.findAll(
            new AssetQuery()
                .select(new BaseAssetQuery.Select(BaseAssetQuery.Include.ALL_EXCEPT_PATH,
                                                  false,
                                                  AttributeType.CONSOLE_PROVIDERS.getName()))
                .type(CONSOLE)
                .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                                new BaseAssetQuery.ObjectValueKeyPredicate("geofence")))
            .stream()
            .filter(ORConsoleGeofenceAssetAdapter::isLinkedToORConsoleGeofenceAdapter)
            .collect(Collectors.toMap(Asset::getId, Asset::getTenantRealm));
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

        // If any console asset was modified in the database, detect geofence provider changes
        from(PERSISTENCE_TOPIC)
            .routeId("ORConsoleGeofenceAdapterAssetChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                if (isPersistenceEventForAssetType(CONSOLE).matches(exchange)) {
                    PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                    final Asset console = (Asset) persistenceEvent.getEntity();
                    processConsoleAssetChange(console, persistenceEvent);
                }
            });
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void processLocationPredicates(List<RulesEngine.AssetStateLocationPredicates> modifiedAssetLocationPredicates, boolean initialising) {

        Set<String> notifyAssets = new HashSet<>(modifiedAssetLocationPredicates.size());

        modifiedAssetLocationPredicates.removeIf(assetStateLocationPredicates -> {
            boolean remove = consoleIdRealmMap.containsKey(assetStateLocationPredicates.getAssetId());

            if (remove) {
                assetStateLocationPredicates
                    .getLocationPredicates()
                    .removeIf(locationPredicate ->
                                  !(locationPredicate instanceof BaseAssetQuery.RadialLocationPredicate));

                if (!initialising) {

                    RulesEngine.AssetStateLocationPredicates existingPredicates = assetLocationPredicatesMap.get(
                        assetStateLocationPredicates.getAssetId());
                    if ((existingPredicates == null || existingPredicates.getLocationPredicates().isEmpty()) && !assetStateLocationPredicates.getLocationPredicates().isEmpty()) {
                        // We're not comparing before and after state as RulesService has done that although it could be
                        // that rectangular location predicates have changed but this will do for now
                        notifyAssets.add(assetStateLocationPredicates.getAssetId());
                    }
                }

                if (assetStateLocationPredicates.getLocationPredicates().isEmpty()) {
                    assetLocationPredicatesMap.remove(assetStateLocationPredicates.getAssetId());
                } else {
                    assetLocationPredicatesMap.put(assetStateLocationPredicates.getAssetId(),
                                                   assetStateLocationPredicates);
                }
            }

            return remove;
        });

        notifyAssets.forEach(this::notifyAssetGeofencesChanged);
    }

    @Override
    public GeofenceDefinition[] getAssetGeofences(String assetId) {
        String realm = consoleIdRealmMap.get(assetId);

        if (realm == null) {
            LOG.finest("Console ID not found in map so cannot retrieve geofences");
            // Asset not supported by this adapter
            return null;
        }

        RulesEngine.AssetStateLocationPredicates assetStateLocationPredicates = assetLocationPredicatesMap.get(assetId);

        if (assetStateLocationPredicates == null) {
            // No geofences exist for this asset
            return new GeofenceDefinition[0];
        }

        return assetStateLocationPredicates.getLocationPredicates().stream()
            .map(locationPredicate ->
                     locationPredicateToGeofenceDefinition(assetStateLocationPredicates.getAssetId(),
                                                           locationPredicate))
            .toArray(GeofenceDefinition[]::new);
    }

    protected GeofenceDefinition locationPredicateToGeofenceDefinition(String assetId, BaseAssetQuery.LocationPredicate locationPredicate) {
        BaseAssetQuery.RadialLocationPredicate radialLocationPredicate = (BaseAssetQuery.RadialLocationPredicate) locationPredicate;
        String id = assetId + "_" + Integer.toString(radialLocationPredicate.hashCode());
        String postUrl = String.format(LOCATION_URL_FORMAT_TEMPLATE,
                                       assetId);
        return new GeofenceDefinition(id,
                                      radialLocationPredicate.getLat(),
                                      radialLocationPredicate.getLng(),
                                      radialLocationPredicate.getRadius(),
                                      postUrl);
    }

    protected void notifyAssetGeofencesChanged(String assetId) {
        Map<String, String> data = new HashMap<>();
        data.put("action", "GEOFENCE_REFRESH");
        notificationService.notifyConsoleSilently(assetId, data);
    }

    protected void processConsoleAssetChange(Asset asset, PersistenceEvent persistenceEvent) {

        withLock(getClass().getSimpleName() + "::processAssetChange", () -> {
            switch (persistenceEvent.getCause()) {

                case INSERT:
                case UPDATE:

                    if (isLinkedToORConsoleGeofenceAdapter(asset)) {
                        String realm = TextUtil.isNullOrEmpty(asset.getTenantRealm()) ? identityService.getIdentityProvider().getTenantForRealmId(asset.getRealmId()).getRealm() : asset.getTenantRealm();
                        consoleIdRealmMap.put(asset.getId(), realm);
                    } else {
                        consoleIdRealmMap.remove(asset.getId());
                    }
                    break;
                case DELETE:

                    consoleIdRealmMap.remove(asset.getId());
                    break;
            }
        });
    }

    protected static boolean isLinkedToORConsoleGeofenceAdapter(Asset asset) {
        return ConsoleConfiguration.getConsoleProvider(asset, "geofence")
            .map(ConsoleProvider::getVersion).map(NAME::equals).orElse(false);
    }
}
