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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.openremote.container.Container;
import org.openremote.model.notification.DeviceNotificationToken;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.notification.DeliveryStatus;
import org.openremote.model.util.TextUtil;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

public class FCMDeliveryService {

    private static final Logger LOG = Logger.getLogger(FCMDeliveryService.class.getName());
    public static final String FIREBASE_CONFIG_FILE = "FIREBASE_CONFIG_FILE";

    protected boolean valid;

    public FCMDeliveryService(Container container) {
        this(
            container.getConfig().get(FIREBASE_CONFIG_FILE)
        );
    }

    public FCMDeliveryService(String firebaseConfigFilePath) {

        if (TextUtil.isNullOrEmpty(firebaseConfigFilePath)) {
            LOG.info(FIREBASE_CONFIG_FILE + " not defined, can not send FCM notifications");
            return;
        }

        if (!Files.isReadable(Paths.get(firebaseConfigFilePath))) {
            LOG.info(FIREBASE_CONFIG_FILE + " invalid path or file not readable");
            return;
        }

        try (InputStream is = Files.newInputStream(Paths.get(firebaseConfigFilePath))) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(is))
                .build();

            FirebaseApp.initializeApp(options);
            valid = true;
        } catch (Exception ex) {
            LOG.severe("Exception occurred whilst initialising FCM");
        }
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
                LOG.fine("Sending FCM notification pickup message to: " + deviceToken);
                // Changed to behave like javadoc says
                success = sendMessage(FCMTargetType.DEVICE, deviceToken.getToken(), FCMMessagePriority.HIGH, null, null, null, null, ApnsConfig.builder().putHeader("mutable-content", "1")) || success;
        }
        if (success) {
            LOG.fine("Successfully delivered FCM pickup message for at least one device of: " + userId);
        } else {
            LOG.warning("Unsuccessful sending FCM pickup message for any device of: " + userId);
        }
        return success;
    }


    public boolean sendMessage(FCMTargetType targetType, String target, FCMMessagePriority priority, Notification notification, Map<String, String> data, Date expiration, AndroidConfig.Builder androidConfigBuilder, ApnsConfig.Builder apnsConfigBuilder) {
        if (!isValid()) {
            LOG.warning("FCM invalid configuration so ignoring");
            return false;
        }

        boolean success = false;

        if (TextUtil.isNullOrEmpty(target)) {
            LOG.info("Invalid FCM message, the target is required and either notification and/or data must be set");
            return false;
        }

        try {
            Message.Builder builder = Message.builder();

            switch (targetType) {
                case DEVICE:
                    builder.setToken(target);
                    break;
                case TOPIC:
                    builder.setTopic(target);
                    break;
                case CONDITION:
                    builder.setCondition(target);
                    break;
            }

            // TODO: Add web push support
            if (priority == FCMMessagePriority.HIGH || notification == null || expiration != null) {
                androidConfigBuilder = androidConfigBuilder == null ? AndroidConfig.builder() : androidConfigBuilder;
                apnsConfigBuilder = apnsConfigBuilder == null ? ApnsConfig.builder() : apnsConfigBuilder;

                androidConfigBuilder.setPriority(priority == FCMMessagePriority.HIGH ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL);
                apnsConfigBuilder.putHeader("apns-priority", notification != null ? "10" : "5"); // Don't use 5 for data only

                if (notification == null) {
                    apnsConfigBuilder.putCustomData("content-available", "1");
                }

                if (expiration != null) {
                    long expirationMillis = expiration.getTime();
                    long nowMillis = new Date().getTime();

                    if (expirationMillis > nowMillis) {
                        int seconds = Math.round(((float)expirationMillis)/1000);
                        apnsConfigBuilder.putHeader("apns-expiration", Integer.toString(seconds));
                        androidConfigBuilder.setTtl(expirationMillis);
                    }
                }

                builder.setAndroidConfig(androidConfigBuilder.build());
                builder.setApnsConfig(apnsConfigBuilder.build());
            }

            if (notification != null) {
                builder.setNotification(notification);
            }
            if (data != null) {
                builder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(builder.build());
            success = true;
        } catch (FirebaseMessagingException e) {
            LOG.log(Level.WARNING, "FCM send failed: " + e.getErrorCode(), e);

            // TODO: Implement backoff and blacklisting
            switch (e.getErrorCode()) {
                // Errors we cannot recover from - stop FCM to avoid blacklisting our app
                case "messaging/invalid-argument":
                case "messaging/authentication-error":
                    LOG.severe("FCM critical error so marking FCM as invalid no more messages will be sent");
                    setValid(false);
                    break;
                case "messaging/server-unavailable":
                case "messaging/internal-error":
                    break;
            }
        }

        return success;
    }

    public synchronized boolean isValid() {
        return valid;
    }

    protected synchronized void setValid(boolean valid) {
        this.valid = valid;
    }
}
