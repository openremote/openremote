package org.openremote.model.notification;

public interface LocalizedNotificationMessage {
    AbstractNotificationMessage getMessage(String language);
}
