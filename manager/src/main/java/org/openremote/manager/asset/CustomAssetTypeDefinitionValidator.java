/*
 * Copyright 2026 OpenRemote Inc.
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
package org.openremote.manager.asset;

import org.openremote.model.asset.CustomAssetTypeAttributeDefinition;
import org.openremote.model.asset.CustomAssetTypeDefinition;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CustomAssetTypeDefinitionValidator {

    protected static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^\\w+$");
    protected static final Pattern VALUE_TYPE_PATTERN = Pattern.compile("^\\w+(\\[\\])*$");

    protected static final Set<String> SUPPORTED_VALUE_TYPES = Set.of(
        ValueType.BOOLEAN.getName(),
        ValueType.INTEGER.getName(),
        ValueType.LONG.getName(),
        ValueType.NUMBER.getName(),
        ValueType.TEXT.getName(),
        ValueType.DATE_AND_TIME.getName(),
        ValueType.TIMESTAMP.getName(),
        ValueType.TIMESTAMP_ISO8601.getName(),
        ValueType.GEO_JSON_POINT.getName()
    );

    protected static final Set<String> SUPPORTED_META_ITEMS = Set.of(
        MetaItemType.LABEL.getName(),
        MetaItemType.READ_ONLY.getName(),
        MetaItemType.STORE_DATA_POINTS.getName()
    );

    public void validateForCreate(CustomAssetTypeDefinition definition) {
        validate(definition);
        rejectBuiltInAssetTypeName(definition);
    }

    public void validateForUpdate(CustomAssetTypeDefinition definition) {
        validateForUpdate(definition, null, 0);
    }

    public void validateForUpdate(
        CustomAssetTypeDefinition definition,
        CustomAssetTypeDefinition existingDefinition,
        long usageCount
    ) {
        validate(definition);
        rejectBuiltInAssetTypeName(definition);

        if (existingDefinition != null && usageCount > 0) {
            validateInUseUpdate(existingDefinition, definition);
        }
    }

    protected void validate(CustomAssetTypeDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Custom asset type definition is required");
        }

        validateIdentifier("Asset type name", definition.getName());

        if (isBlank(definition.getDisplayName())) {
            throw new IllegalArgumentException("Asset type display name is required: " + definition.getName());
        }

        Set<String> attributeNames = new HashSet<>();
        for (CustomAssetTypeAttributeDefinition attribute : definition.getAttributes()) {
            validateAttribute(definition, attribute, attributeNames);
        }
    }

    protected void validateAttribute(
        CustomAssetTypeDefinition definition,
        CustomAssetTypeAttributeDefinition attribute,
        Set<String> attributeNames
    ) {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute definition is required: " + definition.getName());
        }

        validateIdentifier("Attribute name", attribute.getName());

        if (!attributeNames.add(attribute.getName())) {
            throw new IllegalArgumentException(
                "Duplicate custom asset type attribute: " + definition.getName() + "." + attribute.getName()
            );
        }

        ValueDescriptor<?> valueDescriptor = validateValueType(definition, attribute);
        validateDefaultValue(definition, attribute, valueDescriptor);
        validateConstraints(definition, attribute, valueDescriptor);
        validateMeta(definition, attribute);
    }

    protected ValueDescriptor<?> validateValueType(
        CustomAssetTypeDefinition definition,
        CustomAssetTypeAttributeDefinition attribute
    ) {
        String valueType = attribute.getType();
        if (isBlank(valueType) || !VALUE_TYPE_PATTERN.matcher(valueType).matches()) {
            throw new IllegalArgumentException("Invalid value type: " + definition.getName() + "." + attribute.getName());
        }

        String baseValueType = baseValueType(valueType);
        if (!SUPPORTED_VALUE_TYPES.contains(baseValueType)) {
            throw new IllegalArgumentException("Unsupported value type: " + valueType);
        }

        ValueDescriptor<?> valueDescriptor = ValueUtil.getValueDescriptor(valueType).orElse(null);
        if (valueDescriptor == null || valueDescriptor.isMetaUseOnly()) {
            throw new IllegalArgumentException("Unknown value type: " + valueType);
        }

        return valueDescriptor;
    }

    protected void validateDefaultValue(
        CustomAssetTypeDefinition definition,
        CustomAssetTypeAttributeDefinition attribute,
        ValueDescriptor<?> valueDescriptor
    ) {
        Object defaultValue = attribute.getDefaultValue();
        if (defaultValue == null) {
            return;
        }

        if (ValueUtil.getValueCoerced(defaultValue, valueDescriptor.getType()).isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid default value for custom asset type attribute: "
                    + definition.getName() + "." + attribute.getName()
            );
        }
    }

    protected void validateConstraints(
        CustomAssetTypeDefinition definition,
        CustomAssetTypeAttributeDefinition attribute,
        ValueDescriptor<?> valueDescriptor
    ) {
        ValueConstraint[] constraints = attribute.getConstraints();
        if (constraints == null) {
            return;
        }

        for (ValueConstraint constraint : constraints) {
            if (constraint == null) {
                throw new IllegalArgumentException(
                    "Null constraint for custom asset type attribute: "
                        + definition.getName() + "." + attribute.getName()
                );
            }
            if (!isConstraintCompatible(constraint, valueDescriptor)) {
                throw new IllegalArgumentException(
                    "Incompatible constraint for custom asset type attribute: "
                        + definition.getName() + "." + attribute.getName()
                );
            }
        }
    }

    protected void validateMeta(CustomAssetTypeDefinition definition, CustomAssetTypeAttributeDefinition attribute) {
        if (attribute.getMeta() == null) {
            return;
        }

        for (MetaItem<?> metaItem : attribute.getMeta().values()) {
            if (metaItem == null || isBlank(metaItem.getName())) {
                throw new IllegalArgumentException(
                    "Invalid meta item for custom asset type attribute: "
                        + definition.getName() + "." + attribute.getName()
                );
            }

            if (!SUPPORTED_META_ITEMS.contains(metaItem.getName())) {
                throw new IllegalArgumentException("Unsupported meta item: " + metaItem.getName());
            }

            MetaItemDescriptor<?> descriptor = ValueUtil.getMetaItemDescriptor(metaItem.getName()).orElse(null);
            if (descriptor == null) {
                throw new IllegalArgumentException("Unknown meta item: " + metaItem.getName());
            }

            if (metaItem.getValue(descriptor.getType().getType()).isEmpty()) {
                throw new IllegalArgumentException("Invalid meta item value: " + metaItem.getName());
            }
        }
    }

    protected void rejectBuiltInAssetTypeName(CustomAssetTypeDefinition definition) {
        if (ValueUtil.getAssetDescriptor(definition.getName()).isPresent()) {
            throw new IllegalArgumentException("Custom asset type name collides with built-in asset type: " + definition.getName());
        }
    }

    protected void validateInUseUpdate(
        CustomAssetTypeDefinition existingDefinition,
        CustomAssetTypeDefinition updatedDefinition
    ) {
        Map<String, CustomAssetTypeAttributeDefinition> existingAttributes = existingDefinition.getAttributes()
            .stream()
            .collect(Collectors.toMap(CustomAssetTypeAttributeDefinition::getName, attribute -> attribute));
        Map<String, CustomAssetTypeAttributeDefinition> updatedAttributes = updatedDefinition.getAttributes()
            .stream()
            .collect(Collectors.toMap(CustomAssetTypeAttributeDefinition::getName, attribute -> attribute));

        for (CustomAssetTypeAttributeDefinition existingAttribute : existingDefinition.getAttributes()) {
            CustomAssetTypeAttributeDefinition updatedAttribute = updatedAttributes.get(existingAttribute.getName());
            if (updatedAttribute == null) {
                throw new IllegalArgumentException(
                    "Cannot remove attribute from in-use custom asset type: "
                        + existingDefinition.getName() + "." + existingAttribute.getName()
                );
            }
            if (!ValueUtil.objectsEqualsWithJSONFallback(existingAttribute, updatedAttribute)) {
                throw new IllegalArgumentException(
                    "Cannot change attribute on in-use custom asset type: "
                        + existingDefinition.getName() + "." + existingAttribute.getName()
                );
            }
        }

        for (CustomAssetTypeAttributeDefinition updatedAttribute : updatedDefinition.getAttributes()) {
            if (!existingAttributes.containsKey(updatedAttribute.getName()) && !updatedAttribute.isOptional()) {
                throw new IllegalArgumentException(
                    "New attributes on in-use custom asset types must be optional: "
                        + updatedDefinition.getName() + "." + updatedAttribute.getName()
                );
            }
        }
    }

    protected void validateIdentifier(String label, String identifier) {
        if (isBlank(identifier) || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(label + " must match " + IDENTIFIER_PATTERN.pattern() + ": " + identifier);
        }
    }

    protected boolean isConstraintCompatible(ValueConstraint constraint, ValueDescriptor<?> valueDescriptor) {
        Class<?> baseType = valueDescriptor.getBaseType();

        if (constraint instanceof ValueConstraint.Pattern) {
            return CharSequence.class.isAssignableFrom(baseType) || baseType.isEnum();
        }
        if (constraint instanceof ValueConstraint.Min || constraint instanceof ValueConstraint.Max) {
            return Number.class.isAssignableFrom(baseType);
        }
        if (constraint instanceof ValueConstraint.Size) {
            return CharSequence.class.isAssignableFrom(baseType)
                || Map.class.isAssignableFrom(baseType)
                || Collection.class.isAssignableFrom(baseType)
                || valueDescriptor.isArray();
        }
        if (
            constraint instanceof ValueConstraint.Past
                || constraint instanceof ValueConstraint.PastOrPresent
                || constraint instanceof ValueConstraint.Future
                || constraint instanceof ValueConstraint.FutureOrPresent
        ) {
            return Date.class.isAssignableFrom(baseType)
                || Instant.class.isAssignableFrom(baseType)
                || Number.class.isAssignableFrom(baseType)
                || CharSequence.class.isAssignableFrom(baseType);
        }
        if (constraint instanceof ValueConstraint.NotBlank) {
            return CharSequence.class.isAssignableFrom(baseType);
        }
        if (constraint instanceof ValueConstraint.NotEmpty) {
            return CharSequence.class.isAssignableFrom(baseType)
                || Map.class.isAssignableFrom(baseType)
                || Collection.class.isAssignableFrom(baseType)
                || valueDescriptor.isArray();
        }

        return true;
    }

    protected String baseValueType(String valueType) {
        while (valueType.endsWith("[]")) {
            valueType = valueType.substring(0, valueType.length() - 2);
        }
        return valueType;
    }

    protected boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
