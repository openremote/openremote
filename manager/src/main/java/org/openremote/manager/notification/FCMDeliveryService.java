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

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.Container;
import org.openremote.model.notification.DeviceNotificationToken;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.notification.DeliveryStatus;
import org.openremote.model.util.TextUtil;

import javax.persistence.EntityManager;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

public class FCMDeliveryService {

    private static final Logger LOG = Logger.getLogger(FCMDeliveryService.class.getName());

    public static final String NOTIFICATION_FIREBASE_API_KEY = "NOTIFICATION_FIREBASE_API_KEY";
    public static final String NOTIFICATION_FIREBASE_URL = "NOTIFICATION_FIREBASE_URL";
    public static final String NOTIFICATION_FIREBASE_URL_DEFAULT = "https://fcm.googleapis.com/fcm/send";

    final protected String fcmKey;
    final protected ResteasyWebTarget firebaseTarget;
    final protected boolean valid;

    public FCMDeliveryService(Container container) {
        this(
            container.getConfig().get(NOTIFICATION_FIREBASE_API_KEY),
            container.getConfig().getOrDefault(NOTIFICATION_FIREBASE_URL, NOTIFICATION_FIREBASE_URL_DEFAULT)
        );
    }

    public FCMDeliveryService(String fcmKey, String firebaseUrl) {
        boolean ok = true;

        if (TextUtil.isNullOrEmpty(fcmKey)) {
            LOG.info(NOTIFICATION_FIREBASE_API_KEY + " not defined, can not send notifications");
            ok = false;
        }
        if (TextUtil.isNullOrEmpty(firebaseUrl)) {
            LOG.info(NOTIFICATION_FIREBASE_URL + " not defined, can not send notifications");
            ok = false;
        }
        if (!ok) {
            this.valid = false;
            this.fcmKey = null;
            this.firebaseTarget = null;
            return;
        }

        this.valid = true;
        this.fcmKey = fcmKey;

        ResteasyClient client = new ResteasyClientBuilder()
            .socketTimeout(30, TimeUnit.SECONDS)
            .establishConnectionTimeout(10, TimeUnit.SECONDS)
            .build();
        firebaseTarget = client.target(firebaseUrl);
    }

    public Consumer<EntityManager> deliverPendingToFCM(String userId, List<DeviceNotificationToken> deviceTokens) {
        if (!isValid()) {
            LOG.warning("FCM invalid configuration so ignoring");
            return null;
        }

        return entityManager -> {
            List<AlertNotification> pendingNotifications = entityManager.createQuery(
                "SELECT an FROM AlertNotification an WHERE an.userId = :userId and an.deliveryStatus = :deliveryStatus",
                AlertNotification.class
            ).setParameter("userId", userId).setParameter("deliveryStatus", DeliveryStatus.PENDING).getResultList();

            // If there are any pending notifications for the user
            if (pendingNotifications.size() > 0) {
                // If at least one of the user's devices received the pickup signal, it's queued
                if (sendPickupSignalThroughFCM(userId, deviceTokens)) {
                    for (AlertNotification notification : pendingNotifications) {
                        notification.setDeliveryStatus(DeliveryStatus.QUEUED);
                    }
                }
            }
        };
    }

    /**
     * @return <code>true</code> if a signal was delivered to at least one device of the user.
     */
    protected boolean sendPickupSignalThroughFCM(String userId, List<DeviceNotificationToken> deviceTokens) {
        if (deviceTokens.size() == 0) {
            LOG.fine("No registered devices, can't deliver pending notifications of: " + userId);
            return false;
        }

        boolean success = false;
        for (DeviceNotificationToken deviceToken : deviceTokens) {
                FCMBaseMessage message;
                if ("ANDROID".equals(deviceToken.getDeviceType())) {
                    message = new FCMBaseMessage(deviceToken.getToken());
                } else {
                    message = new FCMMessage(new FCMNotification("_"), true, true, "high", deviceToken.getToken());
                }

                LOG.fine("Sending FCM notification pickup message to: " + deviceToken);
                // Changed to behave like javadoc says
                success = sendFCMMessage(message) || success;
        }
        if (success) {
            LOG.fine("Successfully delivered FCM pickup message for at least one device of: " + userId);
        } else {
            LOG.warning("Unsuccessful sending FCM pickup message for any device of: " + userId);
        }
        return success;
    }

    public boolean sendFCMMessage(FCMBaseMessage message) {
        if (!isValid()) {
            LOG.warning("FCM invalid configuration so ignoring");
            return false;
        }

        boolean success = false;
        Response response = null;

        try {
            Invocation.Builder builder = firebaseTarget.request().header("Authorization", "key=" + fcmKey);
            response = builder.post(Entity.entity(message, "application/json"));

            if (response.getStatus() != 200) {
                LOG.warning("Send error: status=[" + response.getStatus() + "], statusInformation=[" + response.getStatusInfo() + "]");
            } else {
                LOG.fine("Send success: status=[" + response.getStatus() + "], statusInformation=[" + response.getStatusInfo() + "]");
                success = true;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error sending FCM notification pickup message: " + getRootCause(e), e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return success;
    }

    public boolean isValid() {
        return valid;
    }
}
