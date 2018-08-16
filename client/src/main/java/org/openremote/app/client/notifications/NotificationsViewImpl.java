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
package org.openremote.app.client.notifications;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.app.client.widget.*;
import org.openremote.model.Constants;
import org.openremote.model.notification.SentNotification;

import javax.inject.Inject;
import java.util.Arrays;

public class NotificationsViewImpl extends Composite implements NotificationsView {

    interface UI extends UiBinder<FlexSplitPanel, NotificationsViewImpl> {
    }

    @UiField
    Form form;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    FormButton sendNotificationButton;

    @UiField
    FormButton refreshNotificationsButton;

    @UiField
    FormButton deleteNotificationsButton;

    @UiField
    FlowPanel notificationsContainer;

    @UiField
    ManagerMessages managerMessages;

    @UiField
    WidgetStyle widgetStyle;

    @UiField
    FormListBox typesList;

    @UiField
    FormListBox realmsList;

    @UiField
    FormListBox targetTypesList;

    @UiField
    FormListBox targetsList;

    @UiField
    FormListBox sentInsList;

    protected FilterOptions filterOptions;
    protected Presenter presenter;
    final AssetBrowser assetBrowser;

    @Inject
    public NotificationsViewImpl(AssetBrowser assetBrowser) {
        this.assetBrowser = assetBrowser;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        sendNotificationButton.addClickHandler(event -> {
            if (presenter != null) {
                presenter.showNotificationEditor();
            }
        });

        refreshNotificationsButton.addClickHandler(event -> {
            if (presenter != null) {
                presenter.refreshNotifications();
            }
        });

        deleteNotificationsButton.addClickHandler(event -> {
            if (presenter != null) {
                presenter.deleteNotifications();
            }
        });

        // Add change handlers
        typesList.addChangeHandler(event -> {
            if (filterOptions != null)
                filterOptions.setSelectedNotificationType(typesList.getSelectedValue());
        });

        realmsList.addChangeHandler(event -> {
            if (filterOptions != null)
                filterOptions.setSelectedRealm(realmsList.getSelectedValue());
        });

        targetTypesList.addChangeHandler(event -> {
            if (filterOptions != null)
                filterOptions.setSelectedTargetType(targetTypesList.getSelectedValue());
        });

        targetsList.addChangeHandler(event -> {
            if (filterOptions != null)
                filterOptions.setSelectedTarget(targetsList.getSelectedValue());
        });
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        sidebarContainer.clear();

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setBusy(boolean busy) {
        form.setBusy(busy);
    }

    @Override
    public void setRefreshEnabled(boolean enabled) {
        refreshNotificationsButton.setEnabled(enabled);
    }

    @Override
    public void setDeleteAllEnabled(boolean enabled) {
        deleteNotificationsButton.setEnabled(enabled);
    }

    @Override
    public void setFilterOptions(FilterOptions filterOptions) {
        if (this.filterOptions != null) {
            this.filterOptions.setTargetsChangedCallback(null);
            typesList.clear();
            realmsList.clear();
            targetTypesList.clear();
            targetsList.clear();
            sentInsList.clear();
        }

        this.filterOptions = filterOptions;

        if (filterOptions == null) {
            return;
        }

        filterOptions.setTargetsChangedCallback(this::onTargetsChanged);

        // Type List
        Arrays.stream(filterOptions.getNotificationTypes()).forEach(notificationType ->
            typesList.addItem(managerMessages.notificationType(notificationType), notificationType));

        if (typesList.getItemCount() > 1) {
            typesList.insertItem("", 0);
        }
        typesList.selectItem(filterOptions.getSelectedNotificationType());

        // Realm List
        filterOptions.getRealms().forEach((name, value) ->
            realmsList.addItem(value, name)
        );
        if (realmsList.getItemCount() > 1) {
            realmsList.insertItem("", 0);
        }
        realmsList.selectItem(filterOptions.getSelectedRealm());

        // Target Types
        Arrays.stream(filterOptions.getTargetTypes()).forEach(targetType ->
            targetTypesList.addItem(managerMessages.targetTypes(targetType), targetType));
        if (targetTypesList.getItemCount() > 1) {
            targetTypesList.insertItem("", 0);
            targetTypesList.setSelectedIndex(0);
        }
        targetTypesList.selectItem(filterOptions.getSelectedTargetType() != null ? filterOptions.getSelectedTargetType().name() : null);

        // Targets
        onTargetsChanged();

        // Sent In
        Arrays.stream(FilterOptions.SentInLast.values()).forEach(sentInLast ->
            sentInsList.addItem(managerMessages.sentInLast(sentInLast), sentInLast.name()));
        if (sentInsList.getItemCount() > 1) {
            sentInsList.insertItem("", 0);
            sentInsList.setSelectedIndex(0);
        }
        if (filterOptions.getSelectedSentIn() != null) {
            sentInsList.selectItem(filterOptions.getSelectedSentIn().name());
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

    @Override
    public void setNotifications(SentNotification[] notifications) {
        clearNotifications(notifications.length == 0);
        for (SentNotification notification : notifications) {
            notificationsContainer.add(new NotificationItem(notification));
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

    protected void onTargetsChanged() {
        targetsList.clear();

        if (filterOptions.getTargets() == null) {
            return;
        }

        filterOptions.getTargets().forEach((name, value) -> targetsList.addItem(value, name));

        if (targetsList.getItemCount() > 1) {
            targetsList.insertItem("", 0);
            targetsList.setSelectedIndex(0);
        }

        targetsList.selectItem(filterOptions.getSelectedTarget());
    }

    protected class NotificationItem extends FlowPanel {

        private final SentNotification notification;

        public NotificationItem(SentNotification notification) {
            this.notification = notification;
            setStyleName("flex-none layout vertical or-FormListItem");

            FormGroup sentOnGroup = new FormGroup();
            sentOnGroup.addStyleName("flex");
            sentOnGroup.setFormGroupActions(new FormGroupActions());
            FormField sentOnField = new FormField();
            FormLabel sentOnLabel = new FormLabel(managerMessages.sentOn());
            sentOnGroup.setFormLabel(sentOnLabel);
            sentOnGroup.setFormField(sentOnField);
            FormOutputText sentOnTxt = new FormOutputText(
                DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(notification.getSentOn())
            );
            sentOnTxt.addStyleName("flex");
            sentOnField.add(sentOnTxt);
            add(sentOnGroup);

            FormGroup deliveredOnGroup = new FormGroup();
            deliveredOnGroup.addStyleName("flex");
            deliveredOnGroup.setFormGroupActions(new FormGroupActions());
            FormField deliveredOnField = new FormField();
            FormLabel deliveredOnLabel = new FormLabel(managerMessages.deliveredOn());
            deliveredOnGroup.setFormLabel(deliveredOnLabel);
            deliveredOnGroup.setFormField(deliveredOnField);
            FormOutputText deliveredOnTxt = new FormOutputText(
                notification.getDeliveredOn() != null
                    ? DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(notification.getDeliveredOn())
                    : ""
            );
            deliveredOnTxt.addStyleName("flex");
            deliveredOnField.add(deliveredOnTxt);
            add(deliveredOnGroup);

            FormGroup acknowledgedOnGroup = new FormGroup();
            acknowledgedOnGroup.addStyleName("flex");
            acknowledgedOnGroup.setFormGroupActions(new FormGroupActions());
            FormField acknowledgedOnField = new FormField();
            FormLabel acknowledgedOnLabel = new FormLabel(managerMessages.acknowledgedOn());
            acknowledgedOnGroup.setFormLabel(acknowledgedOnLabel);
            acknowledgedOnGroup.setFormField(acknowledgedOnField);
            FormOutputText acknowledgedOnTxt = new FormOutputText(
                notification.getAcknowledgedOn() != null
                    ? DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(notification.getAcknowledgedOn())
                    : ""
            );
            acknowledgedOnTxt.addStyleName("flex");
            acknowledgedOnField.add(acknowledgedOnTxt);
            add(acknowledgedOnGroup);

            FormButton deleteButton = new FormButton();
            deleteButton.setDanger(true);
            deleteButton.setIcon("trash-o");
            deleteButton.setText(managerMessages.deleteNotification());
            deleteButton.addClickHandler(event -> {
                if (presenter != null)
                    presenter.deleteNotification(notification.getId());
            });
            sentOnGroup.getFormGroupActions().add(deleteButton);

            FormGroup typeGroup = new FormGroup();
            FormField typeField = new FormField();
            FormLabel typeLabel = new FormLabel(managerMessages.type());
            typeGroup.setFormLabel(typeLabel);
            typeGroup.setFormField(typeField);
            FormOutputText typeTxt = new FormOutputText(notification.getType());
            typeField.add(typeTxt);
            add(typeGroup);

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
            messageTxt.setText(notification.getMessage().toString());
            messageField.add(messageTxt);
            add(messageGroup);

            FormGroup acknowledgementGroup = new FormGroup();
            FormField acknowledgementField = new FormField();
            FormLabel acknowledgementLabel = new FormLabel(managerMessages.acknowledgement());
            acknowledgementGroup.setFormLabel(acknowledgementLabel);
            acknowledgementGroup.setFormField(acknowledgementField);
            FormOutputText acknowledgementTxt = new FormOutputText(notification.getAcknowledgement());
            acknowledgementField.add(acknowledgementTxt);
            add(acknowledgementGroup);
            
            FormGroup errorGroup = new FormGroup();
            FormField errorField = new FormField();
            FormLabel errorLabel = new FormLabel(managerMessages.error());
            errorGroup.setFormLabel(errorLabel);
            errorGroup.setFormField(errorField);
            FormOutputText errorTxt = new FormOutputText(notification.getError());
            errorField.add(errorTxt);
            add(errorGroup);
        }

        public SentNotification getNotification() {
            return notification;
        }
    }

}
