/*
 * Copyright 2024, OpenRemote Inc.
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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.LocalizedNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.security.User;
import org.openremote.model.util.TextUtil;

import static org.openremote.manager.notification.NotificationProcessingException.Reason.*;
import static org.openremote.model.security.User.LOCALE_ATTRIBUTE;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This {@link LocalizedNotificationHandler} handles notification differently compared to other handlers.
 * When a {@link LocalizedNotificationMessage} is processed, that normally contains a configuration for multiple languages,
 * we repeat the {@link #isMessageValid} and {@link #getTargets} methods for each individual locale. (using the {@link #notificationHandlerMap})
 */
public class LocalizedNotificationHandler extends RouteBuilder implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(LocalizedNotificationHandler.class.getName());
    private ManagerIdentityService identityService;
    private AssetStorageService assetStorageService;

    protected Map<String, NotificationHandler> notificationHandlerMap = new HashMap<>();
    protected boolean valid;

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        container.getServices(NotificationHandler.class).forEach(notificationHandler -> {
            if (!notificationHandler.getTypeName().equals(this.getTypeName())) {
                notificationHandlerMap.put(notificationHandler.getTypeName(), notificationHandler);
            }
        });
        valid = true;
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

    }

    @Override
    public String getTypeName() {
        return LocalizedNotificationMessage.TYPE;
    }

    @Override
    public boolean isMessageValid(AbstractNotificationMessage message) {

        if (!(message instanceof LocalizedNotificationMessage localizedMessage)) {
            LOG.warning("Invalid message: '" + message.getClass().getSimpleName() + "' is not an instance of LocalizedNotificationMessage");
            return false;
        }

        if (localizedMessage.getMessages().isEmpty()) {
            LOG.warning("Invalid message: must either contain at least one localized message.");
            return false;
        }

        // For each language message, check its handler if the message is valid.
        return localizedMessage.getMessages().values().stream().allMatch(msg -> {
            if (notificationHandlerMap.containsKey(msg.getType())) {
                return notificationHandlerMap.get(msg.getType()).isMessageValid(msg);
            }
            return false;
        });
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }


    @Override
    public List<Notification.Target> getTargets(Notification.Source source, String sourceId, List<Notification.Target> requestedTargets, AbstractNotificationMessage message) {

        LocalizedNotificationMessage localizedMessage = (LocalizedNotificationMessage) message;

        try {
            // Make a list of targets for each language
            Map<String, List<Notification.Target>> targetsMap = localizedMessage.getMessages().entrySet().stream()
                    .map(entry -> {

                        // Check if handler exists for this message type
                        if (!notificationHandlerMap.containsKey(entry.getValue().getType())) {
                            return Map.entry(entry.getKey(), Collections.<Notification.Target>emptyList());
                        }
                        // Get valid targets for this language
                        return Map.entry(entry.getKey(), notificationHandlerMap.get(entry.getValue().getType()).getTargets(source, sourceId, requestedTargets, entry.getValue()));

                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Merge these targets into a single List<Notification.Target>,
            // and add the list allowed language messages to the target using addAllowedLocales();
            Map<String, Notification.Target> mergedTargetsMap = new HashMap<>();
            targetsMap.forEach((language, targets) -> {
                for (Notification.Target target : targets) {
                    mergedTargetsMap.computeIfAbsent(target.getId(), k -> target)
                            .addAllowedLocales(new HashSet<>(Collections.singletonList(language)));
                }
            });
            return new ArrayList<>(mergedTargetsMap.values());


        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public void sendMessage(long id, Notification.Source source, String sourceId, Notification.Target target, AbstractNotificationMessage message) throws Exception {

        if (!(message instanceof LocalizedNotificationMessage localizedMessage)) {
            throw new NotificationProcessingException(SEND_FAILURE, "Invalid message: '" + message.getClass().getSimpleName() + "' is not an instance of LocalizedNotificationMessage");
        }

        // If all locales are 'blocked' for this target, don't send any message
        if (target.getAllowedLocales() == null || target.getAllowedLocales().isEmpty()) {
            throw new NotificationProcessingException(SEND_FAILURE, "No localized message could not be sent to target " + target.getId());
        }

        String targetLocale = target.getLocale();
        if (TextUtil.isNullOrEmpty(targetLocale)) {

            // If locale of the target cannot be found, we try to get the user preferred language through the identity provider.
            // For ASSET targets, we have to query the linked user of the console asset.
            User user = null;
            if (target.getType().equals(Notification.TargetType.USER)) {
                user = identityService.getIdentityProvider().getUser(target.getId());
            }
            else if (target.getType().equals(Notification.TargetType.ASSET)) {
                List<UserAssetLink> links = assetStorageService.findUserAssetLinks(null, null, target.getId());
                if (!links.isEmpty()) {
                    user = identityService.getIdentityProvider().getUser(links.getFirst().getId().getUserId());
                }
            }

            // If user has configured their locale (through the user attribute), update the targetLocale
            if (user != null && user.getAttributeMap() != null && user.getAttributeMap().containsKey(LOCALE_ATTRIBUTE)) {
                targetLocale = user.getAttributeMap().get(LOCALE_ATTRIBUTE).getFirst();
            }

        }
        // If target locale is not set yet, or there is no message available for that locale,
        // use the default language configured in the notification message
        if (TextUtil.isNullOrEmpty(targetLocale) || !localizedMessage.getMessages().containsKey(targetLocale)) {
            targetLocale = localizedMessage.getDefaultLanguage();
        }

        // Check if the message configured for this language is allowed to be sent towards this target.
        if (!target.getAllowedLocales().contains(targetLocale)) {
            throw new NotificationProcessingException(SEND_FAILURE, "The localized message could not be sent to target '" + target.getId() + "' (" + targetLocale + "), because only " + target.getAllowedLocales() + " were allowed.");
        }

        AbstractNotificationMessage targetMsg = localizedMessage.getMessage(targetLocale);
        if (targetMsg == null) {
            throw new NotificationProcessingException(SEND_FAILURE, "Could not get message for the '" + targetLocale + "' language of target " + target.getId());
        }

        if (!notificationHandlerMap.containsKey(targetMsg.getType())) {
            throw new NotificationProcessingException(SEND_FAILURE, "Could not find the handler of type '" + targetMsg.getType() + "' for the message of target " + target.getId());
        }

        // Send message
        notificationHandlerMap.get(targetMsg.getType()).sendMessage(id, source, sourceId, target, targetMsg);
    }
}
