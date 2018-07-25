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
package org.openremote.app.client.admin.users.notifications;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Provider;
import org.openremote.app.client.widget.*;
import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.users.AdminUsersNavigation;
import org.openremote.app.client.app.dialog.Confirmation;
import org.openremote.model.Constants;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.notification.SentNotification;

import javax.inject.Inject;

public class AdminUserNotificationsImpl extends FormViewImpl implements AdminUserNotifications {

    interface UI extends UiBinder<HTMLPanel, AdminUserNotificationsImpl> {
    }

    @UiField(provided = true)
    AdminUsersNavigation adminUsersNavigation;

    @UiField
    FormOutputText usernameOutput;

    @UiField
    FormButton sendNotificationButton;

    @UiField
    FormButton refreshNotificationsButton;

    @UiField
    FormButton deleteNotificationsButton;

    @UiField
    FlowPanel notificationsContainer;

    protected Presenter presenter;

    @Inject
    public AdminUserNotificationsImpl(Environment environment,
                                      Provider<Confirmation> confirmationDialogProvider,
                                      AdminUsersNavigation adminUsersNavigation) {
        super(confirmationDialogProvider, environment.getWidgetStyle());

        this.adminUsersNavigation = adminUsersNavigation;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        sendNotificationButton.addClickHandler(event -> {
            if (presenter != null) {
                presenter.onSendNotification();
            }
        });

        refreshNotificationsButton.addClickHandler(event -> {
            if (presenter != null) {
                presenter.onRefresh();
            }
        });

        deleteNotificationsButton.addClickHandler(event -> {
            if (presenter != null) {
                presenter.onNotificationsDelete();
            }
        });
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            deleteNotificationsButton.setEnabled(false);
            adminUsersNavigation.setVisible(false);
            adminUsersNavigation.reset();
            usernameOutput.setText(null);
        } else {
            adminUsersNavigation.setActive(presenter.getPlace());
            adminUsersNavigation.setVisible(true);
        }
    }

    @Override
    public void setUsername(String username) {
        usernameOutput.setText(username);
    }

    @Override
    public void setNotifications(SentNotification[] notifications) {
        clearNotifications(notifications.length == 0);
        for (SentNotification notification : notifications) {
            notificationsContainer.add(new NotificationItem(notification));
        }
    }

    @Override
    public void removeNotification(Long id) {
        for (int i = 0; i < notificationsContainer.getWidgetCount(); i++) {
            if (notificationsContainer.getWidget(i) instanceof NotificationItem) {
                NotificationItem item = (NotificationItem) notificationsContainer.getWidget(i);
                if (item.getNotification().getId().equals(id))
                    notificationsContainer.remove(i);
            }
        }
        if (notificationsContainer.getWidgetCount() == 0) {
            clearNotifications(true);
        }
    }


    protected void clearNotifications(boolean addEmptyMessage) {
        deleteNotificationsButton.setEnabled(!addEmptyMessage);
        notificationsContainer.clear();
        if (addEmptyMessage) {
            Label emptyLabel = new Label(managerMessages.noNotifications());
            emptyLabel.addStyleName(widgetStyle.FormListEmptyMessage());
            notificationsContainer.add(emptyLabel);
        }
    }

    protected class NotificationItem extends FlowPanel {

        private final SentNotification notification;

        public NotificationItem(SentNotification notification) {
            this.notification = notification;
            setStyleName("flex-none layout vertical or-FormListItem");

            FormGroup statusGroup = new FormGroup();
            statusGroup.setFormGroupActions(new FormGroupActions());
            FormField statusField = new FormField();
            FormLabel statusLabel = new FormLabel(managerMessages.deliveryStatus());
            statusGroup.setFormLabel(statusLabel);
            statusGroup.setFormField(statusField);
            FormOutputText statusTxt = new FormOutputText("");
            statusField.add(statusTxt);

            FormGroup createdOnGroup = new FormGroup();
            createdOnGroup.addStyleName("flex");
            createdOnGroup.setFormGroupActions(new FormGroupActions());
            FormField createdOnField = new FormField();
            FormLabel createdOnLabel = new FormLabel(managerMessages.createdOn());
            createdOnGroup.setFormLabel(createdOnLabel);
            createdOnGroup.setFormField(createdOnField);
            FormOutputText createdOnTxt = new FormOutputText(
                DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(notification.getSentOn())
            );
            createdOnTxt.addStyleName("flex");
            createdOnField.add(createdOnTxt);

            FormButton deleteButton = new FormButton();
            deleteButton.setDanger(true);
            deleteButton.setIcon("trash-o");
            deleteButton.setText(managerMessages.deleteNotification());
            deleteButton.addClickHandler(event -> {
                if (presenter != null)
                    presenter.onNotificationDelete(notification.getId());
            });
            statusGroup.getFormGroupActions().add(deleteButton);

            FlowPanel createdStatusPanel = new FlowPanel();
            createdStatusPanel.setStyleName("layout horizontal wrap");
            createdStatusPanel.add(createdOnGroup);
            createdStatusPanel.add(statusGroup);
            add(createdStatusPanel);

            FormGroup titleGroup = new FormGroup();
            FormField titleField = new FormField();
            FormLabel titleLabel = new FormLabel(managerMessages.title());
            titleGroup.setFormLabel(titleLabel);
            titleGroup.setFormField(titleField);
            FormOutputText titleTxt = new FormOutputText(/*notification.getTitle()*/);
            titleField.add(titleTxt);
            add(titleGroup);

            FormGroup messageGroup = new FormGroup();
            FormField messageField = new FormField();
            FormLabel messageLabel = new FormLabel(managerMessages.message());
            messageGroup.setFormLabel(messageLabel);
            messageGroup.setFormField(messageField);
            FormTextArea messageTxt = new FormTextArea();
            messageTxt.setReadOnly(true);
            messageTxt.setHeight("3em");
            messageTxt.setResizable(false);
            messageTxt.setBorder(true);
            messageTxt.setText(""/*notification.getBody()*/);
            messageField.add(messageTxt);
            add(messageGroup);

/*            for (int i = 0; i < notification.getActions().length(); i++) {
                notification.getActions().get(i).ifPresent(action -> {
                    FormGroup actionGroup = new FormGroup();
                    FormField actionField = new FormField();
                    FormLabel actionLabel = new FormLabel(managerMessages.action());
                    actionGroup.setFormLabel(actionLabel);
                    actionGroup.setFormField(actionField);
                    FormTextArea actionTxt = new FormTextArea();
                    actionTxt.setReadOnly(true);
                    actionTxt.setResizable(false);
                    actionTxt.setBorder(true);
                    actionTxt.setHeight("3em");
                    actionTxt.setText(action.toJson());
                    actionField.add(actionTxt);
                    add(actionGroup);
                });
            }*/

            FormGroup appUrlGroup = new FormGroup();
            FormField appUrlField = new FormField();
            FormLabel appUrlLabel = new FormLabel(managerMessages.notificationAppUrl());
            appUrlGroup.setFormLabel(appUrlLabel);
            appUrlGroup.setFormField(appUrlField);
            FormOutputText appUrlTxt = new FormOutputText(/*notification.getAppUrl()*/);
            appUrlField.add(appUrlTxt);
            add(appUrlGroup);
        }

        public SentNotification getNotification() {
            return notification;
        }
    }

}
