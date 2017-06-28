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
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Collection of functions and types for {@link Value} editor UI.
 */
public final class ValueEditors {

    public enum ConversionValidationFailure implements ValidationFailure {
        NOT_A_VALID_NUMBER
    }

    /**
     * Maps a {@link FormGroup} which is typically one editor line to an {@link ValueHolder} and
     * encapsulates the raw value that should be set on the value holder and the consumer of validation failure
     * results. If there are no validation failures, the update was successful.
     */
    private static class ValueUpdate<T> {
        final FormGroup formGroup;
        final ValueHolder valueHolder;
        final Consumer<List<ValidationFailure>> resultConsumer;
        final T rawValue;

        public ValueUpdate(FormGroup formGroup, ValueHolder valueHolder, Consumer<List<ValidationFailure>> resultConsumer, T rawValue) {
            this.formGroup = formGroup;
            this.valueHolder = valueHolder;
            this.resultConsumer = resultConsumer;
            this.rawValue = rawValue;
        }
    }

    /**
     * Performs the update and notifies the result consumer of any validation failures.
     */
    private static abstract class ValueUpdater<T> implements Consumer<ValueUpdate<T>> {

        @Override
        public void accept(ValueUpdate<T> valueUpdate) {
            valueUpdate.formGroup.setError(false);
            List<ValidationFailure> failures = new ArrayList<>();
            try {
                valueUpdate.valueHolder.setValue(
                    valueUpdate.rawValue != null ? createValue(valueUpdate.rawValue) : null
                );

                // Let the server set the timestamp by setting it to 0
                if (valueUpdate.valueHolder instanceof AbstractValueTimestampHolder) {
                    AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueUpdate.valueHolder;
                    timestampHolder.setValueTimestamp(0);
                }

                failures.addAll(valueUpdate.valueHolder.getValidationFailures());
            } catch (IllegalArgumentException ex) {
                failures.add(ex::getMessage);
            }
            if (!failures.isEmpty()) {
                valueUpdate.formGroup.setError(true);
            }
            if (valueUpdate.resultConsumer != null) {
                valueUpdate.resultConsumer.accept(failures);
            }
        }

        abstract Value createValue(T rawValue) throws IllegalArgumentException;
    }

    private static ValueUpdater<String> STRING_UPDATER = new ValueUpdater<String>() {
        @Override
        Value createValue(String rawValue) throws IllegalArgumentException {
            return Values.create(rawValue);
        }
    };

    private static ValueUpdater<String> DOUBLE_UPDATER = new ValueUpdater<String>() {
        @Override
        Value createValue(String rawValue) throws IllegalArgumentException {
            try {
                return Values.create(Double.valueOf(rawValue));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(ConversionValidationFailure.NOT_A_VALID_NUMBER.name());
            }
        }
    };

    private static ValueUpdater<Boolean> BOOLEAN_UPDATER = new ValueUpdater<Boolean>() {
        @Override
        Value createValue(Boolean rawValue) throws IllegalArgumentException {
            return Values.create(rawValue);
        }
    };

    private static ValueUpdater<Value> VALUE_UPDATER = new ValueUpdater<Value>() {
        @Override
        Value createValue(Value rawValue) throws IllegalArgumentException {
            return rawValue;
        }
    };

    private static class TimestampLabel extends FlowPanel {

        static protected DateTimeFormat dateFormat = DateTimeFormat.getFormat(Constants.DEFAULT_DATE_FORMAT);
        static protected DateTimeFormat timeFormat = DateTimeFormat.getFormat(Constants.DEFAULT_TIME_FORMAT);

        public TimestampLabel(Long timestamp) {
            addStyleName("layout vertical end or-FormInfoLabel or-ValueTimestamp");
            if (timestamp != null) {
                setTimestamp(timestamp);
            }
        }

        public void setTimestamp(long timestamp) {
            clear();
            if (timestamp > 0) {
                add(new FormOutputText(dateFormat.format(new Date(timestamp))));
                add(new FormOutputText(timeFormat.format(new Date(timestamp))));
            }
        }
    }

    public static IsWidget createStringEditor(ValueHolder valueHolder,
                                              String currentValue,
                                              String defaultValue,
                                              boolean readOnly,
                                              String styleName,
                                              FormGroup formGroup,
                                              boolean showTimestamp,
                                              Consumer<List<ValidationFailure>> validationResultConsumer) {
        Consumer<String> updateConsumer = !readOnly
            ? rawValue -> STRING_UPDATER.accept(new ValueUpdate<>(formGroup, valueHolder, validationResultConsumer, rawValue))
            : null;
        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-StringValueEditor");
        IsWidget widget = ValueEditors.createStringEditorWidget(styleName, currentValue, defaultValue, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp && valueHolder instanceof AbstractValueTimestampHolder) {
            AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueHolder;
            TimestampLabel timestampLabel = new TimestampLabel(timestampHolder.getValueTimestamp().orElse(null));
            panel.add(timestampLabel);
        }
        return () -> panel;
    }


    public static IsWidget createNumberEditor(ValueHolder valueHolder,
                                              String currentValue,
                                              String defaultValue,
                                              boolean readOnly,
                                              String styleName,
                                              FormGroup formGroup,
                                              boolean showTimestamp,
                                              Consumer<List<ValidationFailure>> validationResultConsumer) {

        Consumer<String> updateConsumer = !readOnly
            ? rawValue -> DOUBLE_UPDATER.accept(new ValueUpdate<>(formGroup, valueHolder, validationResultConsumer, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-NumberValueEditor");
        IsWidget widget = ValueEditors.createStringEditorWidget(styleName, currentValue, defaultValue, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp && valueHolder instanceof AbstractValueTimestampHolder) {
            AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueHolder;
            TimestampLabel timestampLabel = new TimestampLabel(timestampHolder.getValueTimestamp().orElse(null));
            panel.add(timestampLabel);
        }
        return () -> panel;
    }

    public static IsWidget createBooleanEditor(ValueHolder valueHolder,
                                               Boolean currentValue,
                                               Boolean defaultValue,
                                               boolean readOnly,
                                               String styleName,
                                               FormGroup formGroup,
                                               boolean showTimestamp,
                                               Consumer<List<ValidationFailure>> validationResultConsumer) {
        Consumer<Boolean> updateConsumer = !readOnly
            ? rawValue -> BOOLEAN_UPDATER.accept(new ValueUpdate<>(formGroup, valueHolder, validationResultConsumer, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-BooleanValueEditor");
        IsWidget widget = ValueEditors.createBooleanEditorWidget(styleName, currentValue, defaultValue, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp && valueHolder instanceof AbstractValueTimestampHolder) {
            AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueHolder;
            TimestampLabel timestampLabel = new TimestampLabel(timestampHolder.getValueTimestamp().orElse(null));
            panel.add(timestampLabel);
        }
        return () -> panel;
    }

    public static IsWidget createObjectEditor(ValueHolder valueHolder,
                                              ObjectValue currentValue,
                                              Supplier<Value> resetSupplier,
                                              boolean readOnly,
                                              String label,
                                              String title,
                                              JsonEditor jsonEditor,
                                              FormGroup formGroup,
                                              boolean showTimestamp,
                                              Consumer<List<ValidationFailure>> validationResultConsumer) {
        Consumer<Value> updateConsumer = !readOnly
            ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(formGroup, valueHolder, validationResultConsumer, rawValue))
            : null;
        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-ObjectValueEditor");
        IsWidget widget = createJsonEditorWidget(
            jsonEditor, label, title, currentValue, updateConsumer, resetSupplier
        );
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp && valueHolder instanceof AbstractValueTimestampHolder) {
            AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueHolder;
            TimestampLabel timestampLabel = new TimestampLabel(timestampHolder.getValueTimestamp().orElse(null));
            panel.add(timestampLabel);
        }
        return () -> panel;
    }

    public static IsWidget createArrayEditor(ValueHolder valueHolder,
                                             ArrayValue currentValue,
                                             Supplier<Value> resetSupplier,
                                             boolean readOnly,
                                             String label,
                                             String title,
                                             JsonEditor jsonEditor,
                                             FormGroup formGroup,
                                             boolean showTimestamp,
                                             Consumer<List<ValidationFailure>> validationResultConsumer) {
        Consumer<Value> updateConsumer = !readOnly
            ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(formGroup, valueHolder, validationResultConsumer, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-ArrayValueEditor");
        IsWidget widget = createJsonEditorWidget(
            jsonEditor, label, title, currentValue, updateConsumer, resetSupplier
        );
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp && valueHolder instanceof AbstractValueTimestampHolder) {
            AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueHolder;
            TimestampLabel timestampLabel = new TimestampLabel(timestampHolder.getValueTimestamp().orElse(null));
            panel.add(timestampLabel);
        }
        return () -> panel;
    }

    public static void showValidationError(Environment environment, String error) {
        environment.getEventBus().dispatch(new ShowFailureEvent(error, 5000));
    }

    public static void showValidationError(Environment environment, String fieldLabel, ValidationFailure validationFailure) {
        StringBuilder error = new StringBuilder();
        if (fieldLabel != null)
            error.append(environment.getMessages().validationFailedFor(fieldLabel));
        else
            error.append(environment.getMessages().validationFailed());
        if (validationFailure != null)
            error.append(": ").append(environment.getMessages().validationFailure(validationFailure.name()));
        showValidationError(environment, error.toString());
    }

    private static IsWidget createStringEditorWidget(String styleName,
                                                     String currentValue,
                                                     String defaultValue,
                                                     Consumer<String> updateConsumer) {
        FormInputText input = new FormInputText();
        input.addStyleName(styleName);
        if (currentValue != null) {
            input.setValue(currentValue);
        } else if (defaultValue != null) {
            input.setValue(defaultValue);
        } else if (updateConsumer == null) {
            input.setValue("-");
        }
        if (updateConsumer != null) {
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
                                                      Boolean currentValue,
                                                      Boolean defaultValue,
                                                      Consumer<Boolean> updateConsumer) {
        FormCheckBox input = new FormCheckBox();
        input.addStyleName(styleName);

        Boolean value = null;
        if (currentValue != null) {
            value = currentValue;
        } else if (defaultValue != null) {
            value = defaultValue;
        } else if (updateConsumer == null) {
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
                                                   String label,
                                                   String title,
                                                   Value currentValue,
                                                   Consumer<Value> updateConsumer,
                                                   Supplier<Value> resetSupplier) {
        jsonEditor.setTitle(title);

        if (currentValue != null) {
            jsonEditor.setValue(currentValue);
        }

        FormButton button = new FormButton();
        button.setIcon("file-text-o");
        button.setText(label);
        button.addClickHandler(event -> jsonEditor.show());

        jsonEditor.setOnReset(() -> jsonEditor.setValue(resetSupplier.get()));

        if (updateConsumer != null) {
            jsonEditor.setOnApply(updateConsumer);
        }
        return button;
    }
}
