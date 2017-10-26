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
package org.openremote.manager.server.notification;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.notification.DeviceNotificationToken;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.notification.DeliveryStatus;
import org.openremote.model.user.UserQuery;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

public class NotificationService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    public static final String NOTIFICATION_FIREBASE_API_KEY = "NOTIFICATION_FIREBASE_API_KEY";
    public static final String NOTIFICATION_FIREBASE_URL = "NOTIFICATION_FIREBASE_URL";
    public static final String NOTIFICATION_FIREBASE_URL_DEFAULT = "https://fcm.googleapis.com/fcm/send";

    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected ResteasyWebTarget firebaseTarget;
    private String fcmKey;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);

        fcmKey = container.getConfig().get(NOTIFICATION_FIREBASE_API_KEY);
        if (fcmKey == null) {
            LOG.info(NOTIFICATION_FIREBASE_API_KEY + " not defined, can not send notifications to user devices");
        }

        container.getService(WebService.class).getApiSingletons().add(
            new NotificationResourceImpl(this)
        );

        ResteasyClient client = new ResteasyClientBuilder().build();
        firebaseTarget = client.target(container.getConfig().getOrDefault(NOTIFICATION_FIREBASE_URL, NOTIFICATION_FIREBASE_URL_DEFAULT));
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
        return persistenceService.doReturningTransaction(entityManager -> {
            Query query = entityManager.createQuery(
                "SELECT dnt FROM DeviceNotificationToken dnt WHERE dnt.id.userId =:userId order by dnt.updatedOn desc"
            );
            query.setParameter("userId", userId);
            return query.getResultList();
        });
    }


    public void storeAndNotify(String userId, AlertNotification alertNotification) {
        alertNotification.setUserId(userId);
        alertNotification.setDeliveryStatus(DeliveryStatus.PENDING);
        persistenceService.doTransaction((EntityManager entityManager) -> {
            entityManager.merge(alertNotification);
            if (fcmKey == null) {
                LOG.info("No " + NOTIFICATION_FIREBASE_API_KEY + " configured, not sending to '" + userId + "': " + alertNotification);
                return;
            }
            List<DeviceNotificationToken> allTokenForUser = findAllTokenForUser(userId);
            if (allTokenForUser.size() == 0) {
                LOG.fine("User has no registered devices/notification tokens: " + userId);
            }
            for (DeviceNotificationToken notificationToken : allTokenForUser) {
                try {
                    Invocation.Builder builder = firebaseTarget.request().header("Authorization", "key=" + fcmKey);
                    Notification notification = new Notification("_");

                    FCMBaseMessage message;
                    if ("ANDROID".equals(notificationToken.getDeviceType())) {
                        message = new FCMBaseMessage(notificationToken.getToken());
                    } else {
                        message = new FCMMessage(notification, true, true, "high", notificationToken.getToken());
                    }
                    LOG.fine("Sending notification message to device of user '" + userId + "': " + message);
                    Response response = builder.post(Entity.entity(message, "application/json"));
                    if (response.getStatus() != 200) {
                        LOG.severe("Error send FCM notification status=[" + response.getStatus() + "], statusInformation=[" + response.getStatusInfo() + "]");
                    }
                    response.close();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error sending notification to FCM: " + getRootCause(e), e);
                }
            }
        });
    }

    public List<AlertNotification> getPendingAlertForUserId(String userId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            Query query = entityManager.createQuery("SELECT an FROM AlertNotification an WHERE an.userId =:userId and an.deliveryStatus =:deliveryStatus");
            query.setParameter("userId", userId);
            query.setParameter("deliveryStatus", DeliveryStatus.PENDING);
            return query.getResultList();
        });
    }

    public boolean isPendingAlertForUserId(Long alertId, String userId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT count(an) FROM AlertNotification an WHERE an.id = :alertId and an.userId =:userId and an.deliveryStatus = :deliveryStatus"
                , Long.class);
            query.setParameter("alertId", alertId);
            query.setParameter("userId", userId);
            query.setParameter("deliveryStatus", DeliveryStatus.PENDING);
            return query.getSingleResult() > 0;
        });
    }

    public void removeAlertNotification(Long id) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE AlertNotification SET deliveryStatus=:status  WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("status", DeliveryStatus.DELIVERED);
            query.executeUpdate();
            // TODO Who is cleaning up the delivered notifications?
        });
    }

    public List<String> findAllUsersWithToken() {
        return persistenceService.doReturningTransaction(entityManager -> {
            Query query = entityManager.createQuery("SELECT DISTINCT dnt.id.userId FROM DeviceNotificationToken dnt");
            return query.getResultList();
        });
    }

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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
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

}