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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class MetaEditor extends AbstractAttributeViewExtension {

    public class MetaItemEditor extends FormGroup {
        protected MetaEditor parentEditor;
        protected MetaItem item;
        protected FormField field;
        protected FormListBox nameList;
        protected FormListBox typeList;
        protected FormInputText nameInput;
        protected IsWidget valueEditor;
        protected boolean newItem;
        protected MetaItemDescriptor currentDescriptor;

        public MetaItemEditor(MetaEditor parentEditor, MetaItem item, boolean newItem) {
            this.parentEditor = parentEditor;
            this.item = item;
            this.newItem = newItem;
            field = new FormField();
            this.setFormField(field);

            // Don't show protocol configuration item in main section
            if (!newItem && item.getName()
                .map(name -> AssetMeta.PROTOCOL_CONFIGURATION.getUrn().equals(name))
                .orElse(false)) {
                setVisible(false);
            }

            nameList = new FormListBox();
            field.add(nameList);

            final int[] currentIndex = {0};
            final int[] selectedIndex = {0};
            final MetaItemDescriptor[] currentMetaItemDescriptor = {null};

            nameList.addItem(environment.getMessages().customItem(), "");

            Arrays.stream(metaItemDescriptors)
                .forEach(metaItemDescriptor -> {
                    String name = getMetaItemDisplayName(environment, metaItemDescriptor.name());
                    nameList.addItem(name, metaItemDescriptor.getUrn());

                    if (item.getName().map(n -> n.equals(metaItemDescriptor.getUrn())).orElse(false)) {
                        currentMetaItemDescriptor[0] = metaItemDescriptor;
                        selectedIndex[0] = currentIndex[0] + 1;
                    }
                    currentIndex[0]++;
                });

            nameList.setSelectedIndex(selectedIndex[0]);
            nameList.addChangeHandler(event -> onNameChanged(true));

            nameInput = new FormInputText();
            field.add(nameInput);
            nameInput.addStyleName(style.metaItemNameEditor());
            nameInput.setPlaceholder(getCustomNameWatermark());
            nameInput.addKeyUpHandler(event -> {
                if (isNullOrEmpty(nameInput.getValue())) {
                    item.clearName();
                } else {
                    item.setName(nameInput.getValue());
                }
                validateItem(this);
            });
            nameInput.addValueChangeHandler(event -> {
                if (isNullOrEmpty(event.getValue())) {
                    item.clearName();
                } else {
                    item.setName(event.getValue());
                }
                validateItem(this);
            });

            typeList = new FormListBox();
            field.add(typeList);

            populateTypeList(this);

            typeList.addChangeHandler(event -> onMetaItemTypeChanged(this, true));

            FormGroupActions formGroupActions = new FormGroupActions();
            FormButton button = new FormButton();
            button.setText(newItem ? getAddButtonLabel() : getDeleteButtonLabel());
            // This has to be after setText otherwise get strange behaviour due to faces of push button
            button.setEnabled(!newItem);
            button.setIcon(newItem ? "plus" : "remove");
            button.addClickHandler(clickEvent -> {
                if (newItem) {
                    if (parentEditor.addItem(item.copy(), false)) {
                        reset();
                    }
                } else {
                    parentEditor.removeItem(this);
                }
            });

            formGroupActions.add(button);
            this.setFromGroupActions(formGroupActions);
            nameInput.setValue(item.getName().orElse(null));
            onNameChanged(false);
        }

        public FormListBox getNameList() {
            return nameList;
        }

        public FormListBox getTypeList() {
            return typeList;
        }

        public FormField getField() {
            return field;
        }

        public MetaItem getItem() {
            return item;
        }

        public boolean isNewItem() {
            return newItem;
        }

        public Optional<MetaItemDescriptor> getCurrentDescriptor() {
            return Optional.ofNullable(currentDescriptor);
        }

        protected void onNameChanged(boolean updateItem) {
            if (updateItem) {
                nameInput.setValue(nameList.getSelectedValue());
            }

            String urn = nameInput.getValue().trim();
            Optional<MetaItemDescriptor> currentMetaItemDescriptor = Arrays.stream(metaItemDescriptors)
                .filter(metaItemDescriptor -> metaItemDescriptor.getUrn().equals(urn))
                .findFirst();

            this.currentDescriptor = currentMetaItemDescriptor.orElse(null);

            String typeListValue = currentMetaItemDescriptor
                .map(MetaItemDescriptor::getValueType)
                .map(Enum::name)
                .orElseGet(() ->
                    item.getValue()
                        .map(Value::getType)
                        .map(Enum::name)
                        .orElse("")
                );

            if (updateItem) {
                item.clearValue();
                if (isNullOrEmpty(urn)) {
                    item.clearName();
                } else {
                    item.setName(urn);
                }
            }

            nameInput.setVisible(!currentMetaItemDescriptor.isPresent());
            typeList.setVisible(!currentMetaItemDescriptor.isPresent());
            typeList.selectItem(typeListValue);
            onMetaItemTypeChanged(this, updateItem);
        }

        public void updateValueEditor() {
            if (valueEditor != null) {
                field.remove(valueEditor);
                valueEditor = null;
            }

            if (typeList.getSelectedIndex() > 0) {
                valueEditor = createItemValueEditor(this);
                if (valueEditor != null) {
                    field.add(valueEditor);
                }
            }
        }

        public void onModified(Value newValue) {
            // Push new value into the meta item
            item.setValue(newValue);
            validateItem(this);
        }

        protected void reset() {
            item.clearName();
            item.clearValue();
            nameList.setSelectedIndex(0);
            nameInput.setValue(null);
            typeList.setSelectedIndex(0);
            if (newItem) {
                formGroupActions.forEach(widget -> {
                    if (widget instanceof FormButton) {
                        ((FormButton) widget).setEnabled(!errorInField);
                    }
                });
            }
            onNameChanged(false);
        }

        @Override
        public void setError(boolean errorInField) {
            super.setError(errorInField);
            if (newItem) {
                formGroupActions.forEach(widget -> {
                    if (widget instanceof FormButton) {
                        ((FormButton) widget).setEnabled(!errorInField);
                    }
                });
            }
        }
    }

    protected static final List<ValueType> valueTypes = Arrays.asList(ValueType.values());
    protected final AssetAttribute attribute;
    final protected FlowPanel itemListPanel = new FlowPanel();
    final protected FlowPanel itemEditorPanel = new FlowPanel();
    final protected FormSectionLabel itemEditorSectionLabel;
    protected MetaItemDescriptor[] metaItemDescriptors;
    protected boolean isProtocolConfiguration;
    protected AttributeValidationResult lastValidationResult;

    public MetaEditor(Environment environment, AttributeView.Style style, String existingItemsHeader, String newItemsHeader, AttributeView parentView, AssetAttribute attribute, Supplier<List<ProtocolDescriptor>> protocolDescriptorSupplier) {
        super(environment, style, parentView, attribute, existingItemsHeader);
        setLabelVisible(false);
        this.attribute = attribute;
        this.style = style;
        isProtocolConfiguration = attribute.hasMetaItem(AssetMeta.PROTOCOL_CONFIGURATION);
        List<MetaItemDescriptor> metaItemDescriptors = new ArrayList<>();

        if (isProtocolConfiguration) {
            protocolDescriptorSupplier.get()
                .stream()
                .filter(protocolDescriptor -> protocolDescriptor
                    .getName()
                    .equals(attribute.getValueAsString().orElse(null)))
                .findFirst()
                .ifPresent(protocolDescriptor -> {
                    if (protocolDescriptor.getProtocolConfigurationMetaItems() != null) {
                        metaItemDescriptors.addAll(protocolDescriptor.getProtocolConfigurationMetaItems());
                    }
                });
        } else {
            if (AgentLink.hasAgentLink(attribute)) {
                protocolDescriptorSupplier.get()
                    .forEach(protocolDescriptor -> {
                        if (protocolDescriptor.getLinkedAttributeMetaItems() != null) {
                            protocolDescriptor.getLinkedAttributeMetaItems().forEach(newDescriptor -> {
                                if (metaItemDescriptors.stream().noneMatch(md -> md.getUrn().equals(newDescriptor.getUrn()))) {
                                    metaItemDescriptors.add(newDescriptor);
                                }
                            });
                        }
                    });
            }
        }

        metaItemDescriptors.addAll(
            Arrays.asList(environment.getSecurityService().isSuperUser() ? AssetMeta.values() : AssetMeta.unRestricted())
        );

        this.metaItemDescriptors = metaItemDescriptors.stream()
            .filter(metaItemDescriptor -> {
                // Always add protocol configuration using protocol from attribute drop down
                return metaItemDescriptor != AssetMeta.PROTOCOL_CONFIGURATION &&
                    // Agent link shouldn't be allowed on protocol configurations
                    (!isProtocolConfiguration || metaItemDescriptor != AssetMeta.AGENT_LINK);
            })
            .sorted(Comparator.comparing(MetaItemDescriptor::name))
            .toArray(MetaItemDescriptor[]::new);

        itemEditorSectionLabel = new FormSectionLabel(newItemsHeader);
        add(itemListPanel);
        add(itemEditorSectionLabel);
        add(itemEditorPanel);
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        buildItemList();
        buildNewItemEditor();
    }

    protected void buildItemList() {
        itemListPanel.clear();

        List<MetaItem> items = attribute.getMeta();
        for (MetaItem item : items) {
            addItem(item, true);
        }
    }

    protected String getCustomNameWatermark() {
        return environment.getMessages().enterCustomAssetAttributeMetaName();
    }

    protected String getTypeWatermark() {
        return environment.getMessages().selectType();
    }

    protected String getAddButtonLabel() {
        return environment.getMessages().addItem();
    }

    protected List<MetaItemEditor> getExistingItemEditors() {
        return IntStream.range(0, itemListPanel.getWidgetCount())
            .mapToObj(i -> (MetaItemEditor)itemListPanel.getWidget(i))
            .collect(Collectors.toList());
    }

    protected String getDeleteButtonLabel() {
        return environment.getMessages().deleteItem();
    }

    protected void populateTypeList(MetaItemEditor itemEditor) {
        itemEditor.typeList.addItem(getTypeWatermark(), "");

        valueTypes.stream()
            .map(Enum::name)
            .map(valueType -> new Pair<>(environment.getMessages().valueTypeDisplayName(valueType), valueType))
            .sorted(Comparator.comparing(typeEntry -> typeEntry.key))
            .forEach(typeEntry -> itemEditor.typeList.addItem(typeEntry.key, typeEntry.value));
    }

    protected IsWidget createItemValueEditor(MetaItemEditor itemEditor) {
        return valueEditorSupplier.createValueEditor(itemEditor.item,
            EnumUtil.enumFromString(ValueType.class, itemEditor.typeList.getSelectedValue()).orElse(ValueType.STRING),
            style,
            parentView,
            itemEditor::onModified);
    }

    protected void onMetaItemTypeChanged(MetaItemEditor itemEditor, boolean updateItem) {
        Optional<AssetMeta> assetMeta = AssetMeta.getAssetMeta(itemEditor.nameList.getSelectedValue());

        if (updateItem) {
            itemEditor.item.clearValue();
            Value initialValue = assetMeta.map(AssetMeta::getInitialValue).orElse(null);
            ValueType valueType = EnumUtil.enumFromString(ValueType.class, itemEditor.typeList.getSelectedValue()).orElse(null);
            if (valueType == ValueType.BOOLEAN && initialValue == null) {
                initialValue = Values.create(false);
            }

            itemEditor.onModified(initialValue);
        }
        itemEditor.updateValueEditor();
    }

    protected void validateItem(MetaItemEditor itemEditor) {
        if (itemEditor.newItem) {
            // Do basic validation on new meta item in situ
            List<ValidationFailure> failures = itemEditor.getItem().getValidationFailures(itemEditor.getCurrentDescriptor());
            updateEditorFailures(itemEditor, failures);
        } else {
            // Presenter will notify us of failures in the context of the whole attribute as well as any asset meta
            // type mismatching etc.
            notifyAttributeModified();
        }
    }

    protected void buildNewItemEditor() {
        itemEditorPanel.clear();
        MetaItem item = new MetaItem();
        MetaItemEditor metaItemEditor = createMetaItemEditor(item, true);
        itemEditorPanel.add(metaItemEditor);
    }

    protected boolean addItem(MetaItem item, boolean viewOnly) {

        if (!viewOnly) {
            MetaItemDescriptor[] descriptor = new MetaItemDescriptor[1];

            // Check item can be added
            boolean canAdd = Arrays.stream(metaItemDescriptors)
                .filter(metaItemDescriptor -> metaItemDescriptor.getUrn().equals(item.getName().orElse(null)))
                .findFirst()
                .map(metaItemDescriptor -> {
                    descriptor[0] = metaItemDescriptor;
                    return metaItemDescriptor.getMaxPerAttribute();
                })
                .map(maxCount -> attribute
                    .getMeta()
                    .stream()
                    .filter(metaItem -> metaItem
                        .getName()
                        .map(name -> name.equals(item.getName().orElse(null)))
                        .orElse(false))
                    .count() < maxCount)
                .orElse(true);

            if (!canAdd) {
                showValidationError(attribute.getName().orElse(""), null, new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_DUPLICATION, getMetaItemDisplayName(environment, descriptor[0].name())));
                return false;
            }

            attribute.getMeta().add(item);

            // Notify the presenter that the attribute has changed
            notifyAttributeModified();
        }

        int index = itemListPanel.getWidgetCount();
        MetaItemEditor metaItemEditor = createMetaItemEditor(item, false);
        itemListPanel.add(metaItemEditor);

        setLabelVisible(itemListPanel.getWidgetCount() > 0);

        // Check if we have a validation failure for this editor
        if (lastValidationResult != null && lastValidationResult.getMetaFailures() != null) {
            List<ValidationFailure> failures = lastValidationResult.getMetaFailures().get(index);
            if (failures != null && !failures.isEmpty()) {
                metaItemEditor.setError(true);
            }
        }
        return true;
    }

    protected MetaItemEditor createMetaItemEditor(MetaItem item, boolean isNewItem) {
        return new MetaItemEditor(this, item, isNewItem);
    }

    protected void removeItem(MetaItemEditor itemEditor) {
        int index = itemListPanel.getWidgetIndex(itemEditor);
        itemListPanel.remove(itemEditor);

        setLabelVisible(itemListPanel.getWidgetCount() > 0);

        attribute.getMeta().remove(index);

        // Notify the presenter that the attribute has changed
        notifyAttributeModified();
    }

    @Override
    public void onValidationStateChange(AttributeValidationResult validationResult) {
        lastValidationResult = validationResult;
        boolean hasMetaFailures = validationResult.getMetaFailures() != null;

        List<MetaItemEditor> editors = getExistingItemEditors();
        IntStream.range(0, editors.size())
            .forEach(i -> {
                MetaItemEditor editor = editors.get(i);
                    List<ValidationFailure> failures = !hasMetaFailures ? null : validationResult.getMetaFailures().get(i);
                    updateEditorFailures(editor, failures);
                }
            );

        // Check for missing meta item messages
        if (validationResult.getMetaFailures() != null && validationResult.getMetaFailures().containsKey(-1)) {
            List<ValidationFailure> failures = validationResult.getMetaFailures().get(-1);
            if (failures != null) {
                failures.forEach(failure ->
                    showValidationError(
                        attribute.getName().orElse(""),
                        null,
                        new ValidationFailure(
                            failure.getReason(),
                            failure.getParameter()
                                .map(parameter ->
                                    // Parameter should be meta item name URN
                                    getMetaItemDescriptor(parameter)
                                        .map(metaItemDescriptor -> getMetaItemDisplayName(environment, metaItemDescriptor.name()))
                                        .orElse(parameter)
                                )
                                .orElse("")
                        )
                    )
                );
            }
        }
    }

    protected void updateEditorFailures(MetaItemEditor metaItemEditor, List<ValidationFailure> failures) {
        boolean hasFailures = failures != null && !failures.isEmpty();
        metaItemEditor.setError(hasFailures);

        if (hasFailures) {
            showMetaItemFailure(metaItemEditor, failures);
        }
    }

    protected void showMetaItemFailure(MetaItemEditor metaItemEditor, List<ValidationFailure> failures) {
        if (failures != null) {
            Optional<MetaItemDescriptor> optionalMetaItemDescriptor = metaItemEditor.getCurrentDescriptor();
            String displayName = optionalMetaItemDescriptor
                .map(metaItemDescriptor -> getMetaItemDisplayName(environment, metaItemDescriptor.name()))
                .orElse(metaItemEditor.getItem().getName().orElse(""));

            failures.forEach(failure -> {
                if (failure.getReason() == MetaItem.MetaItemFailureReason.META_ITEM_VALUE_IS_REQUIRED) {
                    // Substitute in value type info
                    String parameter = EnumUtil.enumFromString(ValueType.class, metaItemEditor.getTypeList().getSelectedValue())
                        .map(Enum::name)
                        .orElse("Value");

                    failure = new ValidationFailure(failure.getReason(), parameter);
                }

                showValidationError(
                    attribute.getName().orElse(""),
                    displayName,
                    failure
                );
            });
        }
    }

    protected Optional<MetaItemDescriptor> getMetaItemDescriptor(String metaItemName) {
        return Arrays.stream(metaItemDescriptors)
            .filter(metaItemDescriptor -> metaItemDescriptor.getUrn().equals(metaItemName))
            .findFirst();
    }

    @Override
    public void onAttributeChanged() {
        onAttach();
    }

    @Override
    public void setBusy(boolean busy) {

    }

    public static String getMetaItemDisplayName(Environment environment, String name) {
        String displayName = environment.getMessages().metaItemDisplayName(name);

        if (isNullOrEmpty(displayName)) {
            displayName = name;
        }
        return displayName;
    }


}


