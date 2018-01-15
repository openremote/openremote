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
package org.openremote.app.client.app.dialog;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.app.client.widget.FormTextArea;
import org.openremote.app.client.widget.IconLabel;
import org.openremote.app.client.widget.PushButton;
import org.openremote.model.interop.Consumer;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;
import org.openremote.model.value.Values;
import org.openremote.model.value.impl.ValueUtil;

import javax.inject.Inject;

public class JsonEditorImpl implements JsonEditor {

    final protected Dialog dialog;
    final protected WidgetStyle widgetStyle;
    final protected ManagerMessages managerMessages;

    FormTextArea editor;
    FlowPanel errorPanel = new FlowPanel();
    Consumer<Value> onApply;
    Runnable onReset;
    Runnable onCancel;

    @Inject
    public JsonEditorImpl(Dialog dialog,
                          WidgetStyle widgetStyle,
                          ManagerMessages managerMessages) {
        this.dialog = dialog;
        this.widgetStyle = widgetStyle;
        this.managerMessages = managerMessages;

        dialog.setModal(true);
        dialog.setAutoHideOnHistoryEvents(true);
        dialog.addStyleName(widgetStyle.JsonEditor());

        PushButton okButton = new PushButton();
        okButton.setFocus(true);
        okButton.setText(managerMessages.OK());
        okButton.setIcon("check");
        okButton.addStyleName(widgetStyle.FormControl());
        okButton.addStyleName(widgetStyle.PushButton());
        okButton.addStyleName(widgetStyle.FormButtonPrimary());
        okButton.addClickHandler(event -> {
            Value value = parseValue();
            if (value != null) {
                if (onApply != null)
                    onApply.accept(value);
                dialog.hide();
            }
        });
        dialog.getFooterPanel().add(okButton);

        PushButton resetButton = new PushButton();
        resetButton.setText(managerMessages.reset());
        resetButton.setIcon("refresh");
        resetButton.addStyleName(widgetStyle.FormControl());
        resetButton.addStyleName(widgetStyle.PushButton());
        resetButton.addStyleName(widgetStyle.FormButton());
        resetButton.addClickHandler(event -> {
            if (onReset != null)
                onReset.run();
        });
        dialog.getFooterPanel().add(resetButton);

        PushButton cancelButton = new PushButton();
        cancelButton.setText(managerMessages.cancel());
        cancelButton.setIcon("close");
        cancelButton.addStyleName(widgetStyle.FormControl());
        cancelButton.addStyleName(widgetStyle.PushButton());
        cancelButton.addStyleName(widgetStyle.FormButton());
        cancelButton.addClickHandler(event -> {
            if (errorPanel.isVisible() && onReset != null) {
                onReset.run();
            }
            dialog.hide();
            if (onCancel != null)
                onCancel.run();
        });
        dialog.getFooterPanel().add(cancelButton);

        errorPanel.setStyleName("flex-none layout horizontal error");
        errorPanel.addStyleName(widgetStyle.FormMessages());
        errorPanel.setVisible(false);
        dialog.getContentPanel().add(errorPanel);

        editor = new FormTextArea();
        editor.setHeight("30em");
        editor.setResizable(false);
        editor.setBorder(true);
        editor.setReadOnly(onApply == null);
        dialog.getContentPanel().add(editor);
    }

    @Override
    public void setTitle(String title) {
        dialog.setHeaderLabel(title);
    }

    @Override
    public void setValue(Value value) {
        errorPanel.clear();
        errorPanel.setVisible(false);
        editor.setText(value != null ? ValueUtil.stringify(value, 4) : null);
    }

    @Override
    public void setOnApply(Consumer<Value> onApply) {
        this.onApply = onApply;
        editor.setReadOnly(onApply == null);
    }

    @Override
    public void setOnReset(Runnable onReset) {
        this.onReset = onReset;
    }

    @Override
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @Override
    public void show() {
        dialog.showCenter();
    }

    protected Value parseValue() {
        errorPanel.clear();
        Value value = null;
        String error = null;
        try {
            value = Values.parse(editor.getText()).orElse(null);
            if (value == null)
                error = managerMessages.emptyJsonData();
        } catch (ValueException ex) {
            error = ex.getMessage();
        }
        if (error != null) {
            IconLabel warningLabel = new IconLabel();
            warningLabel.setIcon("warning");
            warningLabel.addStyleName(widgetStyle.MessagesIcon());
            errorPanel.add(warningLabel);
            InlineLabel errorMessage = new InlineLabel(error);
            errorPanel.add(errorMessage);
            errorPanel.setVisible(true);
            return null;
        }
        errorPanel.setVisible(false);
        return value;
    }
}