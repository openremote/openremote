package org.openremote.manager.notification;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.model.Container;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.LocalizedNotificationMessage;
import org.openremote.model.notification.Notification;

import static org.openremote.manager.notification.NotificationProcessingException.Reason.*;

import java.util.*;
import java.util.logging.Logger;

public class LocalizedNotificationHandler extends RouteBuilder implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(LocalizedNotificationHandler.class.getName());
    protected Map<String, NotificationHandler> notificationHandlerMap = new HashMap<>();
    protected boolean valid;

    @Override
    public void init(Container container) throws Exception {
        container.getServices(NotificationHandler.class).forEach(notificationHandler -> {
            if(!notificationHandler.getTypeName().equals(this.getTypeName())) {
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

        if(localizedMessage.getMessages().isEmpty()) {
            LOG.warning("Invalid message: must either contain at least one localized message.");
            return false;
        }

        // For each language message, check its handler if the message is valid.
        return localizedMessage.getMessages().values().stream().allMatch(msg -> {
            if(notificationHandlerMap.containsKey(msg.getType())) {
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

                // Check if handler exists for this language
                if(!notificationHandlerMap.containsKey(entry.getKey())) {
                    return Collections.<Notification.Target>emptyList();
                }
                // Get valid targets for this language
                List<Notification.Target> langTargets = notificationHandlerMap.get(entry.getKey()).getTargets(source, sourceId, requestedTargets, entry.getValue());

                // Save in the 'data' variable of the Notification.Target, what languages of this Notification are 'supported'
                langTargets.forEach(t -> t.setData(t.getData().toString() + "," + entry.getKey()));
                return langTargets;

            }).flatMap(List::stream).toList();

        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }

        // TODO: Delete this
        // For each language, we check if the target is valid for that message (using super.getTargets())
        // After, we append the verified language to the "data" variable of the notification target.
        /*try {
            return localizedMessage.getMessages().entrySet().stream().map(entry -> {
                List<Notification.Target> languageTargets = super.getTargets(source, sourceId, targets, entry.getValue());
                languageTargets.forEach(t -> t.setData(t.getData().toString() + "," + entry.getKey()));
                return languageTargets;

            }).flatMap(List::stream).toList();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }*/

        return Collections.emptyList();
    }

    @Override
    public void sendMessage(long id, Notification.Source source, String sourceId, Notification.Target target, AbstractNotificationMessage message) throws Exception {

        if (!(message instanceof LocalizedNotificationMessage localizedMessage)) {
            throw new NotificationProcessingException(SEND_FAILURE, "Invalid message: '" + message.getClass().getSimpleName() + "' is not an instance of LocalizedNotificationMessage");
        }

        if(target.getData() == null) {
            throw new NotificationProcessingException(SEND_FAILURE, "No localized message could not be sent to target " + target.getId());
        }
        List<String> supportedMessageLocales = Arrays.asList(target.getData().toString().split(","));
        if(!supportedMessageLocales.contains(target.getLocale())) {
            throw new NotificationProcessingException(SEND_FAILURE, "The localized message could not be sent to target " + target.getId());
        }

        AbstractNotificationMessage targetMsg = localizedMessage.getMessage(target.getLocale());
        if(targetMsg == null) {
            throw new NotificationProcessingException(SEND_FAILURE, "Could not get message for the '" + target.getLocale() + "' language of target " + target.getId());
        }

        if(!notificationHandlerMap.containsKey(targetMsg.getType())) {
            throw new NotificationProcessingException(SEND_FAILURE, "Could not find the handler of type '" + targetMsg.getType() + "' for the message of target " + target.getId());
        }

        // Send message
        notificationHandlerMap.get(targetMsg.getType()).sendMessage(id, source, sourceId, target, targetMsg);
    }
}
