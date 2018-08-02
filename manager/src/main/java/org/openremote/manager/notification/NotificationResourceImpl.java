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

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.WebResource;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.http.RequestParams;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationResource;
import org.openremote.model.notification.SentNotification;
import org.openremote.model.value.Value;

import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.*;
import static org.openremote.model.notification.Notification.Source.CLIENT;

public class NotificationResourceImpl extends WebResource implements NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResourceImpl.class.getName());

    final protected NotificationService notificationService;
    final protected MessageBrokerService messageBrokerService;
    final protected AssetStorageService assetStorageService;
    final protected ManagerIdentityService managerIdentityService;

    public NotificationResourceImpl(NotificationService notificationService,
                                    MessageBrokerService messageBrokerService,
                                    AssetStorageService assetStorageService,
                                    ManagerIdentityService managerIdentityService) {
        this.notificationService = notificationService;
        this.messageBrokerService = messageBrokerService;
        this.assetStorageService = assetStorageService;
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public SentNotification[] getNotifications(RequestParams requestParams, List<String> types, Long timestamp, List<String> tenantIds, List<String> userIds, List<String> assetIds) {
        try {
            return notificationService.getNotifications(types, timestamp, tenantIds, userIds, assetIds).toArray(new SentNotification[0]);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
        }
    }

    @Override
    public void removeNotifications(RequestParams requestParams, List<String> types, Long timestamp, List<String> tenantIds, List<String> userIds, List<String> assetIds) {
        try {
            notificationService.removeNotifications(types, timestamp, tenantIds, userIds, assetIds);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
        }
    }

    @Override
    public void removeNotification(RequestParams requestParams, Long notificationId) {
        if (notificationId == null) {
            throw new WebApplicationException("Missing notification ID", BAD_REQUEST);
        }

        notificationService.removeNotification(notificationId);
    }

    @Override
    public void sendNotification(RequestParams requestParams, Notification notification) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(Notification.HEADER_SOURCE, CLIENT);

        if (isAuthenticated()) {
            headers.put(Constants.AUTH_CONTEXT, getAuthContext());
        }

        Object result = messageBrokerService.getProducerTemplate().requestBodyAndHeaders(
            NotificationService.NOTIFICATION_QUEUE, notification, headers);

        if (result instanceof NotificationProcessingException) {
            NotificationProcessingException processingException = (NotificationProcessingException) result;
            switch (processingException.getReason()) {

                case INSUFFICIENT_ACCESS:
                    throw new WebApplicationException(processingException.getMessage(), FORBIDDEN);
                case MISSING_SOURCE:
                case MISSING_NOTIFICATION:
                case MISSING_MESSAGE:
                case MISSING_TARGETS:
                    throw new WebApplicationException(processingException.getMessage(), BAD_REQUEST);
                case UNSUPPORTED_MESSAGE_TYPE:
                    throw new IllegalStateException(processingException);
            }
        }
    }

    @Override
    public void notificationDelivered(RequestParams requestParams, String targetId, Long notificationId) {
        if (notificationId == null) {
            throw new WebApplicationException("Missing notification ID", BAD_REQUEST);
        }

        SentNotification sentNotification = notificationService.getSentNotification(notificationId);
        verifyAccess(sentNotification, targetId);
        notificationService.setNotificationDelivered(notificationId);
    }

    @Override
    public void notificationAcknowledged(RequestParams requestParams, String targetId, Long notificationId, Value acknowledgement) {
        if (notificationId == null) {
            throw new WebApplicationException("Missing notification ID", BAD_REQUEST);
        }

        SentNotification sentNotification = notificationService.getSentNotification(notificationId);
        verifyAccess(sentNotification, targetId);
        notificationService.setNotificationAcknowleged(notificationId, acknowledgement.toString());
    }

    protected void verifyAccess(SentNotification sentNotification, String targetId) {
        if (sentNotification == null) {
            LOG.fine("DENIED: Notification not found");
            throw new WebApplicationException(NOT_FOUND);
        }

        if (sentNotification.getTargetId() == null || !sentNotification.getTargetId().equals(targetId)) {
            LOG.fine("DENIED: Notification target ID doesn't match supplied target ID");
            throw new WebApplicationException(NOT_FOUND);
        }

        if (isSuperUser()) {
            LOG.finest("ALLOWED: Request from super user so allowing");
            return;
        }

        // Anonymous requests can only be actioned against public assets
        if (!isAuthenticated()) {
            if (sentNotification.getTarget() != Notification.TargetType.ASSET) {
                LOG.fine("DENIED: Anonymous request to update a notification not sent to a public asset");
                throw new WebApplicationException("Anonymous request can only update public assets", FORBIDDEN);
            }

            // Check asset is public read amd not linked to any users
            Asset asset = assetStorageService.find(sentNotification.getTargetId(), false, BaseAssetQuery.Access.PUBLIC_READ);
            if (asset == null) {
                LOG.fine("DENIED: Anonymous request to update a notification sent to an asset that doesn't exist or isn't public");
                throw new WebApplicationException("Anonymous request can only update public assets not linked to a user", FORBIDDEN);
            }
            if (assetStorageService.isUserAsset(asset.getId())) {
                LOG.fine("DENIED: Anonymous request to update a notification sent to an asset that is linked to one or more users");
                throw new WebApplicationException("Anonymous request can only update public assets not linked to a user", FORBIDDEN);
            }
        } else {
            // Regular users can only update notifications sent to them or assets in their realm
            // Restricted users can only update notifications sent to them or assets linked to them
            boolean isRestrictedUser = managerIdentityService.getIdentityProvider().isRestrictedUser(getUserId());
            switch (sentNotification.getTarget()) {

                case TENANT:
                    // What does it mean when a notification has been sent to a tenant - who can acknowledge them?
                    if (isRestrictedUser) {
                        LOG.fine("DENIED: Restricted user request to update a notification sent to a tenant");
                        throw new WebApplicationException("Restricted users cannot update a tenant notification", FORBIDDEN);
                    }
                    break;
                case USER:
                    if (!sentNotification.getTargetId().equals(getUserId())) {
                        LOG.fine("DENIED: User request to update a notification sent to a different user");
                        throw new WebApplicationException("Regular and restricted users can only update user notifications sent to themselves", FORBIDDEN);
                    }
                    break;
                case ASSET:
                    Asset asset = assetStorageService.find(sentNotification.getTargetId(), false);
                    if (asset == null) {
                        LOG.fine("DENIED: User request to update a notification sent to an asset that doesn't exist");
                        throw new WebApplicationException("Asset not found", NOT_FOUND);
                    }
                    if (!asset.getRealmId().equals(managerIdentityService.getIdentityProvider().getTenantForRealm(getAuthenticatedRealm()).getId())) {
                        LOG.fine("DENIED: User request to update a notification sent to an asset that is in another realm");
                        throw new WebApplicationException("Asset not in users realm", FORBIDDEN);
                    }
                    if (isRestrictedUser && !assetStorageService.isUserAsset(getUserId(), asset.getId())) {
                        LOG.fine("DENIED: Restricted user request to update a notification sent to an asset that isn't linked to themselves");
                        throw new WebApplicationException("Asset not linked to restricted user", FORBIDDEN);
                    }
                    break;
            }
        }
    }
}
