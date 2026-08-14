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
package org.openremote.manager.datapoint;

import org.openremote.agent.protocol.ProtocolPredictedDatapointService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetPredictedDatapoint;
import org.openremote.model.datapoint.AssetPredictedDatapointEvent;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.security.ClientRole;
import org.openremote.model.value.MetaItemType;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.HOURS;

public class AssetPredictedDatapointService extends AbstractDatapointService<AssetPredictedDatapoint> implements ProtocolPredictedDatapointService {

    private static final Logger LOG = Logger.getLogger(AssetPredictedDatapointService.class.getName());
    protected ClientEventService clientEventService;
    protected ManagerIdentityService identityService;
    protected AssetStorageService assetStorageService;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        clientEventService = container.getService(ClientEventService.class);
        identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);

        clientEventService.addSubscriptionAuthorizer((requestedRealm, auth, subscription) -> {
            if (!subscription.isEventType(AssetPredictedDatapointEvent.class) || auth == null) {
                return false;
            }

            @SuppressWarnings("unchecked")
            EventSubscription<AssetPredictedDatapointEvent> predictedDatapointSubscription =
                (EventSubscription<AssetPredictedDatapointEvent>) subscription;

            if (identityService.getIdentityProvider().isRestrictedUser(auth)) {
                EventFilter<AssetPredictedDatapointEvent> existingFilter = predictedDatapointSubscription.getFilter();
                predictedDatapointSubscription.setFilter(event -> {
                    AssetPredictedDatapointEvent filteredEvent = existingFilter == null ? event : existingFilter.apply(event);
                    if (filteredEvent == null) {
                        return null;
                    }

                    String assetId = filteredEvent.getRef().getId();
                    String attributeName = filteredEvent.getRef().getName();

                    if (!assetStorageService.isUserAsset(auth.getUserId(), assetId)) {
                        return null;
                    }

                    Asset<?> asset = assetStorageService.find(assetId, true);
                    Attribute<?> attribute = asset != null ? asset.getAttribute(attributeName).orElse(null) : null;
                    if (attribute == null || !attribute.getMeta().getValue(MetaItemType.ACCESS_RESTRICTED_READ).orElse(false)) {
                        return null;
                    }

                    return filteredEvent;
                });
            }

            return (auth.isSuperUser() || auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID))
                && identityService.getIdentityProvider().isRealmActiveAndAccessible(auth, requestedRealm);
        });

        container.getService(ManagerWebService.class).addApiSingleton(
            new AssetPredictedDatapointResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                container.getService(AssetStorageService.class),
                this
            )
        );
    }

    @Override
    public void start(Container container) throws Exception {
        dataPointsPurgeScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
            this::purgeDatapoints,
            getFirstPurgeMillis(timerService.getNow()),
            Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS
        );
    }

    public void updateValue(AttributeRef attributeRef, Object value, LocalDateTime timestamp) {
        updateValue(attributeRef.getId(), attributeRef.getName(), value, timestamp);
    }

    public void updateValue(String assetId, String attributeName, Object value, LocalDateTime timestamp) {
        upsertValue(assetId, attributeName, value, timestamp);
        publishPredictedDatapointsEvent(assetId, attributeName);
    }

    public void updateValues(String assetId, String attributeName, List<ValueDatapoint<?>> valuesAndTimestamps) {
        if (valuesAndTimestamps == null || valuesAndTimestamps.isEmpty()) {
            return;
        }

        upsertValues(assetId, attributeName, valuesAndTimestamps);
        publishPredictedDatapointsEvent(assetId, attributeName);
    }

    public void purgeValues(String assetId, String attributeName) {
        int deleted = persistenceService.doReturningTransaction(em -> em.createQuery(
            "delete from " + getDatapointClass().getSimpleName() + " dp where dp.assetId=?1 and dp.attributeName=?2"
        ).setParameter(1, assetId).setParameter(2, attributeName).executeUpdate());

        if (deleted > 0) {
            publishPredictedDatapointsEvent(assetId, attributeName);
        }
    }

    public void purgeValuesBefore(String assetId, String attributeName, Instant timestamp) {
        int deleted = persistenceService.doReturningTransaction(em -> em.createQuery(
            "delete from " + getDatapointClass().getSimpleName() + " dp where dp.assetId=?1 and dp.attributeName=?2 and dp.timestamp<?3"
        ).setParameter(1, assetId).setParameter(2, attributeName).setParameter(3, Timestamp.from(timestamp)).executeUpdate());

        if (deleted > 0) {
            publishPredictedDatapointsEvent(assetId, attributeName);
        }
    }

    @Override
    protected Class<AssetPredictedDatapoint> getDatapointClass() {
        return AssetPredictedDatapoint.class;
    }

    @Override
    protected String getDatapointTableName() {
        return AssetPredictedDatapoint.TABLE_NAME;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected long getFirstPurgeMillis(Instant currentTime) {
        return super.getFirstPurgeMillis(currentTime) - 1800000; // Run half hour before default
    }

    protected void publishPredictedDatapointsEvent(String assetId, String attributeName) {
        if (clientEventService == null) {
            return;
        }

        clientEventService.publishEvent(
            new AssetPredictedDatapointEvent(new AttributeRef(assetId, attributeName), timerService.getNow())
        );
    }

    protected void purgeDatapoints() {
        try {
            // Purge data points not in the above list using default duration
            LOG.finest("Purging predicted data points older than now");
            doPurge("where dp.timestamp < :dt", Date.from(timerService.getNow().truncatedTo(HOURS)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to run data points purge", e);
        }
    }
}
