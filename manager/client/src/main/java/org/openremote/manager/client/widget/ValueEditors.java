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
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * Collection of functions and types for {@link Value} editor UI.
 */
public final class ValueEditors {

    public enum ConversionFailureReason implements ValidationFailure.Reason {
        CONVERSION_NOT_A_VALID_NUMBER
    }

    /**
     * Maps a {@link FormGroup} which is typically one editor line to a {@link ValueHolder} and
     * encapsulates the raw value that should be set on the value holder and the consumer of
     * validation failure results. If there are no validation failures, the update was successful.
     */
    private static class ValueUpdate<T> {
        final ValueHolder valueHolder;
        final Runnable onValueModified;
        final T rawValue;

        public ValueUpdate(ValueHolder valueHolder, Runnable onValueModified, T rawValue) {
            this.valueHolder = valueHolder;
            this.onValueModified = onValueModified;
            this.rawValue = rawValue;
        }
    }

    /**
     * Performs the update and notifies the result consumer of any validation failures.
     */
    private static abstract class ValueUpdater<T> implements Consumer<ValueUpdate<T>> {

        @Override
        public void accept(ValueUpdate<T> valueUpdate) {
            try {
                valueUpdate.valueHolder.setValue(
                    valueUpdate.rawValue != null ? createValue(valueUpdate.rawValue) : null
                );

                // Let the server set the timestamp by setting it to 0
                if (valueUpdate.valueHolder instanceof AbstractValueTimestampHolder) {
                    AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueUpdate.valueHolder;
                    timestampHolder.setValueTimestamp(0);
                }
            } catch (Exception e) {
                // Remove the current value
                valueUpdate.valueHolder.clearValue();
            }
            valueUpdate.onValueModified.run();
        }

        abstract Value createValue(T rawValue) throws IllegalArgumentException;
    }

    public static final String EMPTY_LINE = "----------------------";

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
                throw new IllegalArgumentException(ConversionFailureReason.CONVERSION_NOT_A_VALID_NUMBER.name());
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

    private static ValueUpdater<AttributeRef> ATTRIBUTE_REF_UPDATER = new ValueUpdater<AttributeRef>() {
        @Override
        Value createValue(AttributeRef rawValue) throws IllegalArgumentException {
            return rawValue != null ? rawValue.toArrayValue() : null;
        }
    };

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

    public static IsWidget createStringEditor(ValueHolder valueHolder,
                                              Runnable onValueModified,
                                              boolean readOnly,
                                              boolean showTimestamp,
                                              String currentValue,
                                              String styleName) {
        Consumer<String> updateConsumer = !readOnly
            ? rawValue -> STRING_UPDATER.accept(new ValueUpdate<>(valueHolder, onValueModified, rawValue))
            : null;
        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-StringValueEditor");
        IsWidget widget = ValueEditors.createStringEditorWidget(styleName, readOnly, currentValue, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp)
            addTimestampLabel(valueHolder, panel);
        return () -> panel;
    }


    public static IsWidget createNumberEditor(ValueHolder valueHolder,
                                              Runnable onValueModified,
                                              boolean readOnly,
                                              boolean showTimestamp,
                                              String currentValue,
                                              String styleName) {

        Consumer<String> updateConsumer = !readOnly
            ? rawValue -> DOUBLE_UPDATER.accept(new ValueUpdate<>(valueHolder, onValueModified, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-NumberValueEditor");
        IsWidget widget = createStringEditorWidget(styleName, readOnly, currentValue, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp)
            addTimestampLabel(valueHolder, panel);
        return () -> panel;
    }

    public static IsWidget createBooleanEditor(ValueHolder valueHolder,
                                               Runnable onValueModified,
                                               boolean readOnly,
                                               boolean showTimestamp,
                                               Boolean currentValue,
                                               String styleName) {
        Consumer<Boolean> updateConsumer = !readOnly
            ? rawValue -> BOOLEAN_UPDATER.accept(new ValueUpdate<>(valueHolder, onValueModified, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-BooleanValueEditor");
        IsWidget widget = ValueEditors.createBooleanEditorWidget(styleName, readOnly, currentValue, updateConsumer);
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp)
            addTimestampLabel(valueHolder, panel);
        return () -> panel;
    }

    public static IsWidget createObjectEditor(ValueHolder valueHolder,
                                              Runnable onValueModified,
                                              boolean readOnly,
                                              boolean showTimestamp,
                                              ObjectValue currentValue,
                                              String label,
                                              String title,
                                              JsonEditor jsonEditor) {
        Consumer<Value> updateConsumer = !readOnly
            ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(valueHolder, onValueModified, rawValue))
            : null;
        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-ObjectValueEditor");
        IsWidget widget = createJsonEditorWidget(
            jsonEditor, readOnly, label, title, currentValue, updateConsumer
        );
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp)
            addTimestampLabel(valueHolder, panel);
        return () -> panel;
    }

    public static IsWidget createAttributeRefEditor(ValueHolder valueHolder,
                                                    Runnable onValueModified,
                                                    boolean readOnly,
                                                    BiConsumer<ValueHolder, Consumer<Asset[]>> assetSupplier,
                                                    BiConsumer<Pair<ValueHolder, Asset>, Consumer<AssetAttribute[]>> attributeSupplier,
                                                    String assetWatermark,
                                                    String attributeWatermark,
                                                    String styleName) {

        Consumer<AttributeRef> updateConsumer = !readOnly
            ? rawValue -> ATTRIBUTE_REF_UPDATER.accept(new ValueUpdate<>(valueHolder, onValueModified, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-AttributeRefEditor");
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        panel.add(widgetWrapper);

        List<Asset> assets = new ArrayList<>();
        FormListBox assetList = new FormListBox();
        assetList.setEnabled(!readOnly);
        FormListBox attributeList = new FormListBox();
        attributeList.setEnabled(!readOnly);
        String[] existingValue = valueHolder
            .getValueAsArray()
            .filter(arr -> arr.length() == 2)
            .map(arr -> new String[] {arr.getString(0).orElse(null), arr.getString(1).orElse(null)})
            .orElse(new String[2]);
        Runnable onAssetListChanged = () -> {
            int selectedIndex = assetList.getSelectedIndex();
            attributeList.clear();
            attributeList.addItem(attributeWatermark);
            if (selectedIndex == 0) {
                attributeList.setVisible(false);
                existingValue[0] = null;
                existingValue[1] = null;
            } else {
                if (!assetList.getSelectedValue().equals(existingValue[0])) {
                    existingValue[0] = assetList.getSelectedValue();
                    existingValue[1] = null;
                }

                attributeSupplier.accept(new Pair<>(valueHolder, assets.get(selectedIndex-1)), attributes -> {
                    int attributeSelectedIndex = 0;

                    for (int i=0; i<attributes.length; i++) {
                        AssetAttribute attribute = attributes[i];
                        attributeList.addItem(attribute.getLabelOrName().orElse(""), attribute.getName().orElse(""));
                        if (attribute.getName().orElse("").equals(existingValue[1])) {
                            attributeSelectedIndex = i+1;
                        }
                    }

                    attributeList.setSelectedIndex(attributeSelectedIndex);
                    attributeList.setVisible(true);
                });
            }
            if (updateConsumer != null) {
                updateConsumer.accept(!isNullOrEmpty(existingValue[0]) && !isNullOrEmpty(existingValue[1]) ? new AttributeRef(existingValue[0], existingValue[1]) : null);
            }
        };
        Runnable onAttributeListChanged = () -> {
            int selectedIndex = attributeList.getSelectedIndex();
            if (selectedIndex == 0) {
                existingValue[1] = null;
            } else {
                existingValue[1] = attributeList.getSelectedValue();
            }
            if (updateConsumer != null) {
                updateConsumer.accept(!isNullOrEmpty(existingValue[0]) && !isNullOrEmpty(existingValue[1]) ? new AttributeRef(existingValue[0], existingValue[1]) : null);
            }
        };


        assetList.addItem(assetWatermark);
        attributeList.addItem(attributeWatermark);
        attributeList.setVisible(false);

        assetList.addChangeHandler(event -> onAssetListChanged.run());
        attributeList.addChangeHandler(event -> onAttributeListChanged.run());

        // Populate asset list
        assetSupplier.accept(valueHolder, retrievedAssets -> {
            assets.addAll(Arrays.asList(retrievedAssets));
            int assetSelectedIndex = 0;

            for (int i=0; i<assets.size(); i++) {
                Asset asset = assets.get(i);
                assetList.addItem(asset.getName(), asset.getId());
                if (asset.getId().equals(existingValue[0])) {
                    assetSelectedIndex = i+1;
                }
            }

            assetList.setSelectedIndex(assetSelectedIndex);
            onAssetListChanged.run();
        });

        widgetWrapper.add(assetList);
        widgetWrapper.add(attributeList);

        return () -> panel;
    }

    public static IsWidget createArrayEditor(ValueHolder valueHolder,
                                             Runnable onValueModified,
                                             boolean readOnly,
                                             boolean showTimestamp,
                                             ArrayValue currentValue,
                                             String label,
                                             String title,
                                             JsonEditor jsonEditor) {
        Consumer<Value> updateConsumer = !readOnly
            ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(valueHolder, onValueModified, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center or-ValueEditor or-ArrayValueEditor");
        IsWidget widget = createJsonEditorWidget(
            jsonEditor, readOnly, label, title, currentValue, updateConsumer
        );
        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        widgetWrapper.add(widget);
        panel.add(widgetWrapper);
        if (showTimestamp)
            addTimestampLabel(valueHolder, panel);
        return () -> panel;
    }

    private static void addTimestampLabel(ValueHolder valueHolder, FlowPanel editorPanel) {
        if (valueHolder instanceof AbstractValueTimestampHolder) {
            AbstractValueTimestampHolder timestampHolder = (AbstractValueTimestampHolder) valueHolder;
            TimestampLabel timestampLabel = new TimestampLabel(timestampHolder.getValueTimestamp().orElse(null));
            editorPanel.add(timestampLabel);
        }
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
