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
import org.openremote.model.ValueHolder;
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

import static org.openremote.model.attribute.MetaItem.MetaItemFailureReason.META_ITEM_VALUE_IS_REQUIRED;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class MetaEditor extends AbstractAttributeViewExtension {

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
        protected MetaItemDescriptor currentDescriptor;

        public MetaItemEditor(MetaEditor parentEditor, MetaItem item, boolean isNewItem) {
            this.parentEditor = parentEditor;
            this.item = item;
            this.isNewItem = isNewItem;
            field = new FormField();
            this.setFormField(field);

            // Don't show protocol configuration item in main section
            if (!isNewItem && item.getName()
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
            nameInput.setPlaceholder(environment.getMessages().enterCustomAssetAttributeMetaName());
            nameInput.addKeyUpHandler(event -> {
                if (isNullOrEmpty(nameInput.getValue())) {
                    item.clearName();
                } else {
                    item.setName(nameInput.getValue());
                }
                notifyAttributeModified();
                notifyAttributeModified();
            });
            nameInput.addValueChangeHandler(event -> {
                if (isNullOrEmpty(event.getValue())) {
                    item.clearName();
                } else {
                    item.setName(event.getValue());
                }
                notifyAttributeModified();
            });

            typeList = new FormListBox();
            field.add(typeList);
            typeList.addItem(environment.getMessages().selectType(), "");

            valueTypes.stream()
                .map(Enum::name)
                .map(valueType -> new Pair<>(environment.getMessages().valueTypeDisplayName(valueType), valueType))
                .forEach(typeEntry -> typeList.addItem(typeEntry.key, typeEntry.value));

            typeList.addChangeHandler(event -> onTypeChanged(true));

            formGroupActions = new FormGroupActions();
            FormButton button = new FormButton();
            button.setText(isNewItem ? environment.getMessages().addItem() : environment.getMessages().deleteItem());
            button.setIcon(isNewItem ? "plus" : "remove");
            button.addClickHandler(clickEvent -> {
                if (isNewItem) {
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

        protected IsWidget createItemValueEditor() {
            return valueEditorSupplier.createValueEditor(item,
                valueTypes.get(typeList.getSelectedIndex()-1),
                style,
                this::onModified);
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

            int typeIndex = currentMetaItemDescriptor
                .map(MetaItemDescriptor::getValueType)
                .map(valueType -> valueTypes.indexOf(valueType) + 1)
                .orElseGet(() ->
                    item.getValue()
                        .map(Value::getType)
                        .map(valueType -> valueTypes.indexOf(valueType) + 1)
                        .orElse(1)
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
            typeList.setSelectedIndex(typeIndex);
            onTypeChanged(updateItem);
        }

        protected void onTypeChanged(boolean updateItem) {
            Optional<AssetMeta> assetMeta = AssetMeta.getAssetMeta(nameList.getSelectedValue());

            if (updateItem) {
                item.clearValue();
                Value initialValue = assetMeta.map(AssetMeta::getInitialValue).orElse(null);
                ValueType valueType = EnumUtil.enumFromString(ValueType.class, typeList.getSelectedValue()).orElse(null);
                if (valueType == ValueType.BOOLEAN && initialValue == null) {
                    initialValue = Values.create(false);
                }

                item.setValue(initialValue);
                onModified();
            }
            updateValueEditor();
        }

        protected void updateValueEditor() {
            if (valueEditor != null) {
                field.remove(valueEditor);
            }

            if (typeList.getSelectedIndex() > 0) {
                valueEditor = createItemValueEditor();
                field.add(valueEditor);
            }
        }

        protected void onModified() {
            if (isNewItem) {
                // Do basic validation on new meta item in situ
                List<ValidationFailure> failures = item.getValidationFailures();
                boolean hasFailures = failures != null && !failures.isEmpty();

                if (!hasFailures && currentDescriptor != null) {
                    ValidationFailure failure = item.getValue()
                        .flatMap(currentDescriptor::validateValue)
                        .orElse(null);
                    if (failure != null) {
                        failures = Collections.singletonList(failure);
                    }
                }

                formGroupActions.forEach(widget -> {
                    if (widget instanceof FormButton) {
                        ((FormButton) widget).setEnabled(!hasFailures);
                    }
                });
                updateEditorFailures(this, failures);
            } else {
                // Presenter will notify us of failures in the context of the whole attribute as well as any asset meta
                // type mismatching etc.
                notifyAttributeModified();
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

    protected static final List<ValueType> valueTypes = Arrays.asList(ValueType.values());
    protected final AssetAttribute attribute;
    final protected FlowPanel itemListPanel = new FlowPanel();
    final protected FlowPanel itemEditorPanel = new FlowPanel();
    final protected FormSectionLabel itemEditorSectionLabel;
    protected MetaItemDescriptor[] metaItemDescriptors;
    protected boolean isProtocolConfiguration;
    protected AttributeValidationResult lastValidationResult;

    public MetaEditor(Environment environment, AttributeView.Style style, AttributeView parentView, AssetAttribute attribute, Supplier<List<ProtocolDescriptor>> protocolDescriptorSupplier) {
        super(environment, style, parentView, attribute, environment.getMessages().metaItems());
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

        itemEditorSectionLabel = new FormSectionLabel(environment.getMessages().newItem());
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

    protected void buildNewItemEditor() {
        itemEditorPanel.clear();
        MetaItem item = new MetaItem();
        MetaItemEditor metaItemEditor = new MetaItemEditor(this, item, true);
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
        MetaItemEditor metaItemEditor = new MetaItemEditor(this, item, false);
        itemListPanel.add(metaItemEditor);

        // Check if we have a validation failure for this editor
        if (lastValidationResult != null && lastValidationResult.getMetaFailures() != null) {
            List<ValidationFailure> failures = lastValidationResult.getMetaFailures().get(index);
            if (failures != null && !failures.isEmpty()) {
                metaItemEditor.setError(true);
            }
        }
        return true;
    }

    protected void removeItem(MetaItemEditor itemEditor) {
        int index = itemListPanel.getWidgetIndex(itemEditor);
        itemListPanel.remove(itemEditor);
        attribute.getMeta().remove(index);

        // Notify the presenter that the attribute has changed
        notifyAttributeModified();
    }

    @Override
    public void onValidationStateChange(AttributeValidationResult validationResult) {
        lastValidationResult = validationResult;
        boolean hasMetaFailures = validationResult.getMetaFailures() != null;

        for (int i = 0; i < itemListPanel.getWidgetCount(); i++) {
            MetaItemEditor editor = (MetaItemEditor) itemListPanel.getWidget(i);
            List<ValidationFailure> failures = !hasMetaFailures ? null : validationResult.getMetaFailures().get(i);
            updateEditorFailures(editor, failures);
        }

        if (hasMetaFailures) {
            if (itemListPanel.getWidgetCount() == 0) {
                // Editor hasn't been attached to the view yet but still show toast messages
                validationResult.getMetaFailures().forEach((index, failures) ->
                    showMetaItemFailure(attribute.getMeta().get(index), Optional.empty(), failures)
                );
            } else if (validationResult.getMetaFailures().containsKey(-1)) {
                // Check for missing meta item messages
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
    }

    protected void updateEditorFailures(MetaItemEditor editor, List<ValidationFailure> failures) {
        boolean hasFailures = failures != null && !failures.isEmpty();
        editor.setError(hasFailures);

        if (hasFailures) {
            Optional<ValueType> valueType = EnumUtil.enumFromString(ValueType.class, editor.typeList.getSelectedValue());
            showMetaItemFailure(editor.item, valueType, failures);
        }
    }

    protected void showMetaItemFailure(MetaItem metaItem, Optional<ValueType> valueType, List<ValidationFailure> failures) {
        Optional<MetaItemDescriptor> optionalMetaItemDescriptor = metaItem.getName()
            .flatMap(this::getMetaItemDescriptor);

        String displayName = optionalMetaItemDescriptor
            .map(metaItemDescriptor -> getMetaItemDisplayName(environment, metaItemDescriptor.name()))
            .orElse(metaItem.getName().orElse(""));

        if (failures != null) {
            failures.forEach(failure ->
                showValidationError(
                    attribute.getName().orElse(""),
                    displayName,
                    mapValidationFailure(
                        failure,
                        valueType.orElse(optionalMetaItemDescriptor
                            .map(MetaItemDescriptor::getValueType)
                            .orElse(null))
                    )
                )
            );
        }
    }

    protected Optional<MetaItemDescriptor> getMetaItemDescriptor(String metaItemName) {
        return Arrays.stream(metaItemDescriptors)
            .filter(metaItemDescriptor -> metaItemDescriptor.getUrn().equals(metaItemName))
            .findFirst();
    }

    /**
     * This allows descriptor value type to be used to provide more specific meta item value messages
     */
    protected ValidationFailure mapValidationFailure(ValidationFailure failure, ValueType valueType) {
        ValidationFailure validationFailure = failure;
        ValidationFailure.Reason reason = failure.getReason();
        String parameter = failure.getParameter().orElse(null);

        // Convert certain errors to something more meaningful
        if (reason == META_ITEM_VALUE_IS_REQUIRED) {

            switch (valueType) {
                case OBJECT:
                    reason = ValueHolder.ValueFailureReason.VALUE_EXPECTED_OBJECT;
                    break;
                case ARRAY:
                    reason = ValueHolder.ValueFailureReason.VALUE_EXPECTED_ARRAY;
                    break;
                case STRING:
                    reason = ValueHolder.ValueFailureReason.VALUE_EXPECTED_STRING;
                    break;
                case NUMBER:
                    reason = ValueHolder.ValueFailureReason.VALUE_EXPECTED_NUMBER;
                    break;
                case BOOLEAN:
                    reason = ValueHolder.ValueFailureReason.VALUE_EXPECTED_BOOLEAN;
                    break;
            }
            validationFailure = new ValidationFailure(reason, parameter);
        }

        return validationFailure;
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


