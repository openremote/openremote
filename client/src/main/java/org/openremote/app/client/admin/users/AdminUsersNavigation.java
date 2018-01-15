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
package org.openremote.app.client.admin.users;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Inject;
import org.openremote.app.client.admin.users.edit.AdminUserEditPlace;
import org.openremote.app.client.admin.users.notifications.AdminUserNotificationsPlace;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.widget.Hyperlink;
import org.openremote.app.client.widget.SecondaryNavigation;

public class AdminUsersNavigation extends SecondaryNavigation {

    final PlaceHistoryMapper placeHistoryMapper;

    final Hyperlink allUsersLink;
    final Hyperlink editUserLink;
    final Hyperlink notificationsLink;
    final Hyperlink createUserLink;

    @Inject
    public AdminUsersNavigation(ManagerMessages managerMessages, PlaceHistoryMapper placeHistoryMapper) {
        this.placeHistoryMapper = placeHistoryMapper;

        allUsersLink = new Hyperlink("users", managerMessages.allUsers());
        add(allUsersLink);

        createUserLink = new Hyperlink("plus-square", managerMessages.newUser());
        add(createUserLink);

        editUserLink = new Hyperlink("edit", managerMessages.editUser());
        add(editUserLink);

        notificationsLink = new Hyperlink("send", managerMessages.manageNotifications());
        add(notificationsLink);
    }

    public void reset() {
        reset(allUsersLink);
        reset(createUserLink);
        reset(editUserLink);
        reset(notificationsLink);
    }

    public void setActive(AbstractAdminUsersPlace place) {
        reset();

        if (place.getRealm() != null) {
            allUsersLink.setTargetHistoryToken(
                placeHistoryMapper.getToken(new AdminUsersPlace(place.getRealm()))
            );
            allUsersLink.setVisible(true);

            createUserLink.setTargetHistoryToken(
                placeHistoryMapper.getToken(new AdminUserEditPlace(place.getRealm()))
            );
            createUserLink.setVisible(true);

        } else {
            allUsersLink.setVisible(false);
            createUserLink.setVisible(false);
        }

        editUserLink.setTargetHistoryToken(
            placeHistoryMapper.getToken(new AdminUserEditPlace(place))
        );

        if (place instanceof AdminUserEditPlace) {
            AdminUserEditPlace adminUserEditPlace = (AdminUserEditPlace) place;
            if (adminUserEditPlace.getUserId() == null) {
                createUserLink.addStyleName("active");
                createUserLink.setVisible(true);
            } else {
                editUserLink.addStyleName("active");
                editUserLink.setVisible(true);

                notificationsLink.setTargetHistoryToken(
                    placeHistoryMapper.getToken(new AdminUserNotificationsPlace(place))
                );
                notificationsLink.setVisible(true);

            }
        } else if (place instanceof AdminUserNotificationsPlace) {
            createUserLink.setVisible(true);
            editUserLink.setVisible(true);
            notificationsLink.addStyleName("active");
            notificationsLink.setVisible(true);
        } else if (place instanceof AdminUsersPlace) {
            allUsersLink.setVisible(false);
        }
    }
}
