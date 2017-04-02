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

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import elemental.json.Json;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.util.JsUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.*;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetAttributes;
import org.openremote.model.asset.AssetMeta;

import java.util.logging.Logger;

public class AttributesEditor
    extends AttributesView<AttributesEditor.Container, AttributesEditor.Style, AssetAttributes, AssetAttribute> {

    private static final Logger LOG = Logger.getLogger(AttributesEditor.class.getName());

    protected final RegExp attributeNameRegExp = RegExp.compile(Attribute.ATTRIBUTE_NAME_PATTERN);

    public interface Container extends AttributesView.Container<AttributesEditor.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    final protected boolean isCreate;
    protected FormGroup newAttributeGroup;

    public AttributesEditor(Environment environment, Container container, AssetAttributes attributes, boolean isCreate) {
        super(environment, container, attributes);
        this.isCreate = isCreate;
    }

    @Override
    protected void createAttributeGroups() {
        newAttributeGroup = createNewAttributeEditor();
        container.getPanel().add(newAttributeGroup);
        super.createAttributeGroups();
    }

    @Override
    protected String getAttributeLabel(AssetAttribute attribute) {
        return attribute.getName();
    }

    @Override
    protected String getAttributeDescription(AssetAttribute attribute) {
        return null;
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        newAttributeGroup.setOpaque(opaque);
    }

    @Override
    protected void addAttributeActions(AssetAttribute attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {

        FormButton deleteButton = new FormButton();
        deleteButton.setText(container.getMessages().deleteAttribute());
        deleteButton.setIcon("remove");
        deleteButton.addClickHandler(clickEvent -> {
            removeAttribute(attribute);
            showInfo(environment.getMessages().attributeDeleted(attribute.getName()));
        });
        formGroupActions.add(deleteButton);
    }

    @Override
    protected void addAttributeExtensions(AssetAttribute attribute, FormGroup formGroup) {
        formGroup.addExtension(new MetaEditor(attribute));
    }

    /* ####################################################################### */

    protected FormGroup createNewAttributeEditor() {
        AssetAttribute attribute = new AssetAttribute("NAME", AttributeType.STRING);

        FormGroup formGroup = createAttributeNameEditor(attribute);

        FormGroupActions formGroupActions = new FormGroupActions();
        FormButton addButton = new FormButton();
        addButton.setText(container.getMessages().addAttribute());
        addButton.setIcon("plus");
        addButton.addClickHandler(clickEvent -> {
            if (attribute.isValid()) {
                formGroup.setError(false);
                // TODO This is necessary because JSON elemental behavior is weird
                AssetAttribute att2 =
                    new AssetAttribute(attribute.getName(), Json.parse(attribute.getJsonObject().toJson()));
                getAttributes().put(att2);
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
        label.addStyleName("larger");
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

        protected IsWidget createEditor(MetaItem item, JsonType valueType, boolean forceEditable, FormGroup formGroup) {
            IsWidget editor;

            AttributesEditor.Style style = container.getStyle();
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
                editor = createStringEditorWidget(style, currentValue, null, updateConsumer);
            } else if (valueType.equals(JsonType.NUMBER)) {
                String currentValue = item.getValueAsString();
                Consumer<String> updateConsumer = isEditable == null || isEditable || forceEditable ? value -> {
                    Double decimalValue = Double.valueOf(value);
                    formGroup.setError(false);
                    item.setValueAsDecimal(decimalValue);
                } : null;
                editor = createDecimalEditorWidget(style, currentValue, null, updateConsumer, errorConsumer);
            } else if (valueType.equals(JsonType.BOOLEAN)) {
                Boolean currentValue = item.getValueAsBoolean();
                Consumer<Boolean> updateConsumer = isEditable == null || isEditable || forceEditable ? value -> {
                    formGroup.setError(false);
                    item.setValueAsBoolean(value);
                } : null;
                editor = createBooleanEditorWidget(style, currentValue, null, updateConsumer);
            } else {
                FormField unsupportedField = new FormField();
                unsupportedField.add(new FormOutputText(
                    environment.getMessages().unsupportedMetaItemType(valueType.name())
                ));
                editor = unsupportedField;
            }
            return editor;
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
            itemNameInput.addValueChangeHandler(event -> item.setName(event.getValue()));
            formField.add(itemNameInput);

            wellknownListBox.addChangeHandler(event -> {
                if (wellknownListBox.getSelectedIndex() > 0) {
                    AssetMeta assetMeta = AssetMeta.editable()[wellknownListBox.getSelectedIndex() - 1];
                    itemNameInput.setText(assetMeta.getName());
                    item.setName(assetMeta.getName());
                    AssetMeta.EditableType editableType = AssetMeta.EditableType.byValueType(assetMeta.getValueType());
                    typeListBox.setSelectedIndex(
                        editableType != null ? editableType.ordinal() + 1 : 0
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
            for (AssetMeta.EditableType metaEditableType : AssetMeta.EditableType.values()) {
                typeListBox.addItem(metaEditableType.label, metaEditableType.name());
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
                ? AssetMeta.EditableType.valueOf(typeListBox.getSelectedValue()).valueType
                : JsonType.STRING;
            if (valueType == JsonType.BOOLEAN) {
                item.setValueUnchecked(Json.create(false)); // Special case boolean editor, has an "initial" state, there is always a value
            }
            IsWidget editor = createEditor(item, valueType, true, formGroup);
            formField.add(editor);
        }
    }
}
