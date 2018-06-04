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
package org.openremote.app.client.admin.users.notifications;

import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.*;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.admin.users.edit.AdminUserEditPlace;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.notification.ActionType;
import org.openremote.model.notification.AlertAction;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.notification.NotificationResource;
import org.openremote.model.security.User;
import org.openremote.model.security.UserResource;

import javax.inject.Inject;
import java.util.Collection;

public class AdminUserNotificationsActivity
    extends AbstractAdminActivity<AdminUserNotificationsPlace, AdminUserNotifications>
    implements AdminUserNotifications.Presenter {

    final protected Environment environment;
    final protected UserResource userResource;
    final protected UserMapper userMapper;
    final protected CredentialMapper credentialMapper;
    final protected RoleArrayMapper roleArrayMapper;
    final protected AdminUserNotificationEditor notificationEditor;
    final protected NotificationResource notificationResource;
    final protected AlertNotificationMapper alertNotificationMapper;
    final protected AlertNotificationArrayMapper alertNotificationArrayMapper;

    protected AdminUserEditPlace place;
    protected String realm;
    protected String userId;
    protected User user;
    protected AlertNotification alertNotification;

    @Inject
    public AdminUserNotificationsActivity(Environment environment,
                                          AdminView adminView,
                                          AdminNavigation.Presenter adminNavigationPresenter,
                                          AdminUserNotifications view,
                                          UserResource userResource,
                                          UserMapper userMapper,
                                          CredentialMapper credentialMapper,
                                          RoleArrayMapper roleArrayMapper,
                                          AdminUserNotificationEditor notificationEditor,
                                          NotificationResource notificationResource,
                                          AlertNotificationMapper alertNotificationMapper,
                                          AlertNotificationArrayMapper alertNotificationArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.environment = environment;
        this.userResource = userResource;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.roleArrayMapper = roleArrayMapper;
        this.notificationEditor = notificationEditor;
        this.notificationResource = notificationResource;
        this.alertNotificationMapper = alertNotificationMapper;
        this.alertNotificationArrayMapper = alertNotificationArrayMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin", "write:admin"};
    }

    @Override
    protected AppActivity<AdminUserNotificationsPlace> init(AdminUserNotificationsPlace place) {
        realm = place.getRealm();
        userId = place.getUserId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        adminContent.setPresenter(this);

        adminContent.clearFormMessages();

        if (userId != null) {
            loadUser();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationEditor.reset();
        adminContent.setPresenter(null);
        adminContent.clearFormMessages();
    }

    @Override
    public void onSendNotification() {
        // Keep it so we can send it again later
        if (this.alertNotification == null)
            this.alertNotification = createDefaultNotification();

        // TODO Constraint violation handler and @Valid AlertNotification on resource method?
        notificationEditor.setAlertNotification(this.alertNotification);
        notificationEditor.setOnSend(updatedNotification -> {
            adminContent.setFormBusy(true);
            this.alertNotification = updatedNotification;
            environment.getApp().getRequests().sendWith(
                alertNotificationMapper,
                requestParams -> notificationResource.storeNotificationForUser(requestParams, userId, this.alertNotification),
                204,
                () -> {
                    environment.getEventBus().dispatch(new ShowSuccessEvent(
                        environment.getMessages().notificationSentToUser()
                    ));
                    loadNotifications(() -> adminContent.setFormBusy(false));
                }
            );
        });
        notificationEditor.show();
    }

    @Override
    public void onRefresh() {
        loadNotifications(() -> {
        });
    }

    @Override
    public void onNotificationsDelete() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequests().send(
            requestParams -> notificationResource.removeNotificationsOfUser(requestParams, userId),
            204,
            () -> {
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().notificationsDeleted())
                );
                loadNotifications(() -> adminContent.setFormBusy(false));
            }
        );
    }

    @Override
    public void onNotificationDelete(Long id) {
        adminContent.setFormBusy(true);
        environment.getApp().getRequests().send(
            requestParams -> notificationResource.removeNotification(requestParams, userId, id),
            204,
            () -> {
                adminContent.setFormBusy(false);
                adminContent.removeNotification(id);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().notificationDeleted(id))
                );
            }
        );
    }

    protected AlertNotification createDefaultNotification() {
        AlertNotification alertNotification = new AlertNotification(
            "Hello User",
            "This is a test message.",
            // TODO: Remove below once consoles updated to use action appUrl
            "/#"
        );
        AlertAction alertAction = new AlertAction();
        alertAction.setTitle(environment.getMessages().notificationOpenApplicationDetails());
        alertAction.setActionType(ActionType.LINK);
        alertAction.setAppUrl("/#");
        alertNotification.addAction(alertAction);
        return alertNotification;
    }

    protected void loadUser() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequests().sendAndReturn(
            userMapper,
            requestParams -> userResource.get(requestParams, realm, userId),
            200,
            user -> {
                this.user = user;
                this.realm = user.getRealm();
                adminContent.setUsername(user.getFullName());
                loadNotifications(() -> {
                    adminContent.setFormBusy(false);
                });
            }
        );
    }

    protected void loadNotifications(Runnable onComplete) {
        environment.getApp().getRequests().sendAndReturn(
            alertNotificationArrayMapper,
            requestParams -> notificationResource.getNotificationsOfUser(requestParams, userId),
            200,
            notifications -> {
                adminContent.setNotifications(notifications);
                onComplete.run();
            }
        );
    }
}
