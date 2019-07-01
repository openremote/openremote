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
package org.openremote.app.client.widget;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.attributes.MetaEditor;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.attribute.*;
import org.openremote.model.interop.Consumer;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AttributeLinkEditor extends FlowPanel {

    public enum FailureReason implements ValidationFailure.Reason {
        CONVERTER_KEY_DUPLICATION,
        CONVERTER_KEY_REQUIRED,
        CONVERTER_VALUE_REQUIRED,
        CONVERTER_VALUE_MISMATCH
    }

    protected final Environment environment;
    protected final AttributeRefEditor attributeRefEditor;
    protected final MetaEditor converterEditor;
    protected final Consumer<AttributeLink> onValueModified;
    protected final AttributeView parentView;
    protected final AttributeView.ValidationErrorConsumer validationErrorConsumer;

    // This is used to map converter values to attribute meta so we can use the meta editor
    protected final AssetAttribute converterAttribute;
    protected AttributeRef attributeRef;

    public AttributeLinkEditor(Environment environment,
                               AttributeView.Style viewStyle,
                               AttributeView parentView,
                               AttributeView.ValueEditorSupplier valueEditorSupplier,
                               AttributeView.ValidationErrorConsumer validationErrorConsumer,
                               AttributeLink currentValue,
                               Consumer<AttributeLink> onValueModified,
                               boolean readOnly,
                               Consumer<Consumer<Map<AttributeRefEditor.AssetInfo, List<AttributeRefEditor.AttributeInfo>>>> assetAttributeSupplier,
                               String assetWatermark,
                               String attributeWatermark,
                               String styleName) {

        this.getElement().getStyle().setWidth(100, Style.Unit.PCT);
        this.environment = environment;
        this.parentView = parentView;
        this.validationErrorConsumer = validationErrorConsumer;
        this.onValueModified = onValueModified;

        // Create the attribute ref editor
        this.attributeRef = currentValue != null ? currentValue.getAttributeRef() : null;
        attributeRefEditor = new AttributeRefEditor(
            this.attributeRef,
            this::onAttributeRefModified,
            readOnly,
            assetAttributeSupplier,
            assetWatermark,
            attributeWatermark,
            styleName);

        add(attributeRefEditor);

        // Create the meta editor (although converter is not meta the editor fits here so convert the converter
        // into compatible data - ideally the meta editor would be more generic but this will do for now)
        converterAttribute = new AssetAttribute(parentView.getAttribute().getName().orElse(""));
        if (currentValue != null) {
            currentValue.getConverter()
                .map(AttributeLinkEditor::convertConverterToMeta)
                .ifPresent(converterAttribute::setMeta);
        }

        converterEditor = new MetaEditor(
            environment,
            viewStyle,
            environment.getMessages().attributeLinkConverterValues(),
            environment.getMessages().attributeLinkNewConverterValue(),
            parentView,
            converterAttribute,
            null
        ) {
            @Override
            protected String getCustomNameWatermark() {
                return environment.getMessages().enterConverterValue();
            }

            @Override
            protected String getTypeWatermark() {
                return environment.getMessages().selectConverter();
            }

            @Override
            protected String getAddButtonLabel() {
                return environment.getMessages().addConverterValue();
            }

            @Override
            protected String getDeleteButtonLabel() {
                return environment.getMessages().deleteConverterValue();
            }

            @Override
            protected void populateTypeList(MetaItemEditor itemEditor) {
                itemEditor.getTypeList().addItem(getTypeWatermark(), "");

                // Include converter enum
                AttributeLink.ConverterType[] converters = AttributeLink.ConverterType.values();
                List<Pair<String,String>> typeValues = new ArrayList<>(valueTypes.size() + converters.length);

                typeValues.addAll(
                    Arrays.stream(converters)
                        .map(converter ->
                            new Pair<>(environment.getMessages().converterType(converter.name()), converter.getValue())
                        )
                        .collect(Collectors.toList())
                );

                typeValues.addAll(
                    valueTypes.stream()
                        .map(Enum::name)
                        .map(valueType -> new Pair<>(environment.getMessages().valueTypeDisplayName(valueType), valueType))
                        .collect(Collectors.toList())
                );

                typeValues.stream()
                    .sorted(Comparator.comparing(typeEntry -> typeEntry.key))
                    .forEach(typeEntry -> itemEditor.getTypeList().addItem(typeEntry.key, typeEntry.value));
            }

            @Override
            protected MetaItemEditor createMetaItemEditor(MetaItem item, boolean isNewItem) {
                MetaItemEditor itemEditor = super.createMetaItemEditor(item, isNewItem);
                if (itemEditor.getNameList() != null) {
                    itemEditor.getNameList().setVisible(false);
                }
                return itemEditor;
            }

            @Override
            protected IsWidget createItemValueEditor(MetaItemEditor itemEditor) {
                // If type is a converter then don't use a value editor
                if (isStandardConverter(itemEditor.getTypeList().getSelectedValue())) {
                    // This is a standard converter and no editor required
                    return null;
                }
                return super.createItemValueEditor(itemEditor);
            }

            @Override
            protected void onMetaItemTypeChanged(MetaItemEditor itemEditor, boolean updateItem) {
                if (updateItem) {
                    Value initialValue = AttributeLink.ConverterType
                        .fromValue(itemEditor.getTypeList().getSelectedValue())
                        .map(converter -> (Value)Values.create(converter.getValue()))
                        .orElseGet(() ->
                            EnumUtil.enumFromString(ValueType.class, itemEditor.getTypeList().getSelectedValue())
                                .map(valueType ->
                                    valueType == ValueType.BOOLEAN ? Values.create(false) : null
                                )
                            .orElse(null)
                        );

                    itemEditor.onModified(initialValue);
                } else {
                    // This is the editor initialising select the correct type for known converter values
                    itemEditor.getItem()
                        .getValueAsString()
                        .flatMap(AttributeLink.ConverterType::fromValue)
                        .ifPresent(converterType -> itemEditor.getTypeList().selectItem(converterType.getValue()));
                }

                itemEditor.updateValueEditor();
            }

            @Override
            protected boolean addItem(MetaItem item, boolean viewOnly) {
                if (viewOnly) {
                    return super.addItem(item, true);
                }

                // Check item with this name doesn't already exist
                boolean canAdd = !converterAttribute.hasMetaItem(item.getName().orElse(null));
                if (!canAdd) {
                    String metaItemName = getMetaItemDisplayName(environment, MetaItemType.ATTRIBUTE_LINK.getUrn());
                    showValidationError(attribute.getName().orElse(""), metaItemName, new ValidationFailure(FailureReason.CONVERTER_KEY_DUPLICATION, item.getName().orElse("")));
                    return false;
                }

                attribute.getMeta().add(item);
                // Notify the presenter that the attribute has changed
                notifyAttributeModified();
                return super.addItem(item, true);
            }

            @Override
            protected void validateItem(MetaItemEditor itemEditor) {
                if (!itemEditor.isNewItem()) {
                    super.validateItem(itemEditor);
                    return;
                }

                // Converter items need a key and a value and key must be unique
                List<ValidationFailure> failures = itemEditor.getItem().getValidationFailures();
                boolean hasFailures = failures != null && !failures.isEmpty();
                if (!hasFailures && converterAttribute.hasMetaItem(itemEditor.getItem().getName().orElse(null))) {
                    failures = Collections.singletonList(new ValidationFailure(FailureReason.CONVERTER_KEY_DUPLICATION, itemEditor.getItem().getName().orElse("")));
                }
                updateEditorFailures(itemEditor, failures);
            }

            protected boolean isStandardConverter(String typeValue) {
                return typeValue != null && typeValue.indexOf('@') == 0;
            }
        };

        converterEditor.setValueEditorSupplier(valueEditorSupplier);
        converterEditor.setEditMode(true);
        converterEditor.setAttributeModifiedCallback(this::onConverterModified);
        converterEditor.setValidationErrorConsumer(this::showConverterEditorValidationError);

        add(converterEditor);
    }

    protected void onAttributeRefModified(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
        updateAttributeLink();
    }

    @SuppressWarnings("unused")
    protected void onConverterModified(AssetAttribute converterAttribute) {
        updateAttributeLink();
    }

    @SuppressWarnings("ParameterCanBeLocal")
    protected void showConverterEditorValidationError(String attributeName, String metaItemName, ValidationFailure validationFailure) {
        // Replace metaItemName with AttributeLink
        metaItemName = environment.getMessages().metaItemDisplayName(MetaItemType.ATTRIBUTE_LINK.getUrn().replace(":", ""));

        // Replace meta item validation failures
        if (validationFailure != null) {
            if (validationFailure.getReason() == MetaItem.MetaItemFailureReason.META_ITEM_NAME_IS_REQUIRED) {
                validationFailure = new ValidationFailure(FailureReason.CONVERTER_KEY_REQUIRED, validationFailure.getParameter().orElse(null));
            } else if (validationFailure.getReason() == MetaItem.MetaItemFailureReason.META_ITEM_VALUE_IS_REQUIRED) {
                validationFailure = new ValidationFailure(FailureReason.CONVERTER_VALUE_REQUIRED, validationFailure.getParameter().orElse(null));
            } else if (validationFailure.getReason() == MetaItem.MetaItemFailureReason.META_ITEM_VALUE_MISMATCH) {
                validationFailure = new ValidationFailure(FailureReason.CONVERTER_VALUE_MISMATCH, validationFailure.getParameter().orElse(null));
            }
        }

        validationErrorConsumer.accept(attributeName, metaItemName, validationFailure);
    }

    protected void updateAttributeLink() {
        // Push attribute link back up the chain

        // Validate attributeRef
        if (attributeRef == null) {
            onValueModified.accept(null);
            return;
        }

        // Validate converter
        // Check all meta items have unique non empty names if not flag as meta item failure
        List<String> converterKeys = new ArrayList<>(converterAttribute.getMeta().size());
        AttributeValidationResult validationResult = new AttributeValidationResult(parentView.getAttribute().getName().orElse(""));

        IntStream.range(0, converterAttribute.getMeta().size())
            .forEach(i -> {
                MetaItem metaItem = converterAttribute.getMeta().get(i);
                List<ValidationFailure> failures = metaItem.getValidationFailures();
                if (failures != null) {
                    failures.forEach(failure -> validationResult.addMetaFailure(i, failure));
                }
                if (converterKeys.contains(metaItem.getName().orElse(""))) {
                    // We have a duplicate converter key name
                    validationResult.addMetaFailure(i, new ValidationFailure(FailureReason.CONVERTER_KEY_DUPLICATION, metaItem.getName().orElse("")));
                } else {
                    converterKeys.add(metaItem.getName().orElse(""));
                }
            });

        // Notify the embedded meta editor
        converterEditor.onValidationStateChange(validationResult);

        // Push AttributeLink update
        if (!validationResult.isValid()) {
            onValueModified.accept(null);
        } else {
            onValueModified.accept(new AttributeLink(attributeRef, convertMetaToConverter(converterAttribute.getMeta())));
        }
    }

    protected static Meta convertConverterToMeta(ObjectValue converter) {
        Meta meta = new Meta();

        converter.stream()
            .forEach(stringValuePair -> meta.add(new MetaItem(stringValuePair.key, stringValuePair.value)));

        return meta;
    }

    protected static ObjectValue convertMetaToConverter(Meta meta) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }

        ObjectValue converter = Values.createObject();
        meta.forEach(metaItem -> converter.put(metaItem.getName().orElse(null), metaItem.getValue().orElse(null)));
        return converter;
    }
}
