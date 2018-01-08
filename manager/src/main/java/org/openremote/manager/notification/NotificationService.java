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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.model.notification.DeviceNotificationToken;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.notification.DeliveryStatus;
import org.openremote.model.user.UserQuery;

import javax.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class NotificationService implements ContainerService {

    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected FCMDeliveryService fcmDeliveryService;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);

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

    public void storeAndNotify(String userId, AlertNotification notification) {
        notification.setUserId(userId);
        notification.setDeliveryStatus(DeliveryStatus.PENDING);
        persistenceService.doTransaction(entityManager -> entityManager.merge(notification));

        // Try to queue through FCM in a separate transaction
        persistenceService.doTransaction(fcmDeliveryService.deliverPendingToFCM(userId, findAllTokenForUser(userId)));
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