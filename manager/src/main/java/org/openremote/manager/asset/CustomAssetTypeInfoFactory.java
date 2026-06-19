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

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition;
import org.openremote.model.asset.CustomAssetTypeDefinition;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueFormat;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CustomAssetTypeInfoFactory {

    public AssetTypeInfo toAssetTypeInfo(CustomAssetTypeDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Custom asset type definition is required");
        }

        List<AttributeDescriptor<?>> attributeDescriptors = new ArrayList<>();
        attributeDescriptors.addAll(getBackingAttributeDescriptors());

        definition.getAttributes()
            .stream()
            .sorted(Comparator
                .comparing(
                    CustomAssetTypeAttributeDefinition::getPosition,
                    Comparator.nullsLast(Integer::compareTo)
                )
                .thenComparing(CustomAssetTypeAttributeDefinition::getName))
            .map(this::toAttributeDescriptor)
            .forEach(attributeDescriptors::add);

        return new AssetTypeInfo(
            new AssetDescriptor<>(definition.getName(), definition.getIcon(), definition.getColour()),
            attributeDescriptors.toArray(new AttributeDescriptor<?>[0]),
            new MetaItemDescriptor<?>[0],
            new ValueDescriptor<?>[0]
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected AttributeDescriptor<?> toAttributeDescriptor(CustomAssetTypeAttributeDefinition attribute) {
        ValueDescriptor<?> valueDescriptor = ValueUtil.getValueDescriptor(attribute.getType())
            .orElseThrow(() -> new IllegalArgumentException("Unknown value descriptor: " + attribute.getType()));

        AttributeDescriptor descriptor = new AttributeDescriptor(
            attribute.getName(),
            valueDescriptor,
            attribute.getMeta() != null ? new MetaMap(attribute.getMeta().values()) : null
        );

        descriptor = descriptor.withOptional(attribute.isOptional());

        ValueFormat format = attribute.getFormat();
        if (format != null) {
            descriptor = descriptor.withFormat(format);
        }
        if (attribute.getConstraints() != null) {
            descriptor = descriptor.withConstraints(attribute.getConstraints());
        }
        if (attribute.getUnits() != null) {
            descriptor = descriptor.withUnits(attribute.getUnits());
        }

        return descriptor;
    }

    protected List<AttributeDescriptor<?>> getBackingAttributeDescriptors() {
        List<AttributeDescriptor<?>> attributeDescriptors = new ArrayList<>();
        attributeDescriptors.addAll(getAttributeDescriptorFields(Asset.class));
        attributeDescriptors.addAll(getAttributeDescriptorFields(ThingAsset.class));
        return attributeDescriptors;
    }

    protected List<AttributeDescriptor<?>> getAttributeDescriptorFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field ->
                Modifier.isStatic(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())
                    && AttributeDescriptor.class.isAssignableFrom(field.getType()))
            .<AttributeDescriptor<?>>map(field -> {
                try {
                    return (AttributeDescriptor<?>) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to extract attribute descriptor field: " + field, e);
                }
            })
            .toList();
    }
}
