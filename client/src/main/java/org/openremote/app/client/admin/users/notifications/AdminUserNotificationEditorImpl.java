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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.app.client.widget.*;
import org.openremote.app.client.app.dialog.Dialog;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.model.interop.Consumer;
import org.openremote.model.notification.AlertNotification;

import javax.inject.Inject;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AdminUserNotificationEditorImpl implements AdminUserNotificationEditor {

    final protected Dialog dialog;
    final protected WidgetStyle widgetStyle;
    final protected ManagerMessages managerMessages;

    final protected FlowPanel errorPanel = new FlowPanel();
    final protected FormInputText notificationTitleInput = new FormInputText();
    final protected FormTextArea notificationMessageInput = new FormTextArea();
    final protected FormInputText notificationAppUrlInput = new FormInputText();

    AlertNotification alertNotification;

    Consumer<AlertNotification> onSend;
    Runnable onClose;

    @Inject
    public AdminUserNotificationEditorImpl(Dialog dialog,
                                           WidgetStyle widgetStyle,
                                           ManagerMessages managerMessages) {
        this.dialog = dialog;
        this.widgetStyle = widgetStyle;
        this.managerMessages = managerMessages;

        dialog.setHeaderLabel(managerMessages.sendNotification());

        dialog.setModal(true);
        dialog.setAutoHideOnHistoryEvents(true);
        dialog.addStyleName(widgetStyle.AdminNotificationEditor());

        PushButton sendButton = new PushButton();
        sendButton.setFocus(true);
        sendButton.setText(managerMessages.sendNotification());
        sendButton.setIcon("send");
        sendButton.addStyleName(widgetStyle.FormControl());
        sendButton.addStyleName(widgetStyle.PushButton());
        sendButton.addStyleName(widgetStyle.FormButtonPrimary());
        sendButton.addClickHandler(event -> {
            AlertNotification notification = buildAlertNotification();
            if (notification != null) {
                if (onSend != null)
                    onSend.accept(notification);
                dialog.hide();
            }
        });
        dialog.getFooterPanel().add(sendButton);

        PushButton cancelButton = new PushButton();
        cancelButton.setText(managerMessages.cancel());
        cancelButton.setIcon("close");
        cancelButton.addStyleName(widgetStyle.FormControl());
        cancelButton.addStyleName(widgetStyle.PushButton());
        cancelButton.addStyleName(widgetStyle.FormButton());
        cancelButton.addClickHandler(event -> {
            dialog.hide();
            if (onClose != null)
                onClose.run();
        });
        dialog.getFooterPanel().add(cancelButton);

        errorPanel.setStyleName("flex-none layout horizontal error");
        errorPanel.addStyleName(widgetStyle.FormMessages());
        errorPanel.setVisible(false);
        dialog.getContentPanel().add(errorPanel);

        FormGroup titleGroup = new FormGroup();
        FormLabel titleLabel = new FormLabel(managerMessages.title());
        titleGroup.setFormLabel(titleLabel);
        FormField titleField = new FormField();
        titleGroup.setFormField(titleField);
        notificationTitleInput.addStyleName("flex");
        titleField.add(notificationTitleInput);
        dialog.getContentPanel().add(titleGroup);

        FormGroup messageGroup = new FormGroup();
        FormLabel messageLabel = new FormLabel(managerMessages.message());
        messageGroup.setFormLabel(messageLabel);
        FormField messageField = new FormField();
        messageGroup.setFormField(messageField);
        notificationMessageInput.setHeight("5em");
        notificationMessageInput.setResizable(false);
        notificationMessageInput.setBorder(true);
        messageField.add(notificationMessageInput);
        dialog.getContentPanel().add(messageGroup);

        FormGroup appUrlGroup = new FormGroup();
        FormLabel appUrlLabel = new FormLabel(managerMessages.notificationAppUrl());
        appUrlGroup.setFormLabel(appUrlLabel);
        FormField appUrlField = new FormField();
        appUrlGroup.setFormField(appUrlField);
        notificationAppUrlInput.addStyleName("flex");
        appUrlField.add(notificationAppUrlInput);
        dialog.getContentPanel().add(appUrlGroup);
    }

    @Override
    public void reset() {
        alertNotification = null;
        onSend = null;
        onClose = null;
        errorPanel.clear();
        notificationTitleInput.setValue(null);
        notificationMessageInput.setValue(null);
        notificationAppUrlInput.setValue(null);
    }

    @Override
    public void setAlertNotification(AlertNotification notification) {
        this.alertNotification = notification;
        errorPanel.clear();
        errorPanel.setVisible(false);
        notificationTitleInput.setText(notification.getTitle());
        notificationMessageInput.setText(notification.getMessage());
        notificationAppUrlInput.setText(notification.getAppUrl());
    }

    public void setOnSend(Consumer<AlertNotification> onSend) {
        this.onSend = onSend;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void show() {
        dialog.showCenter();
    }

    protected AlertNotification buildAlertNotification() {
        errorPanel.clear();
        String notificationTitle = notificationTitleInput.getText();
        String notificationMessage = notificationMessageInput.getText();
        String notificationAppUrl = notificationAppUrlInput.getText();
        if (isNullOrEmpty(notificationTitle)
            || isNullOrEmpty(notificationMessage)
            || isNullOrEmpty(notificationAppUrl)) {
            IconLabel warningLabel = new IconLabel();
            warningLabel.setIcon("warning");
            warningLabel.addStyleName(widgetStyle.MessagesIcon());
            errorPanel.add(warningLabel);
            InlineLabel errorMessage = new InlineLabel(managerMessages.enterTitleMessageAppurlForNotification());
            errorPanel.add(errorMessage);
            errorPanel.setVisible(true);
            return null;
        }
        errorPanel.setVisible(false);
        alertNotification.setTitle(notificationTitle);
        alertNotification.setMessage(notificationMessage);
        alertNotification.setAppUrl(notificationAppUrl);
        return alertNotification;
    }
}