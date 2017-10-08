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
package org.openremote.manager.server.notification;

import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.notification.DeviceNotificationToken;
import org.openremote.model.notification.AlertNotification;
import org.openremote.manager.shared.notification.NotificationResource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class NotificationResourceImpl extends WebResource implements NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResourceImpl.class.getName());

    final protected NotificationService notificationService;

    public NotificationResourceImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void storeDeviceToken(RequestParams requestParams, String deviceId, String token, String deviceType) {
        if (token == null || token.length() == 0 || deviceId == null || deviceId.length() == 0) {
            throw new WebApplicationException("Missing token or device identifier", BAD_REQUEST);
        }
        notificationService.storeDeviceToken(deviceId, getUserId(), token, deviceType);
    }

    @Override
    public List<DeviceNotificationToken> getDeviceTokens(RequestParams requestParams, String userId) {
        if (!isSuperUser()) {
            LOG.fine("Forbidden access for user '" + getUsername() + "', can't get device tokens for user: " + userId);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return notificationService.findAllTokenForUser(userId);
    }

    @Override
    public void deleteDeviceToken(RequestParams requestParams, String userId, String deviceId) {
        if (!isSuperUser()) {
            LOG.fine("Forbidden access for user '" + getUsername() + "', can't delete device token  for user: " + userId);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        notificationService.deleteDeviceToken(deviceId, userId);
    }

    @Override
    public void storeAlertNotification(AlertNotification alertNotification) {
        if (alertNotification == null) {
            throw new WebApplicationException("Missing alertNotification", BAD_REQUEST);
        }
        notificationService.storeAndNotify(getUserId(), alertNotification);
    }

    @Override
    public List<AlertNotification> getAlertNotification() {
        return notificationService.getPendingAlertForUserId(getUserId());
    }

    @Override
    public void removeAlertNotification(Long id) {
        if (id == null) {
            throw new WebApplicationException("Missing alert id", BAD_REQUEST);
        }
        notificationService.removeAlertNotification(id);
    }
}
