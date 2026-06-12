/*
 * Copyright 2026 OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.asset;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttributeConfigurationAssetTypeMismatch;
import org.openremote.model.asset.AssetAttributeConfigurationAttribute;
import org.openremote.model.asset.AssetAttributeConfigurationDocument;
import org.openremote.model.asset.AssetAttributeConfigurationEntry;
import org.openremote.model.asset.AssetAttributeConfigurationExportRequest;
import org.openremote.model.asset.AssetAttributeConfigurationImportPreview;
import org.openremote.model.asset.AssetAttributeConfigurationTypeMismatch;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AssetAttributeConfigurationService {

    protected AssetAttributeConfigurationService() {
    }

    public static AssetAttributeConfigurationDocument exportConfiguration(Asset<?> asset, AssetAttributeConfigurationExportRequest request) {
        if (asset == null) {
            throw new IllegalArgumentException("Target asset is required");
        }

        Set<String> selectedAttributeNames = toSelectedAttributeNames(request);
        Map<String, AssetAttributeConfigurationEntry> attributes = new LinkedHashMap<>();

        asset.getAttributes().values().stream()
            .filter(attribute -> selectedAttributeNames == null || selectedAttributeNames.contains(attribute.getName()))
            .filter(attribute -> !attribute.getMeta().isEmpty())
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .forEach(attribute -> attributes.put(
                attribute.getName(),
                new AssetAttributeConfigurationEntry(getAttributeTypeName(attribute), cloneMeta(attribute.getMeta()))
            ));

        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("No attribute configuration to export");
        }

        return new AssetAttributeConfigurationDocument(
            AssetAttributeConfigurationDocument.CURRENT_VERSION,
            asset.getType(),
            attributes
        );
    }

    public static AssetAttributeConfigurationImportPreview previewImportConfiguration(Asset<?> targetAsset, AssetAttributeConfigurationDocument configuration) {
        validateTargetAsset(targetAsset);
        validateConfiguration(configuration);

        AssetAttributeConfigurationAssetTypeMismatch assetTypeMismatch = null;
        if (!targetAsset.getType().equals(configuration.getAssetType())) {
            assetTypeMismatch = new AssetAttributeConfigurationAssetTypeMismatch(targetAsset.getType(), configuration.getAssetType());
        }

        List<AssetAttributeConfigurationAttribute> importableAttributes = new ArrayList<>();
        List<AssetAttributeConfigurationAttribute> missingAttributes = new ArrayList<>();
        List<AssetAttributeConfigurationTypeMismatch> typeMismatches = new ArrayList<>();
        AttributeMap patchedAttributes = cloneAttributes(targetAsset.getAttributes());

        for (Map.Entry<String, AssetAttributeConfigurationEntry> importedAttribute : configuration.getAttributes().entrySet()) {
            String attributeName = importedAttribute.getKey();
            AssetAttributeConfigurationEntry importedConfiguration = importedAttribute.getValue();

            validateAttributeEntry(attributeName, importedConfiguration);

            Optional<Attribute<Object>> targetAttributeOptional = targetAsset.getAttribute(attributeName);
            if (targetAttributeOptional.isEmpty()) {
                missingAttributes.add(new AssetAttributeConfigurationAttribute(attributeName, importedConfiguration.getType()));
                continue;
            }

            Attribute<?> targetAttribute = targetAttributeOptional.get();
            String targetType = getAttributeTypeName(targetAttribute);
            if (!importedConfiguration.getType().equals(targetType)) {
                typeMismatches.add(new AssetAttributeConfigurationTypeMismatch(attributeName, importedConfiguration.getType(), targetType));
                continue;
            }

            importableAttributes.add(new AssetAttributeConfigurationAttribute(attributeName, importedConfiguration.getType()));
            patchedAttributes.get(attributeName).ifPresent(attribute -> attribute.setMeta(cloneMeta(importedConfiguration.getMeta())));
        }

        if (importableAttributes.isEmpty()) {
            throw new IllegalArgumentException("No importable attributes found");
        }

        return new AssetAttributeConfigurationImportPreview(
            assetTypeMismatch,
            importableAttributes,
            missingAttributes,
            typeMismatches,
            patchedAttributes
        );
    }

    protected static Set<String> toSelectedAttributeNames(AssetAttributeConfigurationExportRequest request) {
        if (request == null || request.getAttributeNames() == null) {
            return null;
        }

        return new LinkedHashSet<>(request.getAttributeNames());
    }

    protected static void validateTargetAsset(Asset<?> targetAsset) {
        if (targetAsset == null) {
            throw new IllegalArgumentException("Target asset is required");
        }

        if (TextUtil.isNullOrEmpty(targetAsset.getType())) {
            throw new IllegalArgumentException("Target asset type is required");
        }
    }

    protected static void validateConfiguration(AssetAttributeConfigurationDocument configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration document is required");
        }

        if (configuration.getVersion() != AssetAttributeConfigurationDocument.CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported configuration version: " + configuration.getVersion());
        }

        if (TextUtil.isNullOrEmpty(configuration.getAssetType())) {
            throw new IllegalArgumentException("Configuration asset type is required");
        }

        if (configuration.getAttributes() == null) {
            throw new IllegalArgumentException("Configuration attributes are required");
        }
    }

    protected static void validateAttributeEntry(String attributeName, AssetAttributeConfigurationEntry attributeConfiguration) {
        if (TextUtil.isNullOrEmpty(attributeName)) {
            throw new IllegalArgumentException("Configuration attribute name is required");
        }

        if (attributeConfiguration == null) {
            throw new IllegalArgumentException("Configuration attribute entry is required: " + attributeName);
        }

        if (TextUtil.isNullOrEmpty(attributeConfiguration.getType())) {
            throw new IllegalArgumentException("Configuration attribute type is required: " + attributeName);
        }

        if (attributeConfiguration.getMeta() == null) {
            throw new IllegalArgumentException("Configuration attribute meta is required: " + attributeName);
        }
    }

    protected static String getAttributeTypeName(Attribute<?> attribute) {
        if (attribute.getType() == null) {
            throw new IllegalArgumentException("Attribute type is required: " + attribute.getName());
        }

        return attribute.getType().getName();
    }

    protected static AttributeMap cloneAttributes(AttributeMap attributes) {
        AttributeMap clonedAttributes = ValueUtil.clone(attributes);
        if (clonedAttributes != null) {
            return clonedAttributes;
        }

        AttributeMap fallback = new AttributeMap();
        for (Attribute<?> attribute : attributes.values()) {
            Attribute<?> clonedAttribute = ValueUtil.clone(attribute);
            if (clonedAttribute == null) {
                throw new IllegalArgumentException("Failed to clone attribute: " + attribute.getName());
            }
            fallback.addOrReplace(clonedAttribute);
        }
        return fallback;
    }

    protected static MetaMap cloneMeta(MetaMap meta) {
        MetaMap clonedMeta = ValueUtil.clone(meta);
        return clonedMeta != null ? clonedMeta : new MetaMap(meta.values());
    }
}
