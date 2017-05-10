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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.openremote.model.attribute.Attribute.ATTRIBUTE_NAME_VALIDATOR;

public class AttributesEditor
    extends AttributesView<AttributesEditor.Container, AttributesEditor.Style> {

    public interface Container extends AttributesView.Container<AttributesEditor.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    final protected boolean isCreate;
    protected FormGroup newAttributeGroup;

    public AttributesEditor(Environment environment, Container container, List<AssetAttribute> attributes, boolean isCreate) {
        super(environment, container, attributes);
        this.isCreate = isCreate;
    }

    @Override
    protected void createAttributeGroups() {
        newAttributeGroup = createNewAttributeEditor();
        container.getPanel().add(newAttributeGroup);
        super.createAttributeGroups();
    }

    protected FormLabel createAttributeLabel(AssetAttribute attribute) {
        FormLabel formLabel = new FormLabel(TextUtil.ellipsize(getAttributeLabel(attribute), 30));
        formLabel.setIcon("edit");
        formLabel.addStyleName("larger");
        return formLabel;
    }

    @Override
    protected String getAttributeLabel(AssetAttribute attribute) {
        return attribute.getName().orElse("");
    }

    @Override
    protected Optional<String> getAttributeDescription(AssetAttribute attribute) {
        return Optional.empty();
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
            attribute.getName()
                .ifPresent(name ->
                    showInfo(environment.getMessages().attributeDeleted(name))
                );
        });
        formGroupActions.add(deleteButton);
    }

    @Override
    protected void addAttributeExtensions(AssetAttribute attribute, FormGroup formGroup) {
        formGroup.addExtension(new MetaEditor(attribute));
    }

    @Override
    protected boolean isEditorReadOnly(AssetAttribute attribute) {
        return false; // Always allow editing of attribute value
    }

    @Override
    protected boolean isShowTimestamp(AssetAttribute attribute) {
        return false;
    }

    /* ####################################################################### */

    protected FormGroup createNewAttributeEditor() {
        AssetAttribute attribute = new AssetAttribute();

        FormGroup formGroup = createAttributeNameEditor(attribute);

        FormGroupActions formGroupActions = new FormGroupActions();
        FormButton addButton = new FormButton();
        addButton.setText(container.getMessages().addAttribute());
        addButton.setIcon("plus");
        addButton.addClickHandler(clickEvent -> {

            // Must initialize timestamp for proper attribute state (here it
            // means: "When was the initial 'empty' attribute value state created?")
            attribute.setValueTimestamp();

            List<ValidationFailure> failures = attribute.getValidationFailures();
            if (failures.isEmpty()) {
                formGroup.setError(false);
                getAttributes().add(attribute);
                showInfo(environment.getMessages().attributeAdded(attribute.getName().get()));
                build();
            } else {
                formGroup.setError(true);
                for (ValidationFailure failure : failures) {
                    showValidationError(attribute.getLabelOrName().orElse(null), failure);
                }
            }
        });
        formGroupActions.add(addButton);
        formGroup.addFormGroupActions(formGroupActions);

        return formGroup;
    }

    protected FormGroup createAttributeNameEditor(Attribute attribute) {
        FormGroup formGroup = new FormGroup();

        FormLabel label = new FormLabel(environment.getMessages().newAttribute());
        label.setIcon("plus-square");
        label.addStyleName("larger");
        formGroup.addFormLabel(label);

        FormField formField = new FormField();
        formGroup.addFormField(formField);

        FormInputText nameInput = createFormInputText(container.getStyle().stringEditor());
        nameInput.setPlaceholder(environment.getMessages().attributeName());
        nameInput.addValueChangeHandler(event -> {
            if (!ATTRIBUTE_NAME_VALIDATOR.test(event.getValue())) {
                formGroup.setError(true);
                attribute.clearName();
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
            typeListBox.addItem(environment.getMessages().attributeType(attributeType.name()));
        }
        typeListBox.addChangeHandler(event -> {
            if (typeListBox.getSelectedIndex() > 0) {
                attribute.setTypeAndClearValue(
                    AttributeType.values()[typeListBox.getSelectedIndex() - 1]
                );
            } else {
                attribute.clearType();
            }
        });
        formField.add(typeListBox);

        return formGroup;
    }

    /* ####################################################################### */

    public class MetaEditor extends FlowPanel {

        final protected AssetAttribute attribute;
        final protected FormSectionLabel itemListSectionLabel = new FormSectionLabel(environment.getMessages().metaItems());
        final protected FlowPanel itemListPanel = new FlowPanel();
        final protected FormSectionLabel itemEditorSectionLabel = new FormSectionLabel(environment.getMessages().newItem());
        final protected FlowPanel itemEditorPanel = new FlowPanel();

        public MetaEditor(AssetAttribute attribute) {
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

            if (attribute.getMeta().isEmpty()) {
                itemListSectionLabel.setVisible(false);
                return;
            }
            itemListSectionLabel.setVisible(true);

            List<MetaItem> items = attribute.getMeta();
            for (int i = 0; i < items.size(); i++) {
                MetaItem item = items.get(i);
                FormGroup formGroup = new FormGroup();

                FormLabel label = new FormLabel(item.getName().orElse(null));
                label.addStyleName("largest");
                formGroup.addFormLabel(label);

                FormField formField = new FormField();

                // Determine editor based on value type
                if (item.getValue().isPresent()) {
                    ValueType valueType = item.getValue().get().getType();
                    formField.add(createEditor(item, valueType, false, formGroup));
                    formGroup.addFormField(formField);
                } else {
                    formField.add(noValueItemEditor(item));
                    formGroup.addFormField(formField);
                }

                FormGroupActions formGroupActions = new FormGroupActions();

                FormButton deleteButton = new FormButton();
                deleteButton.setText(container.getMessages().deleteItem());
                deleteButton.setIcon("remove");
                int index = i;
                deleteButton.addClickHandler(clickEvent -> {
                    attributeGroups.get(attribute).setErrorInExtension(false);
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

                // We ask the attribute for validation failures instead of the
                // item, because the item must be valid in its context
                List<ValidationFailure> failures = attribute.getMetaItemValidationFailures(item);

                if (failures.isEmpty()) {
                    itemNameEditorGroup.setError(false);
                    itemValueEditorGroup.setError(false);
                    attribute.getMeta().add(item);
                    buildItemList();
                    buildItemEditor();
                } else {
                    itemNameEditorGroup.setError(true);
                    itemValueEditorGroup.setError(true);
                    for (ValidationFailure failure : failures) {
                        showValidationError(item.getName().orElse(null), failure);
                    }
                }
            });
            formGroupActions.add(addButton);
            itemValueEditorGroup.addFormGroupActions(formGroupActions);
        }

        protected IsWidget createEditor(MetaItem item, ValueType valueType, boolean forceEditable, FormGroup formGroup) {
            IsWidget editor;

            AttributesEditor.Style style = container.getStyle();

            // For some meta items we know if we can edit them or not... custom items are always editable
            boolean isEditable = item.getName().map(AssetMeta::isEditable).orElse(forceEditable);

            // Validation failure of meta item marks the attribute form extension as error state
            Consumer<Boolean> resultConsumer = !forceEditable ?
                success -> {
                    // Validate again in context of attribute
                    if (success) {
                        List<ValidationFailure> failures = attribute.getMetaItemValidationFailures(item);
                        if (!failures.isEmpty()) {
                            success = false;
                            formGroup.setError(true);
                            for (ValidationFailure failure : failures) {
                                showValidationError(item.getName().orElse(null), failure);
                            }
                        }
                    }
                    attributeGroups.get(attribute).setErrorInExtension(!success);
                }
                : null;

            if (valueType.equals(ValueType.STRING)) {
                String currentValue = item.getValue().map(Object::toString).orElse(null);
                Consumer<String> updateConsumer = isEditable
                    ? rawValue -> STRING_UPDATER.accept(new ValueUpdate<>(item.getName().orElse(null), formGroup, item, resultConsumer, rawValue))
                    : null;
                editor = createStringEditorWidget(style, currentValue, Optional.empty(), updateConsumer);
            } else if (valueType.equals(ValueType.NUMBER)) {
                String currentValue = item.getValue().map(Object::toString).orElse(null);
                Consumer<String> updateConsumer = isEditable
                    ? rawValue -> DOUBLE_UPDATER.accept(new ValueUpdate<>(item.getName().orElse(null), formGroup, item, resultConsumer, rawValue))
                    : null;
                editor = createDecimalEditorWidget(style, currentValue, Optional.empty(), updateConsumer);
            } else if (valueType.equals(ValueType.BOOLEAN)) {
                Boolean currentValue = item.getValueAsBoolean().orElse(null);
                Consumer<Boolean> updateConsumer = isEditable
                    ? rawValue -> BOOLEAN_UPDATER.accept(new ValueUpdate<>(item.getName().orElse(null), formGroup, item, resultConsumer, rawValue))
                    : null;
                editor = createBooleanEditorWidget(style, currentValue, Optional.empty(), updateConsumer);
            } else if (valueType.equals(ValueType.OBJECT)) {
                ObjectValue currentValue = item.getValueAsObject().orElse(null);
                Consumer<Value> updateConsumer = isEditable
                    ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(item.getName().orElse(null), formGroup, item, resultConsumer, rawValue))
                    : null;
                Supplier<Value> resetSupplier = () -> item.getValue().orElse(null);
                String title = updateConsumer != null
                    ? environment.getMessages().edit() + " " + environment.getMessages().jsonObject()
                    : environment.getMessages().jsonObject();
                editor = createJsonEditorWidget(style, title, currentValue, updateConsumer, resetSupplier);
            } else if (valueType.equals(ValueType.ARRAY)) {
                ArrayValue currentValue = item.getValueAsArray().orElse(null);
                Consumer<Value> updateConsumer = isEditable
                    ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(item.getName().orElse(null), formGroup, item, resultConsumer, rawValue))
                    : null;
                Supplier<Value> resetSupplier = () -> item.getValue().orElse(null);
                String title = updateConsumer != null
                    ? environment.getMessages().edit() + " " + environment.getMessages().jsonArray()
                    : environment.getMessages().jsonArray();
                editor = createJsonEditorWidget(style, title, currentValue, updateConsumer, resetSupplier);
            } else {
                FormField unsupportedField = new FormField();
                unsupportedField.add(new FormOutputText(
                    environment.getMessages().unsupportedMetaItemType(valueType.name())
                ));
                editor = unsupportedField;
            }
            return editor;
        }

        /**
         * This should actually never be called as we don't allow storing of empty meta items. But
         * because we don't have proper database constraints, who knows...
         */
        protected IsWidget noValueItemEditor(MetaItem item) {
            FormField field = new FormField();
            field.add(new FormOutputText(environment.getMessages().emptyMetaItem()));
            return () -> field;
        }

        protected FormGroup createItemNameEditor(MetaItem item, FormListBox typeListBox) {
            FormGroup formGroup = new FormGroup();

            FormLabel label = new FormLabel(environment.getMessages().itemName());
            label.addStyleName("larger");
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
                    itemNameInput.setText(assetMeta.getUrn());
                    item.setName(assetMeta.getUrn());
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
            label.addStyleName("larger");
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
            ValueType valueType = typeListBox.getSelectedIndex() > 0
                ? AssetMeta.EditableType.valueOf(typeListBox.getSelectedValue()).valueType
                : ValueType.STRING;
            if (valueType == ValueType.BOOLEAN) {
                item.setValue(Values.create(false)); // Special case boolean editor, has an "initial" state, there is always a value
            }
            IsWidget editor = createEditor(item, valueType, true, formGroup);
            formField.add(editor);
        }
    }
}
