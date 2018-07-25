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

import org.openremote.container.ContainerService;
import org.openremote.model.asset.AssetType;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationSendResult;

/**
 * A {@link NotificationHandler} is responsible for sending compatible {@link AbstractNotificationMessage} messages to
 * the specified target. Before a message is sent:
 * <ol>
 *     <li>The {@link #isMessageValid} method will be called which allows the {@link NotificationHandler} to validate
 *     that the message is compatible and correctly structured.
 *     <li>The {@link #mapTarget} method will be called which allows the {@link NotificationHandler} to map the requested
 *     target to a target that is compatible with this handler.
 * </ol>
 */
public interface NotificationHandler extends ContainerService {

    /**
     * The unique type name; it must correspond with the value set in {@link AbstractNotificationMessage#getType} so that
     * {@link AbstractNotificationMessage}s can be mapped to a {@link NotificationHandler}.
     */
    String getTypeName();

    /**
     * Allows the handler to validate the specified {@link AbstractNotificationMessage}.
     */
    boolean isMessageValid(AbstractNotificationMessage message);

    /**
     * Map the requested target to one or more targets that are compatible with this handler; if there are no compatible
     * targets then return null. If the requested target is already compatible then it can just be returned; handlers
     * are free to determine how the requested target should be mapped (e.g. if requested {@link Notification.TargetType}
     * was {@link Notification.TargetType#TENANT} and this handler only supports targets of type
     * {@link Notification.TargetType#ASSET} where {@link AssetType} equals {@link AssetType#CONSOLE} then the handler
     * needs to find all console assets that belong to the specified tenant).
     */
    Notification.Targets mapTarget(Notification.TargetType targetType, String targetId, AbstractNotificationMessage message);

    /**
     * Send the specified {@link AbstractNotificationMessage} to the target; the target supplied would be a target
     * previously returned by {@link #mapTarget}. It is the responsibility of the {@link NotificationHandler} to
     * maintain any required cache to ensure this call is as performant as possible (i.e. the handler should avoid
     * making excessive DB calls if possible).
     * <p>
     * The ID can be used by the {@link NotificationHandler} to update the delivered and/or acknowledged status of the notification
     * by calling {@link NotificationService#setNotificationDelivered} or {@link NotificationService#setNotificationAcknowleged}
     */
    NotificationSendResult sendMessage(long id, Notification.TargetType targetType, String targetId, AbstractNotificationMessage message);
}
