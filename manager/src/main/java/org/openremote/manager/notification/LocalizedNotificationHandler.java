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
            return localizedMessage.getMessages().entrySet().stream().map(entry -> {

                // Check if handler exists for this message type
                if (!notificationHandlerMap.containsKey(entry.getValue().getType())) {
                    return Collections.<Notification.Target>emptyList();
                }
                // Get valid targets for this language
                List<Notification.Target> langTargets = notificationHandlerMap.get(entry.getValue().getType()).getTargets(source, sourceId, requestedTargets, entry.getValue());

                // Save in the 'data' variable of the Notification.Target, what languages of this Notification are 'supported'
                langTargets.forEach(t -> t.addAllowedLocale(entry.getKey()));

                return langTargets;

            }).flatMap(List::stream).toList();

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

            if (target.getType().equals(Notification.TargetType.ASSET)) {

                // If locale of the target cannot be found,
                // we check the linked user of this console asset, to get the user preferred language through the identity provider.
                List<UserAssetLink> links = assetStorageService.findUserAssetLinks(null, null, target.getId());
                if (!links.isEmpty()) {
                    User user = identityService.getIdentityProvider().getUser(links.get(0).getId().getUserId());
                    if (user != null) {
                        targetLocale = user.getAttributeMap().get(LOCALE_ATTRIBUTE).get(0);
                    }
                }
            }
        }

        // If target locale is not set yet, we either use;
        // - the default language configured in the notification message.
        // - use one of the languages that got a 'green light' to be sent.
        if (TextUtil.isNullOrEmpty(targetLocale)) {
            targetLocale = Optional.ofNullable(localizedMessage.getDefaultLanguage()).orElse(target.getAllowedLocales().get(0));
        }

        // Check if the message configured for this language is allowed to be sent towards this target.
        if (!target.getAllowedLocales().contains(targetLocale)) {
            throw new NotificationProcessingException(SEND_FAILURE, "The localized message could not be sent to target " + target.getId());
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
