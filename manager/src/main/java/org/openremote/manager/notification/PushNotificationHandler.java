/*
 * Copyright 2018, OpenRemote Inc.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.console.ConsoleConfiguration;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationSendResult;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.filter.ObjectValueKeyPredicate;
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.util.TextUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.persistence.PersistenceEvent.*;
import static org.openremote.model.asset.AssetType.CONSOLE;
import static org.openremote.model.notification.PushNotificationMessage.TargetType.DEVICE;
import static org.openremote.model.notification.PushNotificationMessage.TargetType.TOPIC;
import static org.openremote.model.query.BaseAssetQuery.Include.ONLY_ID_AND_NAME;
import static org.openremote.model.query.BaseAssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES;

public class PushNotificationHandler extends RouteBuilder implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(PushNotificationHandler.class.getName());
    public static final String FIREBASE_CONFIG_FILE = "FIREBASE_CONFIG_FILE";
    public static final int CONNECT_TIMEOUT_MILLIS = 3000;
    public static final int READ_TIMEOUT_MILLIS = 3000;
    public static final String FCM_PROVIDER_NAME = "fcm";

    protected AssetStorageService assetStorageService;
    protected boolean valid;
    protected Map<String, String> consoleFCMTokenMap = new HashMap<>();
    protected List<String> fcmTokenBlacklist = new ArrayList<>();

    public void init(Container container) throws Exception {
        this.assetStorageService = container.getService(AssetStorageService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);

        String firebaseConfigFilePath = container.getConfig().get(FIREBASE_CONFIG_FILE);

        if (TextUtil.isNullOrEmpty(firebaseConfigFilePath)) {
            LOG.warning(FIREBASE_CONFIG_FILE + " not defined, can not send FCM notifications");
            return;
        }

        if (!Files.isReadable(Paths.get(firebaseConfigFilePath))) {
            LOG.warning(FIREBASE_CONFIG_FILE + " invalid path or file not readable");
            return;
        }

        try (InputStream is = Files.newInputStream(Paths.get(firebaseConfigFilePath))) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(is))
                .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
                .setReadTimeout(READ_TIMEOUT_MILLIS)
                .build();

            FirebaseApp.initializeApp(options);
            valid = true;
        } catch (Exception ex) {
            LOG.severe("Exception occurred whilst initialising FCM");
        }
    }

    @Override
    public void start(Container container) throws Exception {

        if (!isValid()) {
            LOG.warning("FCM configuration invalid so cannot start");
            return;
        }

        // Not using Collectors.toMap as there is a quirk in there which means null values aren't supported!
        consoleFCMTokenMap = new HashMap<>();

        // Find all console assets that use this adapter
        assetStorageService.findAll(
            new AssetQuery()
                .select(new BaseAssetQuery.Select(BaseAssetQuery.Include.ALL_EXCEPT_PATH,
                    false,
                    AttributeType.CONSOLE_PROVIDERS.getName()))
                .type(CONSOLE)
                .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                    new ObjectValueKeyPredicate("push")))
            .stream()
            .filter(PushNotificationHandler::isLinkedToFcmProvider)
            .forEach(asset -> consoleFCMTokenMap.put(asset.getId(), getFcmToken(asset).orElse(null)));
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {
        // If any console asset was modified in the database, detect push provider changes
        from(PERSISTENCE_TOPIC)
            .routeId("PushNotificationAssetChanges")
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
    public String getTypeName() {
        return PushNotificationMessage.TYPE;
    }

    @Override
    public boolean isMessageValid(AbstractNotificationMessage message) {

        if (!(message instanceof PushNotificationMessage)) {
            LOG.warning("Invalid message: '" + message.getClass().getSimpleName() + "' is not an instance of PushNotificationMessage");
            return false;
        }

        PushNotificationMessage pushMessage = (PushNotificationMessage) message;
        if (TextUtil.isNullOrEmpty(pushMessage.getTitle()) && pushMessage.getData() == null) {
            LOG.warning("Invalid message: must either contain a title and/or data");
            return false;
        }

        return true;
    }

    @Override
    public Notification.Targets mapTarget(Notification.Source source, String sourceId, Notification.TargetType targetType, String targetId, AbstractNotificationMessage message) {

        // Check if message is going to a topic if so then filter consoles subscribed to that topic
        PushNotificationMessage pushMessage = (PushNotificationMessage) message;
        BaseAssetQuery.Select select;
        boolean forTopic = pushMessage.getTargetType() == TOPIC;
        List<Asset> mappedConsoles;

        if (forTopic) {
            select = new BaseAssetQuery.Select(ONLY_ID_AND_NAME_AND_ATTRIBUTES, false, AttributeType.CONSOLE_PROVIDERS.getName());
        } else {
            select = new BaseAssetQuery.Select(ONLY_ID_AND_NAME);
        }

        switch (targetType) {

            case TENANT:
                // Get all console assets with a push provider defined within the specified tenant
                mappedConsoles = assetStorageService.findAll(
                    new AssetQuery()
                        .select(select)
                        .tenant(new TenantPredicate(targetId))
                        .type(AssetType.CONSOLE)
                        .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                            new ObjectValueKeyPredicate(PushNotificationMessage.TYPE))
                );

                if (forTopic) {
                    mappedConsoles.removeIf(c -> !isConsoleSubscribedToTopic(c, pushMessage.getTarget()));
                }

                if (mappedConsoles.isEmpty()) {
                    LOG.fine("No console assets linked to target realm");
                    return null;
                }

                return new Notification.Targets(Notification.TargetType.ASSET,
                    mappedConsoles.stream().map(Asset::getId).collect(Collectors.toList()));

            case USER:

                // Get all console assets linked to the specified user
                List<String> ids = assetStorageService.findUserAssets(null, targetId, null)
                    .stream()
                    .map(userAsset -> userAsset.getId().getAssetId()).collect(Collectors.toList());

                if (!ids.isEmpty()) {

                    mappedConsoles = assetStorageService.findAll(
                        new AssetQuery()
                            .select(select)
                            .ids(ids)
                            .type(AssetType.CONSOLE)
                            .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                                new ObjectValueKeyPredicate(PushNotificationMessage.TYPE))
                    );

                    if (forTopic) {
                        mappedConsoles.removeIf(c ->
                            !isConsoleSubscribedToTopic(c, pushMessage.getTarget())
                        );
                    }
                } else {
                    LOG.fine("No console assets linked to target user");
                    return null;
                }

                return new Notification.Targets(Notification.TargetType.ASSET,
                    mappedConsoles.stream().map(Asset::getId).collect(Collectors.toList()));

            case ASSET:

                // Find all console descendants of the specified asset
                mappedConsoles = assetStorageService.findAll(
                    new AssetQuery()
                        .select(select)
                        .path(new PathPredicate(targetId))
                        .type(AssetType.CONSOLE)
                        .attributeValue(AttributeType.CONSOLE_PROVIDERS.getName(),
                            new ObjectValueKeyPredicate(PushNotificationMessage.TYPE))
                );

                if (forTopic) {
                    mappedConsoles.removeIf(c -> !isConsoleSubscribedToTopic(c, pushMessage.getTarget()));
                }

                if (mappedConsoles.isEmpty()) {
                    LOG.fine("No console assets descendants of target asset");
                    return null;
                }

                return new Notification.Targets(Notification.TargetType.ASSET,
                    mappedConsoles.stream().map(Asset::getId).collect(Collectors.toList()));
        }

        return null;
    }

    @Override
    public NotificationSendResult sendMessage(long id, Notification.Source source, String sourceId, Notification.TargetType targetType, String targetId, AbstractNotificationMessage message) {

        if (targetType != Notification.TargetType.ASSET) {
            LOG.warning("Target type not supported: " + targetType);
            return NotificationSendResult.failure("Target type not supported: " + targetType);
        }

        if (!isValid()) {
            LOG.warning("FCM invalid configuration so ignoring");
            return NotificationSendResult.failure("FCM invalid configuration so ignoring");
        }

        // Check this asset has an FCM token (i.e. it is registered for push notifications)
        String fcmToken = consoleFCMTokenMap.get(targetId);

        if (TextUtil.isNullOrEmpty(fcmToken)) {
            LOG.warning("No FCM token found for console: " + targetId);
            return NotificationSendResult.failure("No FCM token found for console: " + targetId);
        }

        PushNotificationMessage pushMessage = (PushNotificationMessage) message;

        // Assume DEVICE target if not specified
        if (pushMessage.getTargetType() == null) {
            pushMessage.setTargetType(DEVICE);
        }

        switch (pushMessage.getTargetType()) {
            case DEVICE:
                // Always use fcm token from the console asset (so users cannot target other devices)
                pushMessage.setTarget(fcmToken);
                break;
            case TOPIC:
                // TODO: Decide how to handle FCM topic support (too much power for users to put anything in target)
                LOG.warning("Messages sent to a topic are not supported");
                return NotificationSendResult.failure("Messages sent to a topic are not supported");
            case CONDITION:
                // TODO: Decide how to handle conditions support (too much power for users to put anything in target)
                LOG.warning("Messages sent to conditional targets are not supported");
                return NotificationSendResult.failure("Messages sent to conditional targets are not supported");
        }

        return sendMessage(buildFCMMessage(id, pushMessage));
    }

//    public NotificationSendResult sendMessage(PushNotificationMessage.TargetType targetType, String fcmTarget, Message.Builder messageBuilder) {
//
//        switch (targetType) {
//            case DEVICE:
//                messageBuilder.setToken(fcmTarget);
//                break;
//            case TOPIC:
//                messageBuilder.setTopic(fcmTarget);
//                break;
//            case CONDITION:
//                messageBuilder.setCondition(fcmTarget);
//                break;
//        }
//
//        NotificationSendResult result = sendMessage(messageBuilder.build());
//        if (result.isSuccess()) {
//            LOG.warning("FCM send to '" + targetType + ":" + fcmTarget + "' success");
//        } else {
//            LOG.warning("FCM send to '" + targetType + ":" + fcmTarget + "' failed: " + result.getMessage());
//        }
//
//        return result;
//    }

    @Override
    public boolean isValid() {
        return valid;
    }

    public NotificationSendResult sendMessage(Message message) {
        try {
            FirebaseMessaging.getInstance().send(message);
            return NotificationSendResult.success();
        } catch (FirebaseMessagingException e) {
            handleFcmException(e);
            return NotificationSendResult.failure("FCM send failed: " + e.getErrorCode());
        }
    }

    protected boolean isConsoleSubscribedToTopic(Asset asset, String topic) {
        return ConsoleConfiguration.getConsoleProvider(asset, PushNotificationMessage.TYPE)
            .map(ConsoleProvider::getData)
            .flatMap(objectValue -> objectValue.getArray("topics"))
            .map(arrayValue -> arrayValue.contains(topic))
            .orElse(false);
    }

    protected static Message buildFCMMessage(long id, PushNotificationMessage pushMessage) {

        Message.Builder builder = Message.builder();
        boolean dataOnly = TextUtil.isNullOrEmpty(pushMessage.getTitle());

        switch (pushMessage.getTargetType()) {
            case DEVICE:
                builder.setToken(pushMessage.getTarget());
                break;
            case TOPIC:
                builder.setTopic(pushMessage.getTarget());
                break;
            case CONDITION:
                builder.setCondition(pushMessage.getTarget());
                break;
        }

        AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder();
        ApnsConfig.Builder apnsConfigBuilder = ApnsConfig.builder();
        Aps.Builder apsBuilder = Aps.builder();
        WebpushConfig.Builder webpushConfigBuilder = WebpushConfig.builder();

        if (!dataOnly) {
            // Don't set basic notification on Android if there is data so even if app is in background we can show a custom notification
            // with actions that use the actions from the data
            if (pushMessage.getData() != null || pushMessage.getAction() != null || pushMessage.getButtons() != null) {
                androidConfigBuilder.putData("or-title", pushMessage.getTitle());
                if (pushMessage.getBody() != null) {
                    androidConfigBuilder.putData("or-body", pushMessage.getBody());
                }
            }

            // Use alert dictionary for apns
            // https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/PayloadKeyReference.html
            apsBuilder.setAlert(ApsAlert.builder().setTitle(pushMessage.getTitle()).setBody(pushMessage.getBody()).build())
                .setSound("default");

            webpushConfigBuilder.setNotification(new WebpushNotification(pushMessage.getTitle(), pushMessage.getBody()));
        }

        if (pushMessage.getData() != null) {
            pushMessage.getData().stream().forEach(stringValuePair -> {
                if (stringValuePair.value != null) {
                    builder.putData(stringValuePair.key, stringValuePair.value.toString());
                }
            });

            if (dataOnly) {
                apsBuilder.setContentAvailable(true);
            }
        }

        // Store ID so console can mark notification as delivered and/or acknowledged
        builder.putData("notification-id", Long.toString(id));

        try {
            if (pushMessage.getAction() != null) {
                builder.putData("action", Container.JSON.writeValueAsString(pushMessage.getAction()));
            }

            if (pushMessage.getButtons() != null) {
                builder.putData("buttons", Container.JSON.writeValueAsString(pushMessage.getButtons()));
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        androidConfigBuilder.setPriority(pushMessage.getPriority() == PushNotificationMessage.MessagePriority.HIGH ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL);
        apnsConfigBuilder.putHeader("apns-priority", !dataOnly ? "10" : "5"); // Don't use 10 for data only

        // set the following APNS flag to allow console to customise the notification before delivery and to ensure delivery
        apsBuilder.setMutableContent(true);

        if (pushMessage.getTtlSeconds() != null) {
            long timeToLiveSeconds = Math.max(pushMessage.getTtlSeconds(), 0);
            long timeToLiveMillis = timeToLiveSeconds * 1000;
            Date expirationDate = new Date(new Date().getTime() + timeToLiveMillis);
            long epochSeconds = Math.round(((float) expirationDate.getTime()) / 1000);

            apnsConfigBuilder.putHeader("apns-expiration", Long.toString(epochSeconds));
            androidConfigBuilder.setTtl(timeToLiveMillis);
            webpushConfigBuilder.putHeader("TTL", Long.toString(timeToLiveSeconds));
        }

        apnsConfigBuilder.setAps(apsBuilder.build());
        builder.setAndroidConfig(androidConfigBuilder.build());
        builder.setApnsConfig(apnsConfigBuilder.build());
        builder.setWebpushConfig(webpushConfigBuilder.build());

        return builder.build();
    }

    protected static boolean isLinkedToFcmProvider(Asset asset) {
        return ConsoleConfiguration.getConsoleProvider(asset, "push")
            .map(ConsoleProvider::getVersion).map(FCM_PROVIDER_NAME::equals).orElse(false);
    }

    protected static Optional<String> getFcmToken(Asset asset) {
        return ConsoleConfiguration.getConsoleProvider(asset, "push")
            .map(ConsoleProvider::getData)
            .flatMap(objValue -> objValue.getString("token"));
    }

    protected void processConsoleAssetChange(Asset asset, PersistenceEvent persistenceEvent) {

        withLock(getClass().getSimpleName() + "::processAssetChange", () -> {

            String fcmToken = consoleFCMTokenMap.remove(asset.getId());
            if (!TextUtil.isNullOrEmpty(fcmToken)) {
                fcmTokenBlacklist.remove(fcmToken);
            }

            switch (persistenceEvent.getCause()) {

                case INSERT:
                case UPDATE:

                    consoleFCMTokenMap.put(asset.getId(), getFcmToken(asset).orElse(null));
                    break;
            }
        });
    }

    protected void handleFcmException(FirebaseMessagingException e) {

        LOG.log(Level.WARNING, "FCM send failed: " + e.getErrorCode(), e);

        // TODO: Implement backoff and blacklisting
        switch (e.getErrorCode()) {

            case "invalid-argument":
            case "authentication-error":
                LOG.severe("FCM critical error so marking FCM as invalid no more messages will be sent");
                break;
            case "server-unavailable":
            case "internal-error":

                break;
        }
    }
}
