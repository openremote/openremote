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
package org.openremote.manager.client.admin.users.edit;

import org.openremote.manager.client.admin.AdminContent;
import org.openremote.manager.client.admin.users.AbstractAdminUsersPlace;
import org.openremote.manager.shared.notification.DeviceNotificationToken;

import java.util.List;

public interface AdminUserEdit extends AdminContent {

    interface Presenter {

        AbstractAdminUsersPlace getPlace();

        void onRoleAssigned(String id, boolean assigned);

        void onDeviceRegistrationDelete(DeviceNotificationToken.Id id);

        void onDeviceRegistrationsRefresh();

        void create();

        void update();

        void delete();
    }

    void setPresenter(Presenter presenter);

    void setEditMode(boolean show);

    void setUsername(String username);

    String getUsername();

    void setUsernameError(boolean error);

    void setFirstName(String firstName);

    String getFirstName();

    void setFirstNameError(boolean error);

    void setLastName(String lastName);

    String getLastName();

    void setLastNameError(boolean error);

    void setEmail(String email);

    String getEmail();

    void setEmailError(boolean error);

    void setUserEnabled(Boolean enabled);

    boolean getUserEnabled();

    void setUserEnabledError(boolean error);

    void enableResetPassword(boolean enable);

    void enableRoles(boolean enable);

    String getPassword();

    void clearPassword();

    String getPasswordControl();

    void clearPasswordControl();

    void setPasswordError(boolean error);

    void clearRoles();

    void addRole(String id, String label, boolean composite, boolean assigned);

    void toggleRoleAssigned(String id, boolean assigned);

    void setDeviceRegistrations(List<DeviceNotificationToken> deviceNotificationTokens);

    void removeDeviceRegistration(DeviceNotificationToken.Id id);

    void enableCreate(boolean enable);

    void enableUpdate(boolean enable);

    void enableDelete(boolean enable);

}
