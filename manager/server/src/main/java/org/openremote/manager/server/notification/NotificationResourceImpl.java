package org.openremote.manager.server.notification;

import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.notification.NotificationResource;

import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class NotificationResourceImpl extends WebResource implements NotificationResource {

    final protected NotificationService notificationService;

    public NotificationResourceImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void storeDeviceToken(RequestParams requestParams, String deviceId, String token) {
        if (token == null || token.length() == 0 || deviceId == null || deviceId.length() == 0) {
            throw new WebApplicationException("Missing token or device identifier", BAD_REQUEST);
        }
        notificationService.storeDeviceToken(deviceId, getAccessToken().getSubject(), token);
    }
}
