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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.app.client.app.dialog.Dialog;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.app.client.widget.*;
import org.openremote.model.interop.Consumer;
import org.openremote.model.notification.*;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import javax.inject.Inject;
import java.util.*;

import static org.openremote.app.client.widget.ValueEditors.createValueEditor;

public class NotificationEditorImpl implements NotificationEditor {

    public enum ActionType {
        GET,
        DISMISS,
        POST,
        PUT,
        DELETE,
        GET_SILENT,
        POST_SILENT,
        PUT_SILENT,
        DELETE_SILENT,
        GET_INBROWSER,
        POST_INBROWSER,
        PUT_INBROWSER,
        DELETE_INBROWSER
    }

    final protected Dialog dialog;
    final protected WidgetStyle widgetStyle;
    final protected ManagerMessages managerMessages;

    final protected PushButton sendButton = new PushButton();
    final protected FlowPanel resultPanel = new FlowPanel();
    final protected FormListBox realmsList = new FormListBox();
    final protected FormListBox targetTypesList = new FormListBox();
    final protected FormGroup targetsGroup = new FormGroup();
    final protected FormListBox targetsList = new FormListBox();
    final protected FormListBox typesList = new FormListBox();
    final protected Form form = new Form();
    final protected FlowPanel messagePanel = new FlowPanel();

    final protected FormInputText notificationTitleInput = new FormInputText();
    final protected FormTextArea notificationMessageInput = new FormTextArea();
    final protected FormInputText notificationAppUrlInput = new FormInputText();

    protected SendOptions sendOptions;
    protected Runnable onSend;
    protected Runnable onClose;
    protected AbstractNotificationMessage message;
    PushNotificationButtonArrayMapper pushNotificationButtonArrayMapper = GWT.create(PushNotificationButtonArrayMapper.class);


    @Inject
    public NotificationEditorImpl(WidgetStyle widgetStyle,
                                  ManagerMessages managerMessages) {
        this.dialog = new Dialog();
        this.widgetStyle = widgetStyle;
        this.managerMessages = managerMessages;

        dialog.setHeaderLabel(managerMessages.sendNotification());

        dialog.setModal(true);
        dialog.setAutoHideOnHistoryEvents(true);
        dialog.addStyleName(widgetStyle.NotificationEditor());

        sendButton.setFocus(true);
        sendButton.setText(managerMessages.sendNotification());
        sendButton.setIcon("send");
        sendButton.addStyleName(widgetStyle.FormControl());
        sendButton.addStyleName(widgetStyle.PushButton());
        sendButton.addStyleName(widgetStyle.FormButtonPrimary());
        sendButton.addClickHandler(event -> {
            if (onSend != null)
                onSend.run();
        });
        dialog.getFooterPanel().add(sendButton);

        PushButton cancelButton = new PushButton();
        cancelButton.setText(managerMessages.close());
        cancelButton.setIcon("close");
        cancelButton.addStyleName(widgetStyle.FormControl());
        cancelButton.addStyleName(widgetStyle.PushButton());
        cancelButton.addStyleName(widgetStyle.FormButton());
        cancelButton.addClickHandler(event -> {
            dialog.close();
            if (onClose != null)
                onClose.run();
        });
        dialog.getFooterPanel().add(cancelButton);

        resultPanel.setStyleName("flex-none layout horizontal");
        resultPanel.addStyleName(widgetStyle.FormMessages());
        resultPanel.setVisible(false);
        dialog.getContentPanel().add(resultPanel);

        dialog.getContentPanel().add(form);

        FormGroup realmsGroup = new FormGroup();
        FormLabel realmsLabel = new FormLabel(managerMessages.realm());
        realmsGroup.setFormLabel(realmsLabel);
        FormField realmsField = new FormField();
        realmsGroup.setFormField(realmsField);
        realmsField.add(realmsList);
        form.add(realmsGroup);

        FormGroup targetTypeGroup = new FormGroup();
        FormLabel targetTypeLabel = new FormLabel(managerMessages.targetType());
        targetTypeGroup.setFormLabel(targetTypeLabel);
        FormField targetTypeField = new FormField();
        targetTypeGroup.setFormField(targetTypeField);
        targetTypeField.add(targetTypesList);
        form.add(targetTypeGroup);

        FormLabel targetsLabel = new FormLabel(managerMessages.targets());
        targetsGroup.setFormLabel(targetsLabel);
        FormField targetsField = new FormField();
        targetsGroup.setFormField(targetsField);
        targetsField.add(targetsList);
        form.add(targetsGroup);

        FormGroup typeGroup = new FormGroup();
        FormLabel typeLabel = new FormLabel(managerMessages.type());
        typeGroup.setFormLabel(typeLabel);
        FormField typeField = new FormField();
        typeGroup.setFormField(typeField);
        typeField.add(typesList);
        form.add(typeGroup);

        messagePanel.setStyleName("flex-none layout vertical");
        messagePanel.setVisible(false);
        form.add(messagePanel);

        // Add change handlers
        realmsList.addChangeHandler(event -> {
            if (sendOptions != null) {
                sendOptions.setSelectedRealm(realmsList.getSelectedValue());
                
                if (sendOptions.getSelectedTargetType() == Notification.TargetType.TENANT) {
                    sendOptions.setSelectedTarget(sendOptions.getSelectedRealm());
                }
            }
        });

        targetTypesList.addChangeHandler(event -> {
            if (sendOptions != null) {
                sendOptions.setSelectedTargetType(targetTypesList.getSelectedValue());
                if (sendOptions.getSelectedTargetType() == Notification.TargetType.TENANT) {
                    sendOptions.setSelectedTarget(sendOptions.getSelectedRealm());
                    targetsGroup.setVisible(false);
                } else {
                    targetsList.selectItem("");
                    targetsGroup.setVisible(true);
                }
            }
        });

        targetsList.addChangeHandler(event -> {
            if (sendOptions != null) {
                sendOptions.setSelectedTarget(targetsList.getSelectedValue());
            }
        });

        typesList.addChangeHandler(event -> {
            if (sendOptions != null) {
                sendOptions.setSelectedNotificationType(typesList.getSelectedValue());
            }
            updateMessagePanel();
        });
    }

    @Override
    public void reset() {
        setSendOptions(null);
        onSend = null;
        onClose = null;
        message = null;
        resultPanel.clear();
        messagePanel.clear();
        notificationTitleInput.setValue(null);
        notificationMessageInput.setValue(null);
        notificationAppUrlInput.setValue(null);
    }

    @Override
    public void setSendOptions(SendOptions sendOptions) {
        if (this.sendOptions != null) {
            this.sendOptions.setTargetsChangedCallback(null);
        }

        typesList.clear();
        realmsList.clear();
        targetTypesList.clear();
        targetsList.clear();

        this.sendOptions = sendOptions;

        if (sendOptions == null) {
            return;
        }

        sendOptions.setTargetsChangedCallback(this::onTargetsChanged);

        // Type List
        Arrays.stream(sendOptions.getNotificationTypes()).forEach(notificationType ->
            typesList.addItem(managerMessages.notificationType(notificationType), notificationType));

        if (typesList.getItemCount() > 1) {
            typesList.insertItem("", 0);
        }
        typesList.selectItem(sendOptions.getSelectedNotificationType());

        // Realm List
        sendOptions.getRealms().forEach((name, value) ->
            realmsList.addItem(value, name)
        );
        if (realmsList.getItemCount() > 1) {
            realmsList.insertItem("", 0);
        }
        realmsList.selectItem(sendOptions.getSelectedRealm());

        // Target Types
        Arrays.stream(sendOptions.getTargetTypes()).forEach(targetType ->
            targetTypesList.addItem(managerMessages.targetTypes(targetType), targetType));
        if (targetTypesList.getItemCount() > 1) {
            targetTypesList.insertItem("", 0);
            targetTypesList.setSelectedIndex(0);
        }
        targetTypesList.selectItem(sendOptions.getSelectedTargetType() != null ? sendOptions.getSelectedTargetType().name() : null);

        // Only show target group list when target type is not TENANT
        targetsGroup.setVisible(sendOptions.getSelectedTargetType() != Notification.TargetType.TENANT);

        // Targets
        onTargetsChanged();

        updateMessagePanel();
    }

    protected void onTargetsChanged() {
        targetsList.clear();

        if (sendOptions.getTargets() == null) {
            return;
        }

        sendOptions.getTargets().forEach((name, value) -> targetsList.addItem(value, name));

        if (targetsList.getItemCount() > 1) {
            targetsList.insertItem("", 0);
            targetsList.setSelectedIndex(0);
        }

        if (sendOptions.getSelectedTargetType() == Notification.TargetType.TENANT) {
            targetsList.selectItem(sendOptions.getSelectedRealm());
        } else {
            targetsList.selectItem(sendOptions.getSelectedTarget());
        }
    }

    protected void updateMessagePanel() {
        if (sendOptions == null) {
            messagePanel.clear();
            return;
        }

        if (sendOptions.getMessage() != message) {
            messagePanel.clear();
            message = sendOptions.getMessage();
            if (message != null) {
                switch (message.getType()) {
                    case PushNotificationMessage.TYPE:
                        buildPushMessageEditor();
                }
                messagePanel.setVisible(true);
            }
        }
    }

    protected void buildPushMessageEditor() {
        PushNotificationMessage message = (PushNotificationMessage)this.message;

        if (message.getAction() == null) {
            message.setAction(new PushNotificationAction("", null, false, false, "GET"));
        }

        FormGroup titleGroup = new FormGroup();
        FormLabel titleLabel = new FormLabel(managerMessages.title());
        titleGroup.setFormLabel(titleLabel);
        FormField titleField = new FormField();
        titleGroup.setFormField(titleField);
        FormInputText titleInput = new FormInputText(message.getTitle());
        titleInput.addStyleName("flex");
        titleField.add(titleInput);
        messagePanel.add(titleGroup);

        FormGroup bodyGroup = new FormGroup();
        FormLabel bodyLabel = new FormLabel(managerMessages.body());
        bodyGroup.setFormLabel(bodyLabel);
        FormField bodyField = new FormField();
        bodyGroup.setFormField(bodyField);
        FormInputText bodyInput = new FormInputText(message.getBody());
        bodyInput.addStyleName("flex");
        bodyField.add(bodyInput);
        messagePanel.add(bodyGroup);

        FormGroup actionGroup = new FormGroup();
        FormLabel actionLabel = new FormLabel(managerMessages.action());
        actionGroup.setFormLabel(actionLabel);
        FormField actionField = new FormField();
        actionGroup.setFormField(actionField);
        createActionEditor(actionField, null, message.getAction(), null, this::onMessageActionChanged);
        messagePanel.add(actionGroup);

        FormGroup buttonsGroup = new FormGroup();
        FormLabel buttonsLabel = new FormLabel(managerMessages.buttons());
        buttonsGroup.setFormLabel(buttonsLabel);
        FormField buttonsField = new FormField();
        buttonsGroup.setFormField(buttonsField);
        Optional<Long> timestamp = Optional.empty();
        ArrayValue buttonsArray = null;

        if (message.getButtons() != null && message.getButtons().size() > 0) {
            List<PushNotificationButton> buttonList = message.getButtons();
            buttonsArray = Values.parse(pushNotificationButtonArrayMapper.write(buttonList)).flatMap(Values::getArray).orElse(null);
        }

        buttonsField.add(ValueEditors.createArrayEditor(
            buttonsArray,
            arrayValue -> {
                message.setButtons(pushNotificationButtonArrayMapper.read(arrayValue.toJson()));
            },
            timestamp,
            false,
            widgetStyle,
            managerMessages
        ));
        messagePanel.add(buttonsGroup);

        FormGroup dataGroup = new FormGroup();
        FormLabel dataLabel = new FormLabel(managerMessages.notificationData());
        dataGroup.setFormLabel(dataLabel);
        FormField dataField = new FormField();
        dataGroup.setFormField(dataField);
        messagePanel.add(dataGroup);
        dataField.add(ValueEditors.createObjectEditor(
            message.getData(),
            message::setData,
            timestamp,
            false,
            widgetStyle,
            managerMessages
        ));

        titleInput.addChangeHandler(event -> message.setTitle(titleInput.getValue()));
        bodyInput.addChangeHandler(event -> message.setBody(bodyInput.getValue()));

    }

    protected void onMessageActionChanged(PushNotificationAction action) {

    }
    
    protected void createActionEditor(FormField parent, String title, PushNotificationAction action, Consumer<String> titleChangedCallback, Consumer<PushNotificationAction> actionChangedCallback) {

        final FormInputText titleInput = new FormInputText(title);

        if (title != null && titleChangedCallback != null) {
            FormLabel titleLabel = new FormLabel(managerMessages.title());
            parent.add(titleLabel);
            parent.add(titleInput);
        }

        FormListBox typesList = new FormListBox();
        parent.add(typesList);

        Arrays.stream(ActionType.values()).forEach(actionType ->
            typesList.addItem(managerMessages.notificationActionType(actionType), actionType.name())
        );

        // Initialise the type list value
        ActionType selectedActionType = action.getHttpMethod() == null ?
            ActionType.DISMISS : ActionType.valueOf(action.getHttpMethod().toUpperCase(Locale.ROOT));
        if (selectedActionType != ActionType.DISMISS) {
            if (action.isSilent()) {
                selectedActionType = ActionType.valueOf(selectedActionType.name() + "_SILENT");
            } else if (action.isOpenInBrowser()) {
                selectedActionType = ActionType.valueOf(selectedActionType.name() + "_INBROWSER");
            }
        }
        typesList.selectItem(selectedActionType.name());

        FormLabel urlLabel = new FormLabel(managerMessages.notificationActionUrl());
        urlLabel.addStyleName(widgetStyle.NotificationActionLabel());
        FormInputText urlInput = new FormInputText(action.getUrl());
        urlLabel.setVisible(!ActionType.DISMISS.name().equals(typesList.getSelectedValue()));
        urlInput.setVisible(urlLabel.isVisible());
        parent.add(urlLabel);
        parent.add(urlInput);

        FormLabel dataLabel = new FormLabel(managerMessages.data());
        dataLabel.addStyleName(widgetStyle.NotificationActionLabel());
        FlowPanel dataEditorPanel = new FlowPanel();
        dataEditorPanel.setStyleName("flex-none layout horizontal");

        FormListBox dataTypeList = new FormListBox();
        dataTypeList.addItem(managerMessages.none(), "");
        Arrays.stream(ValueType.values())
            .map(Enum::name)
            .map(valueType -> new Pair<>(managerMessages.valueTypeDisplayName(valueType), valueType))
            .sorted(Comparator.comparing(typeEntry -> typeEntry.key))
            .forEach(typeEntry -> dataTypeList.addItem(typeEntry.key, typeEntry.value));
        ValueType currentValueType = Optional.ofNullable(action.getData()).map(Value::getType).orElse(null);
        dataTypeList.selectItem(currentValueType != null ? currentValueType.name() : null);
        dataEditorPanel.add(dataTypeList);
        updateActionDataValueEditor(dataEditorPanel, action.getData(), currentValueType, action::setData);

        dataLabel.setVisible(!ActionType.DISMISS.name().equals(typesList.getSelectedValue()));
        dataEditorPanel.setVisible(dataLabel.isVisible());
        parent.add(dataLabel);
        parent.add(dataEditorPanel);

        // Add change handlers
        if (titleChangedCallback != null) {
            titleInput.addChangeHandler(event -> titleChangedCallback.accept(titleInput.getValue()));
        }

        urlInput.addChangeHandler(event -> {
            action.setUrl(urlInput.getValue());
            actionChangedCallback.accept(action);
        });

        typesList.addChangeHandler(event -> {
            ActionType actionType = ActionType.valueOf(typesList.getSelectedValue());
            switch (actionType) {

                case DISMISS:
                    action.setUrl(null);
                    action.setHttpMethod(null);
                    action.setOpenInBrowser(false);
                    action.setSilent(true);
                    action.setData(null);
                    urlLabel.setVisible(false);
                    urlInput.setVisible(false);
                    dataLabel.setVisible(false);
                    dataEditorPanel.setVisible(false);
                    break;

                default:
                    String[] actionTypeArr = actionType.name().split("_");
                    action.setUrl(urlInput.getValue());
                    action.setHttpMethod(actionTypeArr[0]);
                    action.setOpenInBrowser(actionTypeArr.length == 2 && "INBROWSER".equals(actionTypeArr[1]));
                    action.setSilent(actionTypeArr.length == 2 && "SILENT".equals(actionTypeArr[1]));
                    urlLabel.setVisible(true);
                    urlInput.setVisible(true);
                    dataLabel.setVisible(true);
                    dataEditorPanel.setVisible(true);
                    break;
            }
        });

        dataTypeList.addChangeHandler(event -> {
            ValueType newValueType = TextUtil.isNullOrEmpty(dataTypeList.getSelectedValue()) ?
                null : ValueType.valueOf(dataTypeList.getSelectedValue());
            updateActionDataValueEditor(dataEditorPanel, action.getData(), newValueType, action::setData);
        });
    }

    protected void updateActionDataValueEditor(FlowPanel editorParent,
                                               Value currentValue,
                                               ValueType valueType,
                                               Consumer<Value> onValueModified) {

        if (editorParent.getWidgetCount() > 1) {
            editorParent.remove(1);
        }

        if (valueType != null) {
            IsWidget editor = createValueEditor(
                currentValue,
                valueType,
                onValueModified,
                Optional.empty(),
                false,
                null,
                widgetStyle,
                managerMessages
            );

            editorParent.add(editor);
        }
    }

    @Override
    public void setResult(NotificationSendResult result) {
        resultPanel.clear();
        resultPanel.setVisible(false);

        if (result != null) {
            if (result.isSuccess()) {
                resultPanel.add(new IconLabel("check"));
                resultPanel.removeStyleName("error");
                resultPanel.addStyleName("success");
                resultPanel.add(new InlineLabel(managerMessages.notificationSentSuccessfully()));
            } else {
                resultPanel.add(new IconLabel("warning"));
                resultPanel.addStyleName("error");
                resultPanel.removeStyleName("success");
                resultPanel.add(new InlineLabel(managerMessages.notificationSendFailure(result.getMessage())));
            }
            resultPanel.setVisible(true);
        }
    }

    @Override
    public void setBusy(boolean busy) {
        form.setBusy(busy);
        sendButton.setEnabled(!busy);
    }

    @Override
    public void setOnSend(Runnable onSend) {
        this.onSend = onSend;
    }

    @Override
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void show() {
        dialog.open();
    }
}