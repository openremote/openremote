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
package org.openremote.manager.client.assets.editor;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import elemental.json.Json;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.util.JsUtil;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.*;
import org.openremote.model.Runnable;
import org.openremote.model.asset.AssetMeta;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

public abstract class AttributesEditor<S extends AttributesEditor.Style> {

    private static final Logger LOG = Logger.getLogger(AttributesEditor.class.getName());

    protected final RegExp attributeNameRegExp = RegExp.compile(Attribute.ATTRIBUTE_NAME_PATTERN);

    public interface Container<S extends AttributesEditor.Style> {
        FormView getFormView();

        S getStyle();

        InsertPanel getPanel();

        void showConfirmation(String title, String text, Runnable onConfirm);

        void showConfirmation(String title, String text, Runnable onConfirm, Runnable onCancel);

        ManagerMessages getMessages();
    }

    public interface Style {

        String stringEditor();

        String integerEditor();

        String decimalEditor();

        String booleanEditor();
    }

    final protected Environment environment;
    final protected Container<S> container;
    final protected Attributes attributes;
    final protected boolean isCreate;
    protected FormGroup attributeEditorGroup;
    final protected LinkedHashMap<Attribute, FormGroup> attributeGroups = new LinkedHashMap<>();

    public AttributesEditor(Environment environment, Container<S> container, Attributes attributes, boolean isCreate) {
        this.environment = environment;
        this.container = container;
        this.attributes = attributes;
        this.isCreate = isCreate;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void build() {
        while (container.getPanel().getWidgetCount() > 0) {
            container.getPanel().remove(0);
        }

        attributeEditorGroup = createAttributeEditor();
        container.getPanel().add(attributeEditorGroup);

        attributeGroups.clear();
        for (Attribute attribute : attributes.get()) {
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

    public void setOpaque(boolean opaque) {
        for (FormGroup formGroup : attributeGroups.values()) {
            formGroup.setOpaque(opaque);
        }
        attributeEditorGroup.setOpaque(opaque);
    }

    public void close() {
        // Subclasses can manage lifecycle
    }

    /* ####################################################################### */

    abstract protected void readAttributeValue(Attribute attribute, Runnable onSuccess);

    abstract protected void writeAttributeValue(Attribute attribute);

    /* ####################################################################### */

    protected FormGroup createAttributeEditor() {
        Attribute attribute = new Attribute();

        FormGroup formGroup = createAttributeNameEditor(attribute);

        FormGroupActions formGroupActions = new FormGroupActions();
        FormButton addButton = new FormButton();
        addButton.setText(container.getMessages().addAttribute());
        addButton.setIcon("plus");
        addButton.addClickHandler(clickEvent -> {
            if (attribute.isValid()) {
                formGroup.setError(false);
                // TODO This is necessary because JSON elemental behavior is weird
                Attribute storedAttribute = new Attribute(attribute.getName(), Json.parse(attribute.getJsonObject().toJson()));
                attributes.put(storedAttribute);
                showInfo(environment.getMessages().attributeAdded(attribute.getName()));
                build();
            } else {
                formGroup.setError(true);
                showValidationError(environment.getMessages().enterNameAndSelectType());
            }
        });
        formGroupActions.add(addButton);
        formGroup.addFormGroupActions(formGroupActions);

        return formGroup;
    }

    protected FormGroup createAttributeNameEditor(Attribute attribute) {
        FormGroup formGroup = new FormGroup();

        FormLabel label = new FormLabel(environment.getMessages().newAttribute());
        formGroup.addFormLabel(label);

        FormField formField = new FormField();
        formGroup.addFormField(formField);

        FormInputText nameInput = createFormInputText(container.getStyle().stringEditor());
        nameInput.setPlaceholder(environment.getMessages().attributeName());
        nameInput.addValueChangeHandler(event -> {
            if (!attributeNameRegExp.test(event.getValue())) {
                formGroup.setError(true);
                showValidationError(environment.getMessages().invalidAttributeName());
            } else {
                formGroup.setError(false);
                attribute.setName(event.getValue());
            }
        });
        formField.add(nameInput);

        FormListBox typeListBox = new FormListBox();
        typeListBox.addItem(environment.getMessages().selectType());
        for (AttributeType attributeType : AttributeType.values()) {
            typeListBox.addItem(attributeType.getValue());
        }
        typeListBox.addChangeHandler(event -> {
            AttributeType attributeType =
                typeListBox.getSelectedIndex() > 0
                    ? AttributeType.values()[typeListBox.getSelectedIndex() - 1]
                    : null;
            attribute.setType(attributeType);
        });
        formField.add(typeListBox);

        return formGroup;
    }

    protected FormGroup createAttributeGroup(Attribute attribute) {

        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = createAttributeLabel(attribute);
        formGroup.addFormLabel(formLabel);

        MetaItem description = AssetMeta.getFirst(attribute, AssetMeta.DESCRIPTION);
        if (description != null) {
            formGroup.addInfolabel(new Label(description.getValueAsString()));
        }

        FormGroupActions formGroupActions = new FormGroupActions();

        FormField formField = new FormField();

        IsWidget editor = createEditor(attribute, formGroup);

        // Allow direct read/write of attribute value if asset exists in database and we understand attribute type
        if (!isCreate && editor != null) {
            FormButton writeValueButton = new FormButton();
            writeValueButton.setText(container.getMessages().write());
            writeValueButton.setPrimary(true);
            writeValueButton.setIcon("cloud-upload");
            writeValueButton.addClickHandler(clickEvent -> {
                writeAttributeValue(attribute);
            });
            formGroupActions.add(writeValueButton);

            FormButton readValueButton = new FormButton();
            readValueButton.setText(container.getMessages().read());
            readValueButton.setIcon("cloud-download");
            readValueButton.addClickHandler(clickEvent -> {
                readAttributeValue(attribute, () -> {
                    if (formField.getWidgetCount() > 0) {
                        formField.getWidget(0).removeFromParent();
                    }
                    IsWidget refreshedEditor = createEditor(attribute, formGroup);
                    if (refreshedEditor == null)
                        refreshedEditor = createUnsupportedEditor(attribute);
                    formField.add(refreshedEditor);
                });
            });
            formGroupActions.add(readValueButton);
        }

        FormButton deleteButton = new FormButton();
        deleteButton.setText(container.getMessages().deleteAttribute());
        deleteButton.setIcon("remove");
        deleteButton.addClickHandler(clickEvent -> {
            removeAttribute(attribute);
            showInfo(environment.getMessages().attributeDeleted(attribute.getName()));
        });
        formGroupActions.add(deleteButton);

        if (editor == null)
            editor = createUnsupportedEditor(attribute);
        formField.add(editor);

        formGroup.addFormField(formField);
        formGroup.addFormGroupActions(formGroupActions);
        formGroup.addExtension(createMetaEditor(attribute));

        return formGroup;
    }

    protected FormLabel createAttributeLabel(Attribute attribute) {
        String label = attribute.getName();
        MetaItem labelItem = AssetMeta.getFirst(attribute, AssetMeta.LABEL);
        if (labelItem != null) {
            label = labelItem.getValueAsString();
        }
        FormLabel formLabel = new FormLabel(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");

        return formLabel;
    }

    protected FlowPanel createMetaEditor(Attribute attribute) {
        return new MetaEditor(attribute);
    }

    protected IsWidget createEditor(Attribute attribute, FormGroup formGroup) {
        IsWidget editor;
        S style = container.getStyle();
        MetaItem defaultValueItem = AssetMeta.getFirst(attribute, AssetMeta.DEFAULT);
        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };

        if (attribute.getType().equals(AttributeType.STRING)) {
            String currentValue = attribute.getValueAsString();
            String defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsString() : null;
            Consumer<String> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsString(value);
            };
            editor = createStringEditor(style, currentValue, defaultValue, updateConsumer);
        } else if (attribute.getType().equals(AttributeType.INTEGER)) {
            Integer currentValue = attribute.getValueAsInteger();
            Integer defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsInteger() : null;
            Consumer<Integer> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsInteger(value);
            };
            editor = createIntegerEditor(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        } else if (attribute.getType().equals(AttributeType.DECIMAL)) {
            Double currentValue = attribute.getValueAsDecimal();
            Double defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsDecimal() : null;
            Consumer<Double> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsDecimal(value);
            };
            editor = createDecimalEditor(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
            Boolean currentValue = attribute.getValueAsBoolean();
            Boolean defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsBoolean() : null;
            Consumer<Boolean> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsBoolean(value);
            };
            editor = createBooleanEditor(style, currentValue, defaultValue, updateConsumer);
        } else {
            return null;
        }
        return editor;
    }

    protected IsWidget createUnsupportedEditor(Attribute attribute) {
        FormField unsupportedField = new FormField();
        unsupportedField.add(new FormOutputText(
            environment.getMessages().unsupportedAttributeType(attribute.getType().getValue())
        ));
        return unsupportedField;
    }

    protected IsWidget createEditor(MetaItem item, JsonType valueType, boolean forceEditable, FormGroup formGroup) {
        IsWidget editor;

        S style = container.getStyle();
        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };
        // For some meta items we know if we can edit them or not... custom items are always editable
        Boolean isEditable = AssetMeta.isEditable(item.getName());

        // TODO: We should support more JSON types, and have special editors for well-known meta items
        // Default to STRING editor if value is empty/no type available
        if (valueType == null || valueType.equals(JsonType.STRING)) {
            String currentValue = item.getValueAsString();
            Consumer<String> updateConsumer = isEditable == null || isEditable || forceEditable ? value -> {
                formGroup.setError(false);
                item.setValueAsString(value);
            } : null;
            editor = createStringEditor(style, currentValue, null, updateConsumer);
        } else if (valueType.equals(JsonType.NUMBER)) {
            Double currentValue = item.getValueAsDecimal();
            Consumer<Double> updateConsumer = isEditable == null || isEditable || forceEditable ? value -> {
                formGroup.setError(false);
                item.setValueAsDecimal(value);
            } : null;
            editor = createDecimalEditor(style, currentValue, null, updateConsumer, errorConsumer);
        } else if (valueType.equals(JsonType.BOOLEAN)) {
            Boolean currentValue = item.getValueAsBoolean();
            Consumer<Boolean> updateConsumer = isEditable == null || isEditable || forceEditable ? value -> {
                formGroup.setError(false);
                item.setValueAsBoolean(value);
            } : null;
            editor = createBooleanEditor(style, currentValue, null, updateConsumer);
        } else {
            FormField unsupportedField = new FormField();
            unsupportedField.add(new FormOutputText(
                environment.getMessages().unsupportedMetaItemType(valueType.name())
            ));
            editor = unsupportedField;
        }
        return editor;
    }

    protected FormInputText createStringEditor(S style,
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
                event -> {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? event.getValue()
                            : null
                    );
                }
            );
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormInputNumber createIntegerEditor(S style,
                                                  Integer currentValue,
                                                  Integer defaultValue,
                                                  Consumer<Integer> updateConsumer,
                                                  Consumer<String> errorConsumer) {
        FormInputNumber input = createFormInputNumber(style.integerEditor());

        if (currentValue != null) {
            input.setValue(currentValue.toString());
        } else if (defaultValue != null) {
            input.setValue(defaultValue.toString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? Integer.valueOf(event.getValue())
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

    protected FormInputText createDecimalEditor(S style,
                                                Double currentValue,
                                                Double defaultValue,
                                                Consumer<Double> updateConsumer,
                                                Consumer<String> errorConsumer) {
        FormInputText input = createFormInputText(style.decimalEditor());

        if (currentValue != null) {
            input.setValue(currentValue.toString());
        } else if (defaultValue != null) {
            input.setValue(defaultValue.toString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? Double.valueOf(event.getValue())
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

    protected FormCheckBox createBooleanEditor(S style,
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

    protected boolean isDefaultReadOnly(Attribute attribute) {
        return false;
    }

    protected void removeAttribute(Attribute attribute) {
        attributes.remove(attribute.getName());
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

    /* ####################################################################### */

    public class MetaEditor extends FlowPanel {

        final protected Attribute attribute;
        final protected FormSectionLabel itemListSectionLabel = new FormSectionLabel(environment.getMessages().metaItems());
        final protected FlowPanel itemListPanel = new FlowPanel();
        final protected FormSectionLabel itemEditorSectionLabel = new FormSectionLabel(environment.getMessages().newItem());
        final protected FlowPanel itemEditorPanel = new FlowPanel();

        public MetaEditor(Attribute attribute) {
            this.attribute = attribute;

            add(itemListSectionLabel);
            add(itemListPanel);
            buildItemList();

            add(itemEditorSectionLabel);
            add(itemEditorPanel);
            buildItemEditor();
        }

        protected void buildItemList() {
            itemListPanel.clear();

            if (!attribute.hasMeta() || attribute.getMeta().all().length == 0) {
                itemListSectionLabel.setVisible(false);
                return;
            }
            itemListSectionLabel.setVisible(true);

            MetaItem[] items = attribute.getMeta().all();
            for (int i = 0; i < items.length; i++) {
                MetaItem item = items[i];
                FormGroup formGroup = new FormGroup();

                FormLabel label = new FormLabel(item.getName());
                label.addStyleName("largest");
                formGroup.addFormLabel(label);

                FormField formField = new FormField();

                // Determine editor based on JSON raw value type
                JsonValue value = item.getValue();
                JsonType valueType;
                // TODO https://github.com/gwtproject/gwt/issues/9484
                String jsType = JsUtil.typeOf(value);
                switch (jsType) {
                    case "number":
                        valueType = JsonType.NUMBER;
                        break;
                    case "boolean":
                        valueType = JsonType.BOOLEAN;
                        break;
                    default:
                        valueType = value.getType();
                        break;
                }
                formField.add(createEditor(item, valueType, false, formGroup));
                formGroup.addFormField(formField);

                FormGroupActions formGroupActions = new FormGroupActions();

                FormButton deleteButton = new FormButton();
                deleteButton.setText(container.getMessages().deleteItem());
                deleteButton.setIcon("remove");
                int index = i;
                deleteButton.addClickHandler(clickEvent -> {
                    attribute.getMeta().remove(index);
                    buildItemList();
                });
                formGroupActions.add(deleteButton);

                formGroup.addFormGroupActions(formGroupActions);

                itemListPanel.add(formGroup);
            }
        }

        protected void buildItemEditor() {
            itemEditorPanel.clear();

            MetaItem item = new MetaItem();

            FormListBox valueTypeListBox = new FormListBox();
            FormGroup itemNameEditorGroup = createItemNameEditor(item, valueTypeListBox);
            itemEditorPanel.add(itemNameEditorGroup);

            FormGroup itemValueEditorGroup = createItemValueEditor(item, valueTypeListBox);
            itemEditorPanel.add(itemValueEditorGroup);

            FormGroupActions formGroupActions = new FormGroupActions();
            FormButton addButton = new FormButton();
            addButton.setText(container.getMessages().addItem());
            addButton.setIcon("plus");
            addButton.addClickHandler(clickEvent -> {
                if (item.isValid()) {
                    itemNameEditorGroup.setError(false);
                    itemValueEditorGroup.setError(false);
                    // TODO This is necessary because JSON elemental behavior is weird
                    MetaItem storedItem = new MetaItem(Json.parse(item.getJsonObject().toJson()));
                    if (!attribute.hasMeta()) {
                        attribute.setMeta(new Meta());
                    }
                    attribute.getMeta().add(storedItem);
                    buildItemList();
                    buildItemEditor();
                } else {
                    itemNameEditorGroup.setError(true);
                    itemValueEditorGroup.setError(true);
                    showValidationError(environment.getMessages().enterNameAndValue());
                }
            });
            formGroupActions.add(addButton);
            itemValueEditorGroup.addFormGroupActions(formGroupActions);
        }

        protected FormGroup createItemNameEditor(MetaItem item, FormListBox typeListBox) {
            FormGroup formGroup = new FormGroup();

            FormLabel label = new FormLabel(environment.getMessages().itemName());
            label.addStyleName("largest");
            formGroup.addFormLabel(label);

            FormField formField = new FormField();
            formGroup.addFormField(formField);

            FormListBox wellknownListBox = new FormListBox();
            wellknownListBox.addItem(environment.getMessages().selectItem());
            for (AssetMeta wellknown : AssetMeta.editable()) {
                wellknownListBox.addItem(wellknown.name());
            }
            formField.add(wellknownListBox);

            formField.add(new FormOutputText(environment.getMessages().or()));

            FormInputText itemNameInput = createFormInputText(container.getStyle().stringEditor());
            itemNameInput.setPlaceholder(environment.getMessages().enterCustomAssetAttributeMetaName());
            itemNameInput.addValueChangeHandler(event -> {
                item.setName(event.getValue());
            });
            formField.add(itemNameInput);

            wellknownListBox.addChangeHandler(event -> {
                if (wellknownListBox.getSelectedIndex() > 0) {
                    AssetMeta assetMeta = AssetMeta.editable()[wellknownListBox.getSelectedIndex() - 1];
                    itemNameInput.setText(assetMeta.getName());
                    item.setName(assetMeta.getName());
                    typeListBox.setSelectedIndex(
                        AssetMeta.ValueType.byValueType(assetMeta.getValueType()).ordinal() + 1
                    );
                } else {
                    itemNameInput.setText(null);
                    item.setName(null);
                    typeListBox.setSelectedIndex(0);
                }
                DomEvent.fireNativeEvent(Document.get().createChangeEvent(), typeListBox);
            });

            return formGroup;
        }

        protected FormGroup createItemValueEditor(MetaItem item, FormListBox typeListBox) {
            FormGroup formGroup = new FormGroup();

            FormLabel label = new FormLabel(environment.getMessages().value());
            label.addStyleName("largest");
            formGroup.addFormLabel(label);

            FormField formField = new FormField();
            formGroup.addFormField(formField);

            typeListBox.addItem(environment.getMessages().selectType());
            for (AssetMeta.ValueType metaValueType : AssetMeta.ValueType.values()) {
                typeListBox.addItem(metaValueType.name, metaValueType.name());
            }
            typeListBox.addChangeHandler(event -> resetItemValueEditor(item, formGroup, formField, typeListBox));

            formField.add(typeListBox);

            // Initial state
            resetItemValueEditor(item, formGroup, formField, typeListBox);

            return formGroup;
        }

        protected void resetItemValueEditor(MetaItem item, FormGroup formGroup, FormField formField, FormListBox typeListBox) {
            // Remove the last used editor (last widget in form field)
            if (formField.getWidgetCount() > 1)
                formField.remove(formField.getWidget(formField.getWidgetCount() - 1));

            // Create and add new editor, default to empty STRING
            item.clearValue();
            JsonType valueType = typeListBox.getSelectedIndex() > 0
                ? AssetMeta.ValueType.valueOf(typeListBox.getSelectedValue()).valueType
                : JsonType.STRING;
            if (valueType == JsonType.BOOLEAN) {
                item.setValueAsBoolean(false); // Special case boolean editor, has an "initial" state, there is always a value
            }
            IsWidget editor = createEditor(item, valueType, true, formGroup);
            formField.add(editor);
        }
    }
}
