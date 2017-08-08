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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.openremote.manager.client.widget.ValueEditors.*;
import static org.openremote.model.attribute.Attribute.ATTRIBUTE_NAME_VALIDATOR;
import static org.openremote.model.attribute.Attribute.isAttributeNameEqualTo;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AttributesEditor
    extends AttributesView<AttributesEditor.Container, AttributesEditor.Style> {

    public interface Container extends AttributesView.Container<AttributesEditor.Style> {

    }

    public interface Style extends AttributesView.Style {
        String metaItemNameEditor();

        String metaItemValueEditor();
    }

    final protected boolean isCreate;
    protected final BiConsumer<ValueHolder, Consumer<Asset[]>> assetSupplier;
    protected final BiConsumer<Pair<ValueHolder, Asset>, Consumer<AssetAttribute[]>> attributeSupplier;
    protected static final List<ValueType> valueTypes = Arrays.asList(ValueType.values());

    public AttributesEditor(Environment environment, Container container, List<AssetAttribute> attributes, boolean isCreate, BiConsumer<ValueHolder, Consumer<Asset[]>> assetSupplier, BiConsumer<Pair<ValueHolder, Asset>, Consumer<AssetAttribute[]>> attributeSupplier) {
        super(environment, container, attributes);
        this.isCreate = isCreate;
        this.assetSupplier = assetSupplier;
        this.attributeSupplier = attributeSupplier;
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
            buildNewItemEditor();
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
                MetaItemEditor metaItemEditor = new MetaItemEditor(this, item, false);
                itemListPanel.add(metaItemEditor);
            }
        }

        protected void addItem(MetaItem item) {
            attribute.getMeta().add(item);
            MetaItemEditor metaItemEditor = new MetaItemEditor(this, item, false);
            itemListPanel.add(metaItemEditor);
        }

        protected void removeItem(MetaItemEditor itemEditor) {
            int index = itemListPanel.getWidgetIndex(itemEditor);
            itemListPanel.remove(itemEditor);
            attributeGroups.get(attribute).setErrorInExtension(false);
            attribute.getMeta().remove(index);
        }

        protected void buildNewItemEditor() {
            MetaItem item = new MetaItem();
            MetaItemEditor metaItemEditor = new MetaItemEditor(this, item, true);
            itemEditorPanel.add(metaItemEditor);
        }
    }

    public class MetaItemEditor extends FormGroup {
        protected MetaEditor parentEditor;
        protected MetaItem item;
        protected FormField field;
        protected FormListBox nameList;
        protected FormListBox typeList;
        protected FormInputText nameInput;
        protected FormGroupActions formGroupActions;
        protected IsWidget valueEditor;
        protected boolean isNewItem;

        public MetaItemEditor(MetaEditor parentEditor, MetaItem item, boolean isNewItem) {
            this.parentEditor = parentEditor;
            this.item = item;
            this.isNewItem = isNewItem;
            boolean isSuperUser = environment.getSecurityService().isSuperUser();
            Optional<AssetMeta> currentWellknown = item.getName().flatMap(AssetMeta::getAssetMeta);
            field = new FormField();
            this.addFormField(field);

            nameList = new FormListBox();
            field.add(nameList);
            nameList.addItem(environment.getMessages().customItem(), "");
            AssetMeta[] availableMeta = isSuperUser ? AssetMeta.values() : AssetMeta.unRestricted();
            int selectedNameIndex = 0;

            for (int i=0; i<availableMeta.length; i++) {
                AssetMeta wellknown = availableMeta[i];
                nameList.addItem(environment.getMessages().assetMetaDisplayName(wellknown.name()), wellknown.getUrn());
                if (currentWellknown.map(current -> current.getUrn().equals(wellknown.getUrn())).orElse(false)) {
                    selectedNameIndex = i+1;
                }
            }

            nameList.addChangeHandler(event -> {
                onNameChanged(true);
            });

            nameInput = new FormInputText();
            field.add(nameInput);
            nameInput.addStyleName(container.getStyle().metaItemNameEditor());
            nameInput.setPlaceholder(environment.getMessages().enterCustomAssetAttributeMetaName());
            nameInput.addKeyUpHandler(event -> {
                if (isNullOrEmpty(nameInput.getValue())) {
                    item.clearName();
                } else {
                    item.setName(nameInput.getValue());
                }
                onValidation(item.getValidationFailures());
            });
            nameInput.addValueChangeHandler(event -> {
                if (isNullOrEmpty(event.getValue())) {
                    item.clearName();
                } else {
                    item.setName(event.getValue());
                }
                onValidation(item.getValidationFailures());
            });

            typeList = new FormListBox();
            field.add(typeList);
            typeList.addItem(environment.getMessages().selectType(), "");
            int selectedTypeIndex = 0;
            ValueType currentValueType = currentWellknown.map(AssetMeta::getValueType).orElse(item.getValue().map(Value::getType).orElse(null));
            for (int i=0; i<valueTypes.size(); i++) {
                ValueType valueType = valueTypes.get(i);
                typeList.addItem(environment.getMessages().valueTypeDisplayName(valueType.name()), valueType.name());
                if (currentValueType == valueType) {
                    selectedTypeIndex = i+1;
                }
            }

            typeList.addChangeHandler(event -> {
                onTypeChanged(true);
            });

            formGroupActions = new FormGroupActions();
            FormButton button = new FormButton();
            button.setText(isNewItem ? container.getMessages().addItem() : container.getMessages().deleteItem());
            button.setIcon(isNewItem ? "plus" : "remove");
            button.addClickHandler(clickEvent -> {
                if (isNewItem) {
                    // We ask the attribute for validation failures instead of the
                    // item, because the item must be valid in its context
                    List<ValidationFailure> failures = parentEditor.attribute.getMetaItemValidationFailures(item);

                    if (failures.isEmpty()) {
                        parentEditor.addItem(item.copy());
                        this.setError(false);
                        reset();
                    } else {
                        this.setError(true);
                        for (ValidationFailure failure : failures) {
                            showValidationError(environment, item.getName().orElse(null), failure);
                        }
                    }
                } else {
                    parentEditor.removeItem(this);
                }
            });
            formGroupActions.add(button);
            this.addFormGroupActions(formGroupActions);

            nameList.setSelectedIndex(selectedNameIndex);
            nameInput.setValue(item.getName().orElse(null));
            typeList.setSelectedIndex(selectedTypeIndex);
            onNameChanged(false);
        }

        protected IsWidget createItemValueEditor() {
            // Determine editor based on name then value type
            Optional<AssetMeta> assetMeta = AssetMeta.getAssetMeta(item.getName().orElse(null));
            AttributesEditor.Style style = container.getStyle();
            ValueType valueType = valueTypes.get(typeList.getSelectedIndex()-1);

            // Super users can edit any meta items but other users can only edit non-restricted meta items
            // Individual instances of meta items can be set to restricted by a super user.
            boolean isEditable = environment.getSecurityService().isSuperUser() ||
                (item.hasRestrictedFlag() && !item.isRestricted()) ||
                assetMeta.map(AssetMeta::isRestricted).orElse(true);

            switch(valueType) {
                case OBJECT:
                    ObjectValue valueObj = item.getValueAsObject().orElse(null);
                    String label = environment.getMessages().jsonObject();
                    String title = environment.getMessages().edit() + " " + environment.getMessages().jsonObject();
                    return createObjectEditor(
                        item, valueObj, false, label, title, container.getJsonEditor(), this, false, this::onValidation
                    );
                case ARRAY:
                    if (assetMeta.map(am -> am == AssetMeta.AGENT_LINK).orElse(false)) {
                        String assetWatermark = environment.getMessages().selectAgent();
                        String attributeWatermark = environment.getMessages().selectProtocolConfiguration();
                        return createAttributeRefEditor(item, false, this, assetSupplier, attributeSupplier, assetWatermark, attributeWatermark, this::onValidation);
                    }

                    ArrayValue valueArray = item.getValueAsArray().orElse(null);
                    label = environment.getMessages().jsonArray();
                    title = environment.getMessages().edit() + " " + environment.getMessages().jsonArray();
                    return createArrayEditor(
                        item, valueArray, false, label, title, container.getJsonEditor(), this, false, this::onValidation
                    );
                case STRING:
                    String valueStr = item.getValue().map(Object::toString).orElse(null);
                    return createStringEditor(
                        item, valueStr, false, style.stringEditor(), this, false, this::onValidation
                    );
                case NUMBER:
                    valueStr = item.getValue().map(Object::toString).orElse(null);
                    return createNumberEditor(
                        item, valueStr, false, style.numberEditor(), this, false, this::onValidation
                    );
                case BOOLEAN:
                    Boolean valueBool = item.getValueAsBoolean().orElse(null);
                    return createBooleanEditor(
                        item, valueBool, false, style.booleanEditor(), this, false, this::onValidation
                    );
                default:
                    return new FormOutputText(
                        environment.getMessages().unsupportedMetaItemType(valueType.name())
                    );
            }
        }

        protected void onValidation(List<ValidationFailure> failures) {
            // If we are not creating a new meta item, validate again in context of attribute
            if (!isNewItem && failures.isEmpty()) {
                failures = parentEditor.attribute.getMetaItemValidationFailures(item);
            }
            if (!failures.isEmpty()) {
                this.setError(true);
                if (isNewItem) {
                    formGroupActions.forEach(widget -> {
                        if (widget instanceof FormButton) {
                            ((FormButton) widget).setEnabled(false);
                        }
                    });
                }
                for (ValidationFailure failure : failures) {
                    showValidationError(environment, item.getName().orElse(null), failure);
                }
            } else {
                this.setError(false);
                if (isNewItem) {
                    formGroupActions.forEach(widget -> {
                        if (widget instanceof FormButton) {
                            ((FormButton) widget).setEnabled(true);
                        }
                    });
                }
            }
            // Validation failure of meta item marks the attribute form extension as error state
            if (!isNewItem) {
                FormGroup formGroup = attributeGroups.get(parentEditor.attribute);
                if (formGroup != null) {
                    formGroup.setErrorInExtension(!failures.isEmpty());
                }
            }
        }

        protected void onNameChanged(boolean updateItem) {
            if (updateItem && nameList.getSelectedIndex() == 0) {
                nameInput.setValue(null);
            }

            String urn = nameList.getSelectedIndex() > 0 ? nameList.getSelectedValue() : nameInput.getValue();

            if (updateItem) {
                item.clearValue();
                if (isNullOrEmpty(urn)) {
                    item.clearName();
                } else {
                    item.setName(urn);
                }
            }

            nameInput.setValue(urn);

            if (nameList.getSelectedIndex() > 0) {
                nameInput.setVisible(false);
                typeList.setVisible(false);
                @SuppressWarnings("ConstantConditions")
                AssetMeta assetMeta = AssetMeta.getAssetMeta(nameList.getSelectedValue()).get();
                typeList.setSelectedIndex(valueTypes.indexOf(assetMeta.getValueType()) + 1);
            } else {
                nameInput.setVisible(true);
                typeList.setSelectedIndex(
                    item.getValue()
                    .map(Value::getType)
                    .map(valueType -> valueTypes.indexOf(valueType) + 1)
                    .orElse(0)
                );
                typeList.setVisible(true);
            }

            onTypeChanged(updateItem);
        }

        protected void onTypeChanged(boolean updateItem) {
            Optional<AssetMeta> assetMeta = AssetMeta.getAssetMeta(nameList.getSelectedValue());

            if (updateItem) {
                item.clearValue();
                Value initialValue = assetMeta.map(AssetMeta::getInitialValue).orElse(null);
                ValueType valueType = typeList.getSelectedIndex() > 0 ? valueTypes.get(typeList.getSelectedIndex()-1) : null;
                if (valueType == ValueType.BOOLEAN && initialValue == null) {
                    initialValue = Values.create(false);
                }

                item.setValue(initialValue);
                onValidation(item.getValidationFailures());
            }
            updateValueEditor(assetMeta.map(AssetMeta::isValueFixed).orElse(false));
        }

        protected void updateValueEditor(boolean hideEditor) {
            if (valueEditor != null) {
                field.remove(valueEditor);
            }

            if (!hideEditor && typeList.getSelectedIndex() > 0) {
                valueEditor = createItemValueEditor();
                field.add(valueEditor);
            }
        }

        protected void reset() {
            item.clearName();
            item.clearValue();
            nameList.setSelectedIndex(0);
            nameInput.setValue(null);
            typeList.setSelectedIndex(0);
            onNameChanged(false);
        }
    }
}
