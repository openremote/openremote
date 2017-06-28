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
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.openremote.manager.client.widget.ValueEditors.*;
import static org.openremote.model.attribute.Attribute.ATTRIBUTE_NAME_VALIDATOR;
import static org.openremote.model.attribute.Attribute.isAttributeNameEqualTo;

public class AttributesEditor
    extends AttributesView<AttributesEditor.Container, AttributesEditor.Style> {

    public interface Container extends AttributesView.Container<AttributesEditor.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    final protected boolean isCreate;

    public AttributesEditor(Environment environment, Container container, List<AssetAttribute> attributes, boolean isCreate) {
        super(environment, container, attributes);
        this.isCreate = isCreate;
    }

    protected FormLabel createAttributeLabel(AssetAttribute attribute) {
        FormLabel formLabel = new FormLabel(TextUtil.ellipsize(getAttributeLabel(attribute), 30));
        formLabel.getElement().getStyle().setWidth(20, com.google.gwt.dom.client.Style.Unit.EM);
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
    protected void addAttributeActions(AssetAttribute attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions) {

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

    public boolean addAttribute(String name, AttributeType type) {

        if (getAttributes().stream().anyMatch(isAttributeNameEqualTo(name))) {
            showValidationError(environment, environment.getMessages().duplicateAttributeName());
            return false;
        }

        if (!ATTRIBUTE_NAME_VALIDATOR.test(name)) {
            showValidationError(environment, environment.getMessages().invalidAttributeName());
            return false;
        }

        AssetAttribute attribute = new AssetAttribute();
        attribute.setName(name);
        if (type != null) {
            attribute.setType(type);
        }

        // Tell the server to set the timestamp when saving because we don't want to use browser time
        attribute.setValueTimestamp(0);

        List<ValidationFailure> failures = attribute.getValidationFailures();
        if (!failures.isEmpty()) {
            for (ValidationFailure failure : failures) {
                showValidationError(environment, attribute.getLabelOrName().orElse(null), failure);
            }
            return false;
        }

        attribute.getType().ifPresent(attributeType ->
            attribute.addMeta(attributeType.getDefaultMetaItems())
        );
        getAttributes().add(attribute);

        showInfo(environment.getMessages().attributeAdded(name));

        build();

        return true;
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
                label.getElement().getStyle().setWidth(40, com.google.gwt.dom.client.Style.Unit.EM);
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
                        showValidationError(environment, item.getName().orElse(null), failure);
                    }
                }
            });
            formGroupActions.add(addButton);
            itemValueEditorGroup.addFormGroupActions(formGroupActions);
        }

        protected IsWidget createEditor(MetaItem item, ValueType valueType, boolean isNewMetaItem, FormGroup formGroup) {
            AttributesEditor.Style style = container.getStyle();

            // For some meta items we know if we can edit them or not... custom items are always editable
            boolean isEditable = item.getName().map(AssetMeta::isEditable).orElse(isNewMetaItem);

            Consumer<List<ValidationFailure>> validationResultConsumer =
                failures -> {
                    // If we are not creating a new meta item, validate again in context of attribute
                    if (!isNewMetaItem && failures.isEmpty()) {
                        failures = attribute.getMetaItemValidationFailures(item);
                    }
                    if (!failures.isEmpty()) {
                        formGroup.setError(true);
                        for (ValidationFailure failure : failures) {
                            showValidationError(environment, item.getName().orElse(null), failure);
                        }
                    }
                    // Validation failure of meta item marks the attribute form extension as error state
                    if (!isNewMetaItem) {
                        attributeGroups.get(attribute).setErrorInExtension(!failures.isEmpty());
                    }
                };

            if (valueType.equals(ValueType.STRING)) {
                String currentValue = item.getValue().map(Object::toString).orElse(null);
                return createStringEditor(
                    item, currentValue, null, false, style.stringEditor(), formGroup, false, validationResultConsumer
                );
            } else if (valueType.equals(ValueType.NUMBER)) {
                String currentValue = item.getValue().map(Object::toString).orElse(null);
                return createNumberEditor(
                    item, currentValue, null, false, style.numberEditor(), formGroup, false, validationResultConsumer
                );
            } else if (valueType.equals(ValueType.BOOLEAN)) {
                Boolean currentValue = item.getValueAsBoolean().orElse(null);
                return createBooleanEditor(
                    item, currentValue, null, false, style.booleanEditor(), formGroup, false, validationResultConsumer
                );
            } else if (valueType.equals(ValueType.OBJECT)) {
                ObjectValue currentValue = item.getValueAsObject().orElse(null);
                String label = environment.getMessages().jsonObject();
                String title = environment.getMessages().edit() + " " + environment.getMessages().jsonObject();
                Supplier<Value> resetSupplier = () -> item.getValue().orElse(null);
                return createObjectEditor(
                    attribute, currentValue, resetSupplier, false, label, title, container.getJsonEditor(), formGroup, false, validationResultConsumer
                );
            } else if (valueType.equals(ValueType.ARRAY)) {
                ArrayValue currentValue = item.getValueAsArray().orElse(null);
                String label = environment.getMessages().jsonArray();
                String title = environment.getMessages().edit() + " " + environment.getMessages().jsonArray();
                Supplier<Value> resetSupplier = () -> item.getValue().orElse(null);
                return createArrayEditor(
                    attribute, currentValue, resetSupplier, false, label, title, container.getJsonEditor(), formGroup, false, validationResultConsumer
                );
            } else {
                return new FormOutputText(
                    environment.getMessages().unsupportedMetaItemType(valueType.name())
                );
            }
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

            FormInputText itemNameInput = new FormInputText();
            itemNameInput.addStyleName(container.getStyle().stringEditor());
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
