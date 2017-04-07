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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import elemental.json.Json;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.AttributeType;
import org.openremote.model.Constants;
import org.openremote.model.Consumer;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAssetAttribute;
import org.openremote.model.asset.AbstractAssetAttributes;
import org.openremote.model.asset.AssetMeta;

import java.util.*;

public abstract class AttributesView<
    C extends AttributesView.Container<S>,
    S extends AttributesView.Style,
    ATTRIBUTES extends AbstractAssetAttributes<?, A>,
    A extends AbstractAssetAttribute
    > {

    public interface Container<S extends AttributesView.Style> {
        S getStyle();

        InsertPanel getPanel();

        ManagerMessages getMessages();
    }

    public interface Style {

        String stringEditor();

        String integerEditor();

        String decimalEditor();

        String booleanEditor();
    }

    public interface AttributeEditor extends IsWidget {
    }

    public static class TimestampLabel extends FormOutputText {

        static protected DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT);

        public TimestampLabel() {
            this(null);
        }

        public TimestampLabel(Long timestamp) {
            addStyleName("flex");
            getElement().getStyle().setFontSize(0.8, com.google.gwt.dom.client.Style.Unit.EM);
            getElement().getStyle().setTextAlign(com.google.gwt.dom.client.Style.TextAlign.RIGHT);
            if (timestamp != null) {
                setTimestamp(timestamp);
            }
        }

        public void setTimestamp(long timestamp) {
            setText(timestamp > 0 ? dateTimeFormat.format(new Date(timestamp)) : "");
        }
    }


    final protected Environment environment;
    final protected C container;
    final protected ATTRIBUTES attributes;
    final protected Map<String, AttributeEditor> editors = new HashMap<>();
    final protected LinkedHashMap<A, FormGroup> attributeGroups = new LinkedHashMap<>();

    public AttributesView(Environment environment, C container, ATTRIBUTES attributes) {
        this.environment = environment;
        this.container = container;
        this.attributes = attributes;
    }

    public ATTRIBUTES getAttributes() {
        return attributes;
    }

    public void clear() {
        while (container.getPanel().getWidgetCount() > 0) {
            container.getPanel().remove(0);
        }
        editors.clear();
        attributeGroups.clear();
    }

    public void build() {
        clear();
        createAttributeGroups();
    }

    public void setOpaque(boolean opaque) {
        for (FormGroup formGroup : attributeGroups.values()) {
            formGroup.setOpaque(opaque);
        }
    }

    public void close() {
        // Subclasses can manage lifecycle
    }

    /* ####################################################################### */

    protected void addAttributeActions(A attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {
    }

    protected void addAttributeExtensions(A attribute,
                                          FormGroup formGroup) {
    }

    /* ####################################################################### */

    protected void createAttributeGroups() {
        for (A attribute : attributes.get()) {
            FormGroup formGroup = createAttributeGroup(attribute);
            if (formGroup != null) {
                attributeGroups.put(attribute, formGroup);
            }
        }
        // Sort form groups by label text ascending
        CollectionsUtil.sortMap(attributeGroups, Comparator.comparing(a -> a.getFormLabel().getText()));

        for (FormGroup attributeGroup : attributeGroups.values()) {
            container.getPanel().add(attributeGroup);
        }
    }

    protected FormGroup createAttributeGroup(A attribute) {

        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = createAttributeLabel(attribute);
        formGroup.addFormLabel(formLabel);

        String description = getAttributeDescription(attribute);
        if (description != null) {
            formGroup.addInfolabel(new Label(description));
        }

        FormGroupActions formGroupActions = new FormGroupActions();

        FormField formField = new FormField();

        AttributeEditor attributeEditor = createEditor(attribute, formGroup);

        addAttributeActions(attribute, formGroup, formField, formGroupActions, attributeEditor);

        if (attributeEditor == null)
            attributeEditor = createUnsupportedEditor(attribute);

        formField.add(attributeEditor);

        formGroup.addFormField(formField);
        formGroup.addFormGroupActions(formGroupActions);

        addAttributeExtensions(attribute, formGroup);

        editors.put(attribute.getName(), attributeEditor);
        return formGroup;
    }

    protected FormLabel createAttributeLabel(A attribute) {
        String label = getAttributeLabel(attribute);
        FormLabel formLabel = new FormLabel(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");
        return formLabel;
    }

    protected String getAttributeLabel(A attribute) {
        String label = attribute.getName();
        MetaItem labelItem = attribute.firstMetaItem(AssetMeta.LABEL);
        if (labelItem != null) {
            label = labelItem.getValueAsString();
        }
        return label;
    }

    protected String getAttributeDescription(A attribute) {
        MetaItem description = attribute.firstMetaItem(AssetMeta.DESCRIPTION);
        if (description != null) {
            return description.getValueAsString();
        }
        return null;
    }

    protected AttributeEditor createEditor(A attribute, FormGroup formGroup) {
        MetaItem defaultValueItem = attribute.firstMetaItem(AssetMeta.DEFAULT);
        S style = container.getStyle();

        AttributeEditor attributeEditor;
        if (attribute.getType().equals(AttributeType.STRING)) {
            attributeEditor = createStringEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attribute.getType().equals(AttributeType.INTEGER)) {
            attributeEditor = createIntegerEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attribute.getType().equals(AttributeType.DECIMAL)) {
            attributeEditor = createDecimalEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
            attributeEditor = createBooleanEditor(attribute, defaultValueItem, style, formGroup);
        } else {
            return null;
        }
        return attributeEditor;
    }

    protected AttributeEditor createUnsupportedEditor(A attribute) {
        FormField unsupportedField = new FormField();
        unsupportedField.add(new FormOutputText(
            environment.getMessages().unsupportedAttributeType(attribute.getType().getValue())
        ));
        return () -> unsupportedField;
    }

    protected AttributeEditor createStringEditor(A attribute, MetaItem defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValueAsString();
        String defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsString() : null;

        Consumer<String> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
            formGroup.setError(false);
            attribute.setValueAsString(value);
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputText inputText = createStringEditorWidget(style, currentValue, defaultValue, updateConsumer);
        panel.add(inputText);
        TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
        panel.add(timestampLabel);

        return () -> panel;
    }

    protected FormInputText createStringEditorWidget(S style,
                                                     String currentValue,
                                                     String defaultValue,
                                                     Consumer<String> updateConsumer) {
        FormInputText input = createFormInputText(style.stringEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else if ((defaultValue) != null) {
            input.setValue(defaultValue);
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(
                event -> updateConsumer.accept(
                    event.getValue() != null && event.getValue().length() > 0
                        ? event.getValue()
                        : null
                )
            );
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected AttributeEditor createIntegerEditor(A attribute, MetaItem defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValueAsString();
        String defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsString() : null;
        Consumer<String> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
            Integer intValue = Integer.valueOf(value);
            formGroup.setError(false);
            attribute.setValueAsInteger(intValue);
        };

        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputNumber inputNumber = createIntegerEditorWidget(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        panel.add(inputNumber);
        TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
        panel.add(timestampLabel);

        return () -> panel;
    }

    protected FormInputNumber createIntegerEditorWidget(S style,
                                                        String currentValue,
                                                        String defaultValue,
                                                        Consumer<String> updateConsumer,
                                                        Consumer<String> errorConsumer) {
        FormInputNumber input = createFormInputNumber(style.integerEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else if (defaultValue != null) {
            input.setValue(defaultValue);
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? event.getValue()
                            : null
                    );
                } catch (NumberFormatException ex) {
                    errorConsumer.accept(environment.getMessages().enterOnlyNumbers());
                }
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected AttributeEditor createDecimalEditor(A attribute, MetaItem defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValueAsString();
        String defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsString() : null;
        Consumer<String> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
            Double decimalValue = Double.valueOf(value);
            formGroup.setError(false);
            attribute.setValueAsDecimal(decimalValue);
        };

        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputText inputText = createDecimalEditorWidget(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        panel.add(inputText);
        TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
        panel.add(timestampLabel);

        return () -> panel;
    }

    protected FormInputText createDecimalEditorWidget(S style,
                                                      String currentValue,
                                                      String defaultValue,
                                                      Consumer<String> updateConsumer,
                                                      Consumer<String> errorConsumer) {
        FormInputText input = createFormInputText(style.decimalEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else if (defaultValue != null) {
            input.setValue(defaultValue);
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? event.getValue()
                            : null
                    );
                } catch (NumberFormatException ex) {
                    errorConsumer.accept(environment.getMessages().enterOnlyDecimals());
                }
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected AttributeEditor createBooleanEditor(A attribute, MetaItem defaultValueItem, S style, FormGroup formGroup) {
        Boolean currentValue = attribute.getValueAsBoolean();
        Boolean defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsBoolean() : null;
        Consumer<Boolean> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
            formGroup.setError(false);
            attribute.setValueUnchecked(Json.create(value));
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormCheckBox checkBox = createBooleanEditorWidget(style, currentValue, defaultValue, updateConsumer);
        panel.add(checkBox);
        TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
        panel.add(timestampLabel);

        return () -> panel;
    }

    protected FormCheckBox createBooleanEditorWidget(S style,
                                                     Boolean currentValue,
                                                     Boolean defaultValue,
                                                     Consumer<Boolean> updateConsumer) {
        FormCheckBox input = createFormInputCheckBox(style.booleanEditor());

        Boolean value = null;
        if (currentValue != null) {
            value = currentValue;
        } else if (defaultValue != null) {
            value = defaultValue;
        }

        input.setValue(value);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> updateConsumer.accept(input.getValue()));
        } else {
            input.setEnabled(false);
        }
        return input;
    }

    protected FormInputText createFormInputText(String formFieldStyleName) {
        FormInputText input = new FormInputText();
        input.addStyleName(formFieldStyleName);
        return input;
    }

    protected FormInputNumber createFormInputNumber(String formFieldStyleName) {
        FormInputNumber input = new FormInputNumber();
        input.addStyleName(formFieldStyleName);
        return input;
    }

    protected FormCheckBox createFormInputCheckBox(String formFieldStyleName) {
        FormCheckBox input = new FormCheckBox();
        input.addStyleName(formFieldStyleName);
        return input;
    }

    protected boolean isDefaultReadOnly(A attribute) {
        return false;
    }

    protected void removeAttribute(A attribute) {
        attributes.remove(attribute.getName());
        editors.remove(attribute.getName());
        int attributeGroupIndex = container.getPanel().getWidgetIndex(attributeGroups.get(attribute));
        container.getPanel().remove(attributeGroupIndex);
        attributeGroups.remove(attribute);
    }

    protected void showInfo(String text) {
        environment.getEventBus().dispatch(new ShowInfoEvent(text));
    }

    protected void showSuccess(String text) {
        environment.getEventBus().dispatch(new ShowSuccessEvent(text));
    }

    protected void showValidationError(String error) {
        environment.getEventBus().dispatch(new ShowFailureEvent(error, 3000));
    }

}
