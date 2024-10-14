package org.openremote.model.notification;

import java.util.Map;

public interface LocalizedNotificationMessage {
    AbstractNotificationMessage getMessage(String language);
    Map<String, ? extends AbstractNotificationMessage> getMessages();
}
