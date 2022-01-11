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
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.impl.ConsoleAsset;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationSendResult;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.*;
import org.openremote.model.security.User;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.persistence.PersistenceEvent.*;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.manager.security.ManagerKeycloakIdentityProvider.KEYCLOAK_USER_ATTRIBUTE_PUSH_NOTIFICATIONS_DISABLED;
import static org.openremote.model.notification.PushNotificationMessage.TargetType.*;

@SuppressWarnings("deprecation")
public class PushNotificationHandler extends RouteBuilder implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(PushNotificationHandler.class.getName());
    public static final String FIREBASE_CONFIG_FILE = "FIREBASE_CONFIG_FILE";
    public static final int CONNECT_TIMEOUT_MILLIS = 3000;
    public static final int READ_TIMEOUT_MILLIS = 3000;
    public static final String FCM_PROVIDER_NAME = "fcm";

    protected ManagerIdentityService managerIdentityService;
    protected AssetStorageService assetStorageService;
    protected GatewayService gatewayService;
    protected boolean valid;
    protected Map<String, String> consoleFCMTokenMap = new HashMap<>();
    protected List<String> fcmTokenBlacklist = new ArrayList<>();

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    public void init(Container container) throws Exception {
        this.managerIdentityService = container.getService(ManagerIdentityService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.gatewayService = container.getService(GatewayService.class);
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        String firebaseConfigFilePath = container.getConfig().get(FIREBASE_CONFIG_FILE);

        if (TextUtil.isNullOrEmpty(firebaseConfigFilePath)) {
            LOG.warning(FIREBASE_CONFIG_FILE + " not defined, can not send FCM notifications");
            return;
        }

        if (!Files.isReadable(Paths.get(firebaseConfigFilePath))) {
            LOG.warning(FIREBASE_CONFIG_FILE + " invalid path or file not readable: " + firebaseConfigFilePath);
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
                .select(new AssetQuery.Select().excludePath(true)
                    .attributes(ConsoleAsset.CONSOLE_PROVIDERS.getName()))
                .types(ConsoleAsset.class)
                .attributes(
                    new AttributePredicate(ConsoleAsset.CONSOLE_PROVIDERS, null, false, new NameValuePredicate.Path(PushNotificationMessage.TYPE))
                ))
            .stream()
            .map(asset -> (ConsoleAsset) asset)
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
            .filter(isPersistenceEventForEntityType(ConsoleAsset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                PersistenceEvent<ConsoleAsset> persistenceEvent = (PersistenceEvent<ConsoleAsset>)exchange.getIn().getBody(PersistenceEvent.class);
                final ConsoleAsset asset = persistenceEvent.getEntity();
                processConsoleAssetChange(asset, persistenceEvent);
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
    public List<Notification.Target> getTargets(Notification.Source source, String sourceId, List<Notification.Target> targets, AbstractNotificationMessage message) {

        // Check if message is going to a topic if so then filter consoles subscribed to that topic
        PushNotificationMessage pushMessage = (PushNotificationMessage) message;
        List<Notification.Target> mappedTargets = new ArrayList<>();

        if (pushMessage.getTargetType() == TOPIC || pushMessage.getTargetType() == CONDITION) {
            mappedTargets.add(new Notification.Target(Notification.TargetType.CUSTOM, pushMessage.getTargetType() + ": " + pushMessage.getTarget()));
            return mappedTargets;
        }

        if (targets != null) {

            targets.forEach(target -> {

                Notification.TargetType targetType = target.getType();
                String targetId = target.getId();

                switch (targetType) {

                    case TENANT:
                        // Get all console assets with a push provider defined within the specified tenant
                        List<Asset<?>> consoleAssets = assetStorageService.findAll(
                                new AssetQuery()
                                        .select(AssetQuery.Select.selectExcludeAll())
                                        .tenant(new TenantPredicate(targetId))
                                        .types(ConsoleAsset.class)
                                        .attributes(new AttributePredicate(ConsoleAsset.CONSOLE_PROVIDERS, null, false, new NameValuePredicate.Path(PushNotificationMessage.TYPE))));

                        // Get all user ids which have pushNotificationsDisabled set to false
                        String[] userIds = Arrays.stream(managerIdentityService
                                        .getIdentityProvider()
                                        .queryUsers(new UserQuery().tenant(new TenantPredicate((targetId))))
                                )
                                .filter(user -> Boolean.parseBoolean(user.getAttributes().getOrDefault(KEYCLOAK_USER_ATTRIBUTE_PUSH_NOTIFICATIONS_DISABLED, Collections.singletonList("false")).get(0)))
                                .map(User::getId)
                                .toArray(String[]::new);

                        String[] assetIds = assetStorageService.findUserAssetLinks(targetId, null, null).stream()
                                .filter(userAssetLink -> Arrays.stream(userIds).anyMatch(userId->userId.equals(userAssetLink.getId().getUserId())))
                                .map(userAssetLink -> userAssetLink.getId().getAssetId())
                                .toArray(String[]::new);

                        //Remove consoleAssets which are linked to an User which has pushNotificationsDisabled set to false
                        consoleAssets = consoleAssets.stream()
                                .filter(consoleAsset -> Arrays.stream(assetIds).noneMatch(assetId -> assetId.equals(consoleAsset.getId())))
                                .collect(Collectors.toList());

                            mappedTargets.addAll(
                                    consoleAssets
                                            .stream()
                                            .map(asset -> new Notification.Target(Notification.TargetType.ASSET, asset.getId()))
                                            .collect(Collectors.toList()));
                        break;

                    case USER:
                        Optional<User> user = Arrays.stream(managerIdentityService
                                .getIdentityProvider()
                                .queryUsers(new UserQuery().ids(targetId))).findFirst();

                        if (user.isPresent() && !Boolean.parseBoolean(user.get().getAttributes().getOrDefault(KEYCLOAK_USER_ATTRIBUTE_PUSH_NOTIFICATIONS_DISABLED, Collections.singletonList("false")).get(0))) {

                            // Get all console assets linked to the specified user
                            String[] ids = assetStorageService.findUserAssetLinks(null, targetId, null)
                                    .stream()
                                    .map(userAssetLink -> userAssetLink.getId().getAssetId()).toArray(String[]::new);

                            if (ids.length > 0) {
                                mappedTargets.addAll(
                                        assetStorageService.findAll(
                                                new AssetQuery()
                                                        .select(AssetQuery.Select.selectExcludeAll())
                                                        .ids(ids)
                                                        .types(ConsoleAsset.class)
                                                        .attributes(new AttributePredicate(ConsoleAsset.CONSOLE_PROVIDERS, null, false, new NameValuePredicate.Path(PushNotificationMessage.TYPE))))
                                        .stream()
                                        .map(asset -> new Notification.Target(Notification.TargetType.ASSET, asset.getId()))
                                        .collect(Collectors.toList()));
                            }
                        } else {
                            LOG.fine("No console assets linked to target user");
                            return;
                        }

                        break;

                    case ASSET:
                        // Find all console descendants of the specified asset
                        consoleAssets = assetStorageService.findAll(new AssetQuery()
                                .select(AssetQuery.Select.selectExcludeAll())
                                .paths(new PathPredicate(targetId))
                                .types(ConsoleAsset.class)
                                .attributes(new AttributePredicate(ConsoleAsset.CONSOLE_PROVIDERS, null, false, new NameValuePredicate.Path(PushNotificationMessage.TYPE))));

                        UserAssetLink[] userAssetLinks = consoleAssets.stream()
                                .map(consoleAsset -> assetStorageService.findUserAssetLinks(null, null, consoleAsset.getId()))
                                .flatMap(Collection::stream)
                                .toArray(UserAssetLink[]::new);

                        // Get all user ids which have pushNotificationsDisabled set to false
                        assetIds = Arrays.stream(userAssetLinks)
                                .filter(userAssetLink -> Arrays.stream(managerIdentityService
                                            .getIdentityProvider()
                                            .queryUsers(new UserQuery().asset(new UserAssetPredicate(userAssetLink.getId().getAssetId())))
                                        )
                                        .filter(user1 -> Boolean.parseBoolean(user1.getAttributes().getOrDefault(KEYCLOAK_USER_ATTRIBUTE_PUSH_NOTIFICATIONS_DISABLED, Collections.singletonList("false")).get(0)))
                                        .map(User::getId)
                                        .anyMatch(userId -> userId.equals(userAssetLink.getId().getUserId()))
                                ).map(userAssetLink -> userAssetLink.getId().getAssetId())
                                .toArray(String[]::new);

                        //Remove consoleAssets which are linked to an User which has pushNotificationsDisabled set to false
                        consoleAssets = consoleAssets.stream()
                                .filter(consoleAsset -> Arrays.stream(assetIds).noneMatch(assetId -> assetId.equals(consoleAsset.getId())))
                                .collect(Collectors.toList());


                        mappedTargets.addAll(
                                consoleAssets
                                        .stream()
                                        .map(asset -> new Notification.Target(Notification.TargetType.ASSET, asset.getId()))
                                        .collect(Collectors.toList()));
                        break;
                }
            });
        }

        return mappedTargets;
    }

    @Override
    public NotificationSendResult sendMessage(long id, Notification.Source source, String sourceId, Notification.Target target, AbstractNotificationMessage message) {

        Notification.TargetType targetType = target.getType();
        String targetId = target.getId();

        if (targetType != Notification.TargetType.ASSET && targetType != Notification.TargetType.CUSTOM) {
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
                break;
            case CONDITION:
                // TODO: Decide how to handle conditions support (too much power for users to put anything in target)
                break;
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

    protected boolean isConsoleSubscribedToTopic(ConsoleAsset consoleAsset, String topic) {
        return consoleAsset.getConsoleProviders().flatMap(consoleProviders ->
            Optional.ofNullable(consoleProviders.get(PushNotificationMessage.TYPE))
                .map(ConsoleProvider::getData)
                .map(objectValue -> objectValue.withArray("topics"))
                .map(arrayValue -> StreamSupport.stream(arrayValue.spliterator(), false).anyMatch(node -> node.asText("").equals(topic))))
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
                    .setSound("default").setCategory("openremoteNotification");

            webpushConfigBuilder.setNotification(new WebpushNotification(pushMessage.getTitle(), pushMessage.getBody()));
        }

        if (pushMessage.getData() != null) {
            builder.putAllData(ValueUtil.JSON.convertValue(pushMessage.getData(), new TypeReference<Map<String, String>>(){}));

            if (dataOnly) {
                apsBuilder.setContentAvailable(true);
            }
        }

        // Store ID so console can mark notification as delivered and/or acknowledged
        builder.putData("notification-id", Long.toString(id));

        try {
            if (pushMessage.getAction() != null) {
                builder.putData("action", ValueUtil.asJSONOrThrow(pushMessage.getAction()));
            }

            if (pushMessage.getButtons() != null) {
                builder.putData("buttons", ValueUtil.asJSONOrThrow(pushMessage.getButtons()));
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

    protected static boolean isLinkedToFcmProvider(ConsoleAsset asset) {
        return asset.getConsoleProviders().flatMap(consoleProviders ->
            Optional.ofNullable(consoleProviders.get(PushNotificationMessage.TYPE))
                .map(ConsoleProvider::getVersion)
                .map(FCM_PROVIDER_NAME::equals))
            .orElse(false);
    }

    protected static Optional<String> getFcmToken(ConsoleAsset asset) {
        return asset.getConsoleProviders().flatMap(consoleProviders ->
            Optional.ofNullable(consoleProviders.get(PushNotificationMessage.TYPE))
                .map(ConsoleProvider::getData)
                .map(data -> data.get("token").asText()));
    }

    protected void processConsoleAssetChange(ConsoleAsset asset, PersistenceEvent<ConsoleAsset> persistenceEvent) {

        withLock(getClass().getSimpleName() + "::processAssetChange", () -> {

            String fcmToken = consoleFCMTokenMap.remove(asset.getId());
            if (!TextUtil.isNullOrEmpty(fcmToken)) {
                fcmTokenBlacklist.remove(fcmToken);
            }

            switch (persistenceEvent.getCause()) {

                case CREATE:
                case UPDATE:

                    consoleFCMTokenMap.put(asset.getId(), getFcmToken(asset).orElse(null));
                    break;
            }
        });
    }

    protected void handleFcmException(FirebaseMessagingException e) {

        LOG.log(Level.WARNING, "FCM send failed: " + e.getErrorCode(), e);

//        // TODO: Implement backoff and blacklisting
//        switch (e.getErrorCode()) {
//
//            case INVALID_ARGUMENT:
//            case UNAUTHENTICATED:
//                LOG.severe("FCM critical error so marking FCM as invalid no more messages will be sent");
//                break;
//            case UNAVAILABLE:
//            case INTERNAL:
//                break;
//        }
    }
}
