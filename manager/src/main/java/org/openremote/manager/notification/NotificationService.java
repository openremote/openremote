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
package org.openremote.manager.notification;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Notification;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.console.ConsoleConfiguration;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.notification.AlertAction;
import org.openremote.model.notification.DeviceNotificationToken;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.notification.DeliveryStatus;
import org.openremote.model.user.UserQuery;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class NotificationService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected FCMDeliveryService fcmDeliveryService;
    protected AssetStorageService assetStorageService;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.fcmDeliveryService = new FCMDeliveryService(container);

        container.getService(WebService.class).getApiSingletons().add(
            new NotificationResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    public void storeDeviceToken(String deviceId, String userId, String token, String deviceType) {
        persistenceService.doTransaction(entityManager -> {
            DeviceNotificationToken.Id id = new DeviceNotificationToken.Id(deviceId, userId);
            DeviceNotificationToken deviceToken = new DeviceNotificationToken(id, token, deviceType);
            deviceToken.setUpdatedOn(new Date(timerService.getCurrentTimeMillis()));
            entityManager.merge(deviceToken);
        });
    }

    public void deleteDeviceToken(String deviceId, String userId) {
        persistenceService.doTransaction(entityManager -> {
            DeviceNotificationToken.Id id = new DeviceNotificationToken.Id(deviceId, userId);
            DeviceNotificationToken deviceToken = entityManager.find(DeviceNotificationToken.class, id);
            if (deviceToken != null) {
                entityManager.remove(deviceToken);
            }
        });
    }

    public Optional<DeviceNotificationToken> findDeviceToken(String deviceId, String userId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            DeviceNotificationToken.Id id = new DeviceNotificationToken.Id(deviceId, userId);
            DeviceNotificationToken deviceToken = entityManager.find(DeviceNotificationToken.class, id);
            return Optional.ofNullable(deviceToken);
        });
    }

    public List<DeviceNotificationToken> findAllTokenForUser(String userId) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "SELECT dnt FROM DeviceNotificationToken dnt WHERE dnt.id.userId =:userId order by dnt.updatedOn desc", DeviceNotificationToken.class
        ).setParameter("userId", userId).getResultList());
    }

    public boolean isFcmEnabled() {
        return fcmDeliveryService.isValid();
    }

    public boolean sendFcmMessage(FCMTargetType targetType, String target, FCMNotification notification, Map<String, String> data, FCMMessagePriority priority, Integer expiration) {
        return fcmDeliveryService.sendMessage(targetType, target, notification, data, priority, expiration);
    }

    //TODO: Unify the notification service to support push, email, SMS, etc.
    public void notifyConsole(String consoleId, AlertNotification alertNotification) {
        AssetQuery consoleQuery = new AssetQuery()
            .select(new BaseAssetQuery.Select(BaseAssetQuery.Include.ALL_EXCEPT_PATH,
                                              false,
                                              AttributeType.CONSOLE_PROVIDERS.getName()))
            .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                            new BaseAssetQuery.ObjectValueKeyPredicate("push"))
            .id(consoleId);


        Asset console = assetStorageService.find(consoleQuery);
        if (console == null) {
            LOG.warning("Console cannot be found or doesn't support push notifications");
            return;
        }

        ConsoleProvider consolePushProvider = ConsoleConfiguration.getConsoleProvider(console, "push")
            .orElseThrow(() -> new IllegalStateException("Console push provider is not valid"));

        String pushProviderVersion = consolePushProvider.getVersion();

        if ("fcm".equals(pushProviderVersion)) {
            if (consolePushProvider.getData() == null || !consolePushProvider.getData().getString("token")
                    .map(token -> TextUtil.isNullOrEmpty(token) ? null : token).isPresent()) {
                LOG.warning("Console 'fcm' push provider doesn't contain an FCM token");
                return;
            }

            String token = consolePushProvider.getData().getString("token").orElse(null);

            FCMNotification notification = new FCMNotification(alertNotification.getTitle(), alertNotification.getMessage());
            Map<String, String> data = new HashMap<>();

            if (alertNotification.getActions() != null && !alertNotification.getActions().isEmpty()) {
                data.put("actions", alertNotification.getActions().toJson());
//                alertNotification.getActions().stream()
//                    .map(value -> Values.getObject(value).orElse(null))
//                    .filter(objectValue -> !Objects.isNull(objectValue))
//                    .forEach(actionObj -> {
//                        String type = actionObj.getString("type").orElse(null);
//                        String title = actionObj.getString("title").orElse(null);
//                        String appUrl = actionObj.getString("appUrl").orElse(null);
//                        String assetId = actionObj.getString("assetId").orElse(null);
//                        String rawJson = actionObj.getString("rawJson").orElse(null);
//                        String attributeName = actionObj.getString("attributeName").orElse(null);
//                    });
            }

            // Push alert directly inside the FCM message
            fcmDeliveryService.sendMessage(
                FCMTargetType.DEVICE,
                token,
                notification,
                data,
                FCMMessagePriority.HIGH,
                0 // 0 TTL gives best performance and this is assuming notifications are time critical
                );
        } else {
            LOG.warning("Unsupported push provider version: " + pushProviderVersion);
        }
    }

    public void storeAndNotify(String userId, AlertNotification notification) {
        notification.setUserId(userId);
        notification.setDeliveryStatus(DeliveryStatus.PENDING);
        persistenceService.doTransaction(entityManager -> entityManager.merge(notification));

        // Try to queue through FCM in a separate transaction
        Consumer<EntityManager> fcmMessageGenerator = fcmDeliveryService.deliverPendingToFCM(userId, findAllTokenForUser(userId));
        if (fcmMessageGenerator != null) {
            persistenceService.doTransaction(fcmMessageGenerator);
        }
    }

    public List<AlertNotification> getNotificationsOfUser(String userId, DeliveryStatus deliveryStatus) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "SELECT an FROM AlertNotification an WHERE an.userId =:userId and an.deliveryStatus = :deliveryStatus order by an.createdOn desc",
            AlertNotification.class
        ).setParameter("userId", userId).setParameter("deliveryStatus", deliveryStatus).getResultList());
    }

    public List<AlertNotification> getNotificationsOfUser(String userId) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "SELECT an FROM AlertNotification an WHERE an.userId = :userId order by an.createdOn desc",
            AlertNotification.class
        ).setParameter("userId", userId).getResultList());
    }

    public boolean isQueuedNotificationForUser(Long id, String userId) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "SELECT count(an) FROM AlertNotification an WHERE an.id = :alertId and an.userId =:userId and an.deliveryStatus = :deliveryStatus"
            , Long.class
        ).setParameter("alertId", id).setParameter("userId", userId).setParameter("deliveryStatus", DeliveryStatus.QUEUED).getSingleResult() > 0);
    }

    public void setAcknowledged(Long id) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE AlertNotification SET deliveryStatus=:status WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("status", DeliveryStatus.ACKNOWLEDGED);
            query.executeUpdate();
            // TODO Who is cleaning up the acknowledged notifications?
        });
    }

    public void removeNotificationsOfUser(String userId) {
        persistenceService.doTransaction(entityManager -> entityManager
            .createQuery("delete AlertNotification an where an.userId = :userId")
            .setParameter("userId", userId)
            .executeUpdate()
        );
    }

    public void removeNotification(String userId, Long id) {
        persistenceService.doTransaction(entityManager -> entityManager
            .createQuery("delete AlertNotification an where an.userId = :userId and an.id = :id")
            .setParameter("userId", userId).setParameter("id", id)
            .executeUpdate()
        );
    }

    public List<String> findAllUsersWithToken() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery("SELECT DISTINCT dnt.id.userId FROM DeviceNotificationToken dnt", String.class).getResultList()
        );
    }

    @SuppressWarnings("unchecked")
    public List<String> findAllUsersWithToken(UserQuery userQuery) {
        return persistenceService.doReturningTransaction(entityManager -> {
            Query query = entityManager.createQuery("SELECT DISTINCT dnt.id.userId " + buildFromString(userQuery) + buildWhereString(userQuery));

            // TODO: improve way this is set, should be part of buildWhereString + some other operation, see AssetStorageService
            if (userQuery.tenantPredicate != null) {
                query.setParameter("realmId", userQuery.tenantPredicate.realmId);
            }
            if (userQuery.assetPredicate != null) {
                query.setParameter("assetId", userQuery.assetPredicate.id);
            }

            return query.getResultList();
        });
    }

    protected String buildFromString(UserQuery query) {
        StringBuilder sb = new StringBuilder();

        sb.append("FROM DeviceNotificationToken dnt ");

        if (query.tenantPredicate != null) {
            sb.append("join User u on u.id = dnt.id.userId");
        }
        if (query.assetPredicate != null) {
            sb.append("join UserAsset ua on ua.id.userId = dnt.id.userId ");
        }

        return sb.toString();
    }

    protected String buildWhereString(UserQuery query) {
        StringBuilder sb = new StringBuilder();

        sb.append(" WHERE 1 = 1 ");

        if (query.tenantPredicate != null) {
            sb.append("AND u.realmId = :realmId");
        }
        if (query.assetPredicate != null) {
            sb.append("AND ua.id.assetId = :assetId ");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }

}