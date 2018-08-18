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
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.RulesEngine;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.console.ConsoleConfiguration;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.filter.LocationPredicate;
import org.openremote.model.query.filter.ObjectValueKeyPredicate;
import org.openremote.model.query.filter.RadialLocationPredicate;
import org.openremote.model.rules.geofence.GeofenceDefinition;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.persistence.PersistenceEvent.*;
import static org.openremote.model.asset.AssetResource.Util.WRITE_ATTRIBUTE_HTTP_METHOD;
import static org.openremote.model.asset.AssetResource.Util.getWriteAttributeUrl;
import static org.openremote.model.asset.AssetType.CONSOLE;
import static org.openremote.model.syslog.SyslogCategory.RULES;

/**
 * This implementation is for handling geofences on console assets that support the OR Console Geofence Provider (i.e.
 * Android and iOS Consoles):
 * <p>
 * This adapter utilises push notifications to notify assets when their geofences change; a data only (silent) push
 * notification is sent to affected consoles/assets. Consoles can also manually request their geofences (e.g. on
 * startup)
 */
public class ORConsoleGeofenceAssetAdapter extends RouteBuilder implements GeofenceAssetAdapter, ContainerService {

    private static final Logger LOG = SyslogCategory.getLogger(RULES, ORConsoleGeofenceAssetAdapter.class.getName());
    public static final String NAME = "ORConsole";
    public static int NOTIFY_ASSETS_DEBOUNCE_MILLIS = 60000;
    protected Map<String, RulesEngine.AssetStateLocationPredicates> assetLocationPredicatesMap = new HashMap<>();
    protected NotificationService notificationService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected ManagerExecutorService executorService;
    protected Map<String, String> consoleIdRealmMap;
    protected ScheduledFuture notifyAssetsScheduledFuture;
    protected Set<String> notifyAssets;

    @Override
    public void init(Container container) throws Exception {
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.notificationService = container.getService(NotificationService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        this.executorService = container.getService(ManagerExecutorService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {

        // Find all console assets that use this adapter
        consoleIdRealmMap = new HashMap<>();

        assetStorageService.findAll(
            new AssetQuery()
                .select(new BaseAssetQuery.Select(BaseAssetQuery.Include.ALL_EXCEPT_PATH,
                    false,
                    AttributeType.CONSOLE_PROVIDERS.getName()))
                .type(CONSOLE)
                .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                    new ObjectValueKeyPredicate("geofence")))
            .stream()
            .filter(ORConsoleGeofenceAssetAdapter::isLinkedToORConsoleGeofenceAdapter)
            .forEach(asset -> consoleIdRealmMap.put(asset.getId(), asset.getTenantRealm()));
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
    public void processLocationPredicates(List<RulesEngine.AssetStateLocationPredicates> modifiedAssetLocationPredicates) {

        withLock(getClass().getSimpleName() + "::processLocationPredicates", () -> {

            AtomicBoolean notifierDebounce = new AtomicBoolean(false);

            if (notifyAssets == null) {
                notifyAssets = new HashSet<>(modifiedAssetLocationPredicates.size());
            }

            // Remove all entries that relate to consoles that are compatible with this adapter
            modifiedAssetLocationPredicates.removeIf(assetStateLocationPredicates -> {
                boolean remove = consoleIdRealmMap.containsKey(assetStateLocationPredicates.getAssetId());

                if (remove) {
                    // Keep only radial location predicates (only these are supported on iOS and Android)
                    assetStateLocationPredicates
                        .getLocationPredicates()
                        .removeIf(locationPredicate ->
                            !(locationPredicate instanceof RadialLocationPredicate));


                    RulesEngine.AssetStateLocationPredicates existingPredicates = assetLocationPredicatesMap.get(
                        assetStateLocationPredicates.getAssetId());
                    if (existingPredicates == null || !existingPredicates.getLocationPredicates().equals(assetStateLocationPredicates.getLocationPredicates())) {
                        // We're not comparing before and after state as RulesService has done that although it could be
                        // that rectangular location predicates have changed but this will do for now
                        notifyAssets.add(assetStateLocationPredicates.getAssetId());
                        notifierDebounce.set(true);
                    }

                    if (assetStateLocationPredicates.getLocationPredicates().isEmpty()) {
                        if (assetLocationPredicatesMap.remove(assetStateLocationPredicates.getAssetId()) != null) {
                            LOG.info("Clearing location predicates for asset: " + assetStateLocationPredicates.getAssetId());
                            notifyAssets.add(assetStateLocationPredicates.getAssetId());
                            notifierDebounce.set(true);
                        }
                    } else {
                        LOG.info("Setting "
                            + assetStateLocationPredicates.getLocationPredicates().size()
                            + " location predicate(s) for asset: " + assetStateLocationPredicates.getAssetId());

                        assetLocationPredicatesMap.put(assetStateLocationPredicates.getAssetId(),
                            assetStateLocationPredicates);
                    }
                } else if (assetLocationPredicatesMap.remove(assetStateLocationPredicates.getAssetId()) != null) {
                    // Used to be in this map so must have been deleted so ask console to delete its geofences also
                    LOG.info("Clearing location predicates for asset: " + assetStateLocationPredicates.getAssetId());
                    notifyAssets.add(assetStateLocationPredicates.getAssetId());
                    notifierDebounce.set(true);
                }

                return remove;
            });

            if (notifierDebounce.get()) {
                if (notifyAssetsScheduledFuture == null || notifyAssetsScheduledFuture.cancel(false)) {
                    notifyAssetsScheduledFuture = executorService.schedule(() ->
                            withLock(getClass().getSimpleName() + "::notifyAssets",
                                () -> {
                                    notifyAssetGeofencesChanged(notifyAssets);
                                    notifyAssets = null;
                                    notifyAssetsScheduledFuture = null;
                                }),
                        NOTIFY_ASSETS_DEBOUNCE_MILLIS);
                }
            }
        });
    }

    @Override
    public GeofenceDefinition[] getAssetGeofences(String assetId) {
        String realm = consoleIdRealmMap.get(assetId);

        LOG.info("Request for console geofences: " + assetId);

        if (realm == null) {
            LOG.info("Console ID not found in map so cannot retrieve geofences");
            // Asset not supported by this adapter
            return null;
        }

        RulesEngine.AssetStateLocationPredicates assetStateLocationPredicates = assetLocationPredicatesMap.get(assetId);

        if (assetStateLocationPredicates == null) {
            // No geofences exist for this asset
            LOG.info("No geofences exist");
            return new GeofenceDefinition[0];
        }

        GeofenceDefinition[] geofences = assetStateLocationPredicates.getLocationPredicates().stream()
            .map(locationPredicate ->
                locationPredicateToGeofenceDefinition(assetStateLocationPredicates.getAssetId(),
                    locationPredicate))
            .toArray(GeofenceDefinition[]::new);

        LOG.info("Retrieved " + geofences.length + " geofence(s)");
        return geofences;
    }

    protected GeofenceDefinition locationPredicateToGeofenceDefinition(String assetId, LocationPredicate locationPredicate) {
        RadialLocationPredicate radialLocationPredicate = (RadialLocationPredicate) locationPredicate;
        String id = assetId + "_" + Integer.toString(radialLocationPredicate.hashCode());
        String url = getWriteAttributeUrl(new AttributeRef(assetId, AttributeType.LOCATION.getName()));
        return new GeofenceDefinition(id,
            radialLocationPredicate.getLat(),
            radialLocationPredicate.getLng(),
            radialLocationPredicate.getRadius(),
            WRITE_ATTRIBUTE_HTTP_METHOD,
            url);
    }

    /**
     * Send a silent push notification to the console to get it to refresh its geofences
     */
    protected void notifyAssetGeofencesChanged(Set<String> assetIds) {
        if (assetIds == null) {
            return;
        }

        List<String> ids = new ArrayList<>(assetIds);
        ObjectValue data = Values.createObject();
        data.put("action", "GEOFENCE_REFRESH");
        Notification notification = new Notification("GeofenceRefresh", new PushNotificationMessage().setData(data), null);

        // Break into batches of 10 sent every 10s to avoid consoles bombarding the backend
        int rows = 1 + ids.size() / 10;
        IntStream.range(0, rows)
            .forEach(i -> {
                List<String> subIds = ids.subList(10 * i, Math.min(10 + (10 * i), ids.size()));
                notification.setTargets(new Notification.Targets(Notification.TargetType.ASSET, subIds));
                LOG.info("Notifiying consoles that geofences have changed: " + subIds);
                executorService.schedule(() -> notificationService.sendNotification(notification), i * 10000);
            });
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
