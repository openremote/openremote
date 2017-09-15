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
package org.openremote.manager.client.widget;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.model.Constants;
import org.openremote.model.value.*;

import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * Collection of functions and types for {@link Value} editor UI.
 */
public final class ValueEditors {

    public static final String EMPTY_LINE = "----------------------";

    private static class TimestampLabel extends FlowPanel {

        static protected DateTimeFormat dateFormat = DateTimeFormat.getFormat(Constants.DEFAULT_DATE_FORMAT);
        static protected DateTimeFormat timeFormat = DateTimeFormat.getFormat(Constants.DEFAULT_TIME_FORMAT);

        public TimestampLabel(Long timestamp) {
            addStyleName("layout vertical end or-FormInfoLabel or-ValueTimestamp");
            if (timestamp != null && timestamp > 0) {
                add(new FormOutputText(dateFormat.format(new Date(timestamp))));
                add(new FormOutputText(timeFormat.format(new Date(timestamp))));
            }
        }
    }

    public static IsWidget createStringEditor(StringValue currentValue,
                                              Consumer<StringValue> onValueModified,
                                              Optional<Long> timestamp,
                                              boolean readOnly,
                                              String styleName) {

        Consumer<String> updateConsumer = readOnly ? null : str ->
            onValueModified.accept(isNullOrEmpty(str) ? null : Values.create(str));

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-StringValueEditor");
        IsWidget widget = ValueEditors.createStringEditorWidget(styleName, readOnly, currentValue != null ? currentValue.getString() : null, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        timestamp.ifPresent(time -> addTimestampLabel(time, panel));
        return panel;
    }


    public static IsWidget createNumberEditor(NumberValue currentValue,
                                              Consumer<NumberValue> onValueModified,
                                              Optional<Long> timestamp,
                                              boolean readOnly,
                                              String styleName) {

        Consumer<String> updateConsumer = readOnly ? null : str -> {
            NumberValue newValue = null;

            if (!isNullOrEmpty(str)) {
                try {
                    newValue = Values.create(Double.valueOf(str));
                } catch (NumberFormatException ignored) {
                }
            }

            onValueModified.accept(newValue);
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-NumberValueEditor");
        IsWidget widget = createStringEditorWidget(styleName, readOnly, currentValue != null ? currentValue.asString() : null, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        timestamp.ifPresent(time -> addTimestampLabel(time, panel));
        return panel;
    }

    public static IsWidget createBooleanEditor(BooleanValue currentValue,
                                               Consumer<BooleanValue> onValueModified,
                                               Optional<Long> timestamp,
                                               boolean readOnly,
                                               String styleName) {

        Consumer<Boolean> updateConsumer = readOnly ? null : bool ->
            onValueModified.accept(Values.create(bool));

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-BooleanValueEditor");
        IsWidget widget = ValueEditors.createBooleanEditorWidget(styleName, readOnly, currentValue != null && currentValue.getBoolean(), updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        timestamp.ifPresent(time -> addTimestampLabel(time, panel));
        return panel;
    }

    public static IsWidget createObjectEditor(ObjectValue currentValue,
                                              Consumer<ObjectValue> onValueModified,
                                              Optional<Long> timestamp,
                                              boolean readOnly,
                                              String label,
                                              String title,
                                              JsonEditor jsonEditor) {

        Consumer<Value> updateConsumer = readOnly ? null : value ->
            onValueModified.accept(Values.getObject(value).orElse(null));

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-ObjectValueEditor");
        IsWidget widget = createJsonEditorWidget(
            jsonEditor, readOnly, label, title, currentValue, updateConsumer
        );
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        timestamp.ifPresent(time -> addTimestampLabel(time, panel));
        return panel;
    }

    public static IsWidget createArrayEditor(ArrayValue currentValue,
                                             Consumer<ArrayValue> onValueModified,
                                             Optional<Long> timestamp,
                                             boolean readOnly,
                                             String label,
                                             String title,
                                             JsonEditor jsonEditor) {

        Consumer<Value> updateConsumer = readOnly ? null : value ->
            onValueModified.accept(Values.getArray(value).orElse(null));

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-ArrayValueEditor");
        IsWidget widget = createJsonEditorWidget(
            jsonEditor, readOnly, label, title, currentValue, updateConsumer
        );
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        timestamp.ifPresent(time -> addTimestampLabel(time, panel));
        return panel;
    }

    private static void addTimestampLabel(long timestamp, FlowPanel editorPanel) {
        TimestampLabel timestampLabel = new TimestampLabel(timestamp);
        editorPanel.add(timestampLabel);
    }

    private static IsWidget createStringEditorWidget(String styleName,
                                                     boolean readOnly,
                                                     String value,
                                                     Consumer<String> updateConsumer) {
        FormInputText input = new FormInputText();
        input.addStyleName(styleName);
        if (value != null) {
            input.setValue(value);
        } else if (updateConsumer == null) {
            input.setValue("-");
        }
        if (updateConsumer != null && !readOnly) {
            // Both keyup and change (e.g. after pasting) must be used
            input.addKeyUpHandler(event ->
                updateConsumer.accept(input.getValue() == null || input.getValue().length() == 0 ? null : input.getValue()));
            input.addValueChangeHandler(event ->
                updateConsumer.accept(event.getValue() == null || event.getValue().length() == 0 ? null : event.getValue()));
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    private static IsWidget createBooleanEditorWidget(String styleName,
                                                      boolean readOnly,
                                                      Boolean value,
                                                      Consumer<Boolean> updateConsumer) {
        FormCheckBox input = new FormCheckBox();
        input.addStyleName(styleName);
        input.setEnabled(!readOnly);

        if (value == null && updateConsumer == null) {
            return new FormOutputText("-");
        }

        input.setValue(value);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> updateConsumer.accept(input.getValue()));
        } else {
            input.setEnabled(false);
        }
        return input;
    }

    private static IsWidget createJsonEditorWidget(JsonEditor jsonEditor,
                                                   boolean readOnly,
                                                   String label,
                                                   String title,
                                                   Value currentValue,
                                                   Consumer<Value> updateConsumer) {
        jsonEditor.setTitle(title);

        if (currentValue != null) {
            jsonEditor.setValue(currentValue);
        }

        FormButton button = new FormButton();
        button.setIcon("file-text-o");
        button.setText(label);
        button.addClickHandler(event -> jsonEditor.show());

        jsonEditor.setOnReset(() -> jsonEditor.setValue(currentValue));

        if (updateConsumer != null && !readOnly) {
            jsonEditor.setOnApply(updateConsumer);
        }
        return button;
    }
}
