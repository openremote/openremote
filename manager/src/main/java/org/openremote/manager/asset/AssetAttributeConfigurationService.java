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
import org.openremote.model.asset.AssetAttributeConfigurationGenericParameter;
import org.openremote.model.asset.AssetAttributeConfigurationImportPreview;
import org.openremote.model.asset.AssetAttributeConfigurationTypeMismatch;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.Collection;
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

        Map<String, AssetAttributeConfigurationGenericParameter> genericParameters = applyGenericParameterPaths(
            attributes,
            toGenericParameterPaths(request)
        );

        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("No attribute configuration to export");
        }

        return new AssetAttributeConfigurationDocument(
            AssetAttributeConfigurationDocument.CURRENT_VERSION,
            asset.getType(),
            attributes,
            genericParameters
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

    protected static Set<String> toGenericParameterPaths(AssetAttributeConfigurationExportRequest request) {
        if (request == null || request.getGenericParameterPaths() == null) {
            return Set.of();
        }

        return new LinkedHashSet<>(request.getGenericParameterPaths());
    }

    protected static Map<String, AssetAttributeConfigurationGenericParameter> applyGenericParameterPaths(
        Map<String, AssetAttributeConfigurationEntry> attributes,
        Set<String> genericParameterPaths
    ) {
        Map<String, AssetAttributeConfigurationGenericParameter> genericParameters = new LinkedHashMap<>();
        for (String genericParameterPath : genericParameterPaths) {
            String[] pathParts = validateGenericParameterPath(genericParameterPath);

            Object expectedValue = null;
            boolean expectedValueSet = false;
            List<String> fullPaths = new ArrayList<>();

            for (Map.Entry<String, AssetAttributeConfigurationEntry> attributeEntry : attributes.entrySet()) {
                Optional<Object> pathValue = getMetaPathValue(attributeEntry.getValue().getMeta(), pathParts);
                if (pathValue.isEmpty()) {
                    continue;
                }

                Object value = pathValue.get();
                if (!expectedValueSet) {
                    expectedValue = value;
                    expectedValueSet = true;
                } else if (!ValueUtil.objectsEqualsWithJSONFallback(expectedValue, value)) {
                    throw new IllegalArgumentException("Generic parameter path has different values: " + genericParameterPath);
                }

                fullPaths.add("attributes." + attributeEntry.getKey() + "." + genericParameterPath);
            }

            if (!expectedValueSet) {
                throw new IllegalArgumentException("Generic parameter path not found: " + genericParameterPath);
            }

            for (AssetAttributeConfigurationEntry attributeEntry : attributes.values()) {
                removeMetaPathValue(attributeEntry.getMeta(), pathParts);
            }

            genericParameters.put(
                toGenericParameterName(genericParameterPath),
                new AssetAttributeConfigurationGenericParameter(getGenericParameterType(expectedValue), fullPaths)
            );
        }
        return genericParameters;
    }

    protected static String[] validateGenericParameterPath(String path) {
        if (TextUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Generic parameter path is required");
        }

        String[] pathParts = path.split("\\.");
        if (pathParts.length < 2 || !"meta".equals(pathParts[0])) {
            throw new IllegalArgumentException("Generic parameter path must start with meta: " + path);
        }

        for (String pathPart : pathParts) {
            if (TextUtil.isNullOrEmpty(pathPart)) {
                throw new IllegalArgumentException("Generic parameter path is invalid: " + path);
            }
        }

        return pathParts;
    }

    protected static Optional<Object> getMetaPathValue(MetaMap meta, String[] pathParts) {
        Optional<MetaItem<?>> metaItemOptional = meta.get(pathParts[1]);
        if (metaItemOptional.isEmpty()) {
            return Optional.empty();
        }

        Object value = metaItemOptional.get().getValue(Object.class).orElse(null);
        for (int i = 2; i < pathParts.length; i++) {
            if (!(value instanceof Map<?, ?> valueMap) || !valueMap.containsKey(pathParts[i])) {
                return Optional.empty();
            }
            value = valueMap.get(pathParts[i]);
        }

        return Optional.ofNullable(value);
    }

    @SuppressWarnings("unchecked")
    protected static void removeMetaPathValue(MetaMap meta, String[] pathParts) {
        if (pathParts.length == 2) {
            meta.remove(pathParts[1]);
            return;
        }

        Optional<MetaItem<?>> metaItemOptional = meta.get(pathParts[1]);
        if (metaItemOptional.isEmpty()) {
            return;
        }

        Object value = metaItemOptional.get().getValue(Object.class).orElse(null);
        for (int i = 2; i < pathParts.length - 1; i++) {
            if (!(value instanceof Map<?, ?> valueMap)) {
                return;
            }
            value = valueMap.get(pathParts[i]);
        }

        if (value instanceof Map<?, ?> valueMap) {
            ((Map<String, Object>) valueMap).remove(pathParts[pathParts.length - 1]);
        }
    }

    protected static String toGenericParameterName(String genericParameterPath) {
        String[] pathParts = genericParameterPath.split("\\.");
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < pathParts.length; i++) {
            if (name.isEmpty()) {
                name.append(pathParts[i]);
            } else {
                name.append(Character.toUpperCase(pathParts[i].charAt(0))).append(pathParts[i].substring(1));
            }
        }
        return name.toString();
    }

    protected static String getGenericParameterType(Object value) {
        if (value instanceof CharSequence) {
            return "text";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Map<?, ?>) {
            return "object";
        }
        if (value instanceof Collection<?>) {
            return "array";
        }
        return "unknown";
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

        if (configuration.getGenericParameters() != null && !configuration.getGenericParameters().isEmpty()) {
            throw new IllegalArgumentException("Generic parameters must be resolved before import");
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
