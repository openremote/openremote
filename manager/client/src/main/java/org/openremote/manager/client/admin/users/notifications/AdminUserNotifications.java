/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client.admin.users.notifications;

import org.openremote.manager.client.admin.AdminContent;
import org.openremote.manager.client.admin.users.AbstractAdminUsersPlace;
import org.openremote.model.notification.AlertNotification;

public interface AdminUserNotifications extends AdminContent {

    interface Presenter {

        AbstractAdminUsersPlace getPlace();

        void onRefresh();

        void onSendNotification();

        void onNotificationDelete(Long id);

        void onNotificationsDelete();

    }

    void setPresenter(Presenter presenter);

    void setUsername(String username);

    void setNotifications(AlertNotification[] notifications);

    void removeNotification(Long id);

}
