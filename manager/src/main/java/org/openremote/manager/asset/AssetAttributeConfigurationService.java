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
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.lang.reflect.Field;
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
        return previewImportConfiguration(targetAsset, configuration, null);
    }

    public static AssetAttributeConfigurationImportPreview previewImportConfiguration(Asset<?> targetAsset,
                                                                                     AssetAttributeConfigurationDocument configuration,
                                                                                     Map<String, Object> genericParameterValues) {
        validateTargetAsset(targetAsset);
        validateConfiguration(configuration);

        AssetAttributeConfigurationDocument resolvedConfiguration = resolveGenericParameters(configuration, genericParameterValues);

        AssetAttributeConfigurationAssetTypeMismatch assetTypeMismatch = null;
        if (!targetAsset.getType().equals(resolvedConfiguration.getAssetType())) {
            assetTypeMismatch = new AssetAttributeConfigurationAssetTypeMismatch(targetAsset.getType(), resolvedConfiguration.getAssetType());
        }

        List<AssetAttributeConfigurationAttribute> importableAttributes = new ArrayList<>();
        List<AssetAttributeConfigurationAttribute> missingAttributes = new ArrayList<>();
        List<AssetAttributeConfigurationTypeMismatch> typeMismatches = new ArrayList<>();
        AttributeMap patchedAttributes = cloneAttributes(targetAsset.getAttributes());

        for (Map.Entry<String, AssetAttributeConfigurationEntry> importedAttribute : resolvedConfiguration.getAttributes().entrySet()) {
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
            String genericParameterType = null;
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
                String pathType = getGenericParameterType(attributeEntry.getValue().getMeta(), pathParts);
                if (genericParameterType == null || "unknown".equals(genericParameterType)) {
                    genericParameterType = pathType;
                } else if (!"unknown".equals(pathType) && !genericParameterType.equals(pathType)) {
                    throw new IllegalArgumentException("Generic parameter path has different types: " + genericParameterPath);
                }
            }

            if (!expectedValueSet) {
                throw new IllegalArgumentException("Generic parameter path not found: " + genericParameterPath);
            }

            for (AssetAttributeConfigurationEntry attributeEntry : attributes.values()) {
                removeMetaPathValue(attributeEntry.getMeta(), pathParts);
            }

            genericParameters.put(
                toGenericParameterName(genericParameterPath),
                new AssetAttributeConfigurationGenericParameter(genericParameterType != null ? genericParameterType : "unknown", fullPaths)
            );
        }
        return genericParameters;
    }

    protected static AssetAttributeConfigurationDocument resolveGenericParameters(AssetAttributeConfigurationDocument configuration,
                                                                                 Map<String, Object> genericParameterValues) {
        if (configuration.getGenericParameters() == null || configuration.getGenericParameters().isEmpty()) {
            return configuration;
        }

        Map<String, Object> values = genericParameterValues != null ? genericParameterValues : Map.of();
        Map<String, AssetAttributeConfigurationEntry> resolvedAttributes = cloneConfigurationAttributes(configuration.getAttributes());

        for (Map.Entry<String, AssetAttributeConfigurationGenericParameter> genericParameterEntry : configuration.getGenericParameters().entrySet()) {
            String genericParameterName = genericParameterEntry.getKey();
            AssetAttributeConfigurationGenericParameter genericParameter = genericParameterEntry.getValue();
            validateGenericParameter(genericParameterName, genericParameter);

            if (!values.containsKey(genericParameterName) || values.get(genericParameterName) == null) {
                throw new IllegalArgumentException("Generic parameter value is required: " + genericParameterName);
            }

            Object value = values.get(genericParameterName);
            validateGenericParameterValue(genericParameterName, genericParameter.getType(), value);

            for (String path : genericParameter.getPaths()) {
                setConfigurationPathValue(resolvedAttributes, path, value);
            }
        }

        validateGenericParameterPathTypes(resolvedAttributes, configuration.getGenericParameters());

        return new AssetAttributeConfigurationDocument(
            configuration.getVersion(),
            configuration.getAssetType(),
            resolvedAttributes
        );
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

    protected static String[] validateGenericParameterDocumentPath(String path) {
        if (TextUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Generic parameter document path is required");
        }

        String[] pathParts = path.split("\\.");
        if (pathParts.length < 4 || !"attributes".equals(pathParts[0]) || !"meta".equals(pathParts[2])) {
            throw new IllegalArgumentException("Generic parameter document path is invalid: " + path);
        }

        for (String pathPart : pathParts) {
            if (TextUtil.isNullOrEmpty(pathPart)) {
                throw new IllegalArgumentException("Generic parameter document path is invalid: " + path);
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
            Optional<Object> pathChild = getPathChild(value, pathParts[i]);
            if (pathChild.isEmpty()) {
                return Optional.empty();
            }
            value = pathChild.get();
        }

        return Optional.ofNullable(value);
    }

    protected static Optional<Object> getPathChild(Object value, String pathPart) {
        if (value == null) {
            return Optional.empty();
        }

        Map<String, Object> valueMap;
        try {
            valueMap = toMutableStringObjectMap(value, null);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }

        if (!valueMap.containsKey(pathPart)) {
            return Optional.empty();
        }
        return Optional.ofNullable(valueMap.get(pathPart));
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
        Map<String, Object> valueMap;
        try {
            valueMap = toMutableStringObjectMap(value, String.join(".", pathParts));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (valueMap != value) {
            setMetaItemValue(metaItemOptional.get(), valueMap);
        }
        value = valueMap;

        for (int i = 2; i < pathParts.length - 1; i++) {
            if (!(value instanceof Map<?, ?> currentValueMap)) {
                return;
            }
            Object childValue = currentValueMap.get(pathParts[i]);
            if (childValue == null) {
                return;
            }

            Map<String, Object> childValueMap;
            try {
                childValueMap = toMutableStringObjectMap(childValue, String.join(".", pathParts));
            } catch (IllegalArgumentException ignored) {
                return;
            }

            if (childValueMap != childValue) {
                ((Map<String, Object>) currentValueMap).put(pathParts[i], childValueMap);
            }
            value = childValueMap;
        }

        if (value instanceof Map<?, ?> currentValueMap) {
            ((Map<String, Object>) currentValueMap).remove(pathParts[pathParts.length - 1]);
        }
    }

    protected static void setConfigurationPathValue(Map<String, AssetAttributeConfigurationEntry> attributes, String path, Object value) {
        String[] pathParts = validateGenericParameterDocumentPath(path);

        AssetAttributeConfigurationEntry attributeConfiguration = attributes.get(pathParts[1]);
        if (attributeConfiguration == null) {
            throw new IllegalArgumentException("Generic parameter path references an unknown attribute: " + path);
        }

        MetaMap meta = attributeConfiguration.getMeta();
        if (meta == null) {
            throw new IllegalArgumentException("Generic parameter path references an attribute without meta: " + path);
        }

        String metaItemName = pathParts[3];
        if (pathParts.length == 4) {
            meta.addOrReplace(new MetaItem<>(metaItemName, null, value));
            return;
        }

        MetaItem<?> metaItem = meta.get(metaItemName).orElse(null);
        Map<String, Object> valueMap;
        if (metaItem == null) {
            valueMap = new LinkedHashMap<>();
            meta.addOrReplace(new MetaItem<>(metaItemName, null, valueMap));
        } else {
            Object metaItemValue = metaItem.getValue(Object.class).orElse(null);
            valueMap = toMutableStringObjectMap(metaItemValue, path);
            if (valueMap != metaItemValue) {
                setMetaItemValue(metaItem, valueMap);
            }
        }

        Map<String, Object> currentMap = valueMap;
        for (int i = 4; i < pathParts.length - 1; i++) {
            Object child = currentMap.get(pathParts[i]);
            if (child == null) {
                Map<String, Object> childMap = new LinkedHashMap<>();
                currentMap.put(pathParts[i], childMap);
                currentMap = childMap;
                continue;
            }

            if (!(child instanceof Map<?, ?> childValueMap)) {
                throw new IllegalArgumentException("Generic parameter path parent is not an object: " + path);
            }

            Map<String, Object> mutableChildMap = toMutableStringObjectMap(childValueMap, path);
            if (mutableChildMap != child) {
                currentMap.put(pathParts[i], mutableChildMap);
            }
            currentMap = mutableChildMap;
        }

        currentMap.put(pathParts[pathParts.length - 1], value);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> toMutableStringObjectMap(Object value, String path) {
        if (value instanceof Map<?, ?> valueMap) {
            return toMutableStringObjectMap(valueMap, path);
        }

        if (value == null
            || value instanceof CharSequence
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Collection<?>
            || value.getClass().isArray()) {
            throw new IllegalArgumentException("Generic parameter path parent is not an object" + (path != null ? ": " + path : ""));
        }

        return toMutableStringObjectMap(ValueUtil.JSON.convertValue(value, LinkedHashMap.class), path);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> toMutableStringObjectMap(Map<?, ?> valueMap, String path) {
        if (valueMap instanceof LinkedHashMap<?, ?>) {
            return (Map<String, Object>) valueMap;
        }

        Map<String, Object> mutableMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Generic parameter path parent contains a non-string key" + (path != null ? ": " + path : ""));
            }
            mutableMap.put(key, entry.getValue());
        }
        return mutableMap;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void setMetaItemValue(MetaItem<?> metaItem, Object value) {
        ((MetaItem) metaItem).setValue(value);
    }

    protected static void validateGenericParameter(String genericParameterName,
                                                   AssetAttributeConfigurationGenericParameter genericParameter) {
        if (TextUtil.isNullOrEmpty(genericParameterName)) {
            throw new IllegalArgumentException("Generic parameter name is required");
        }

        if (genericParameter == null) {
            throw new IllegalArgumentException("Generic parameter definition is required: " + genericParameterName);
        }

        if (TextUtil.isNullOrEmpty(genericParameter.getType())) {
            throw new IllegalArgumentException("Generic parameter type is required: " + genericParameterName);
        }

        validateGenericParameterType(genericParameter.getType());

        if (genericParameter.getPaths() == null || genericParameter.getPaths().isEmpty()) {
            throw new IllegalArgumentException("Generic parameter paths are required: " + genericParameterName);
        }

        for (String path : genericParameter.getPaths()) {
            validateGenericParameterDocumentPath(path);
        }
    }

    protected static void validateGenericParameterValue(String genericParameterName, String type, Object value) {
        validateGenericParameterType(type);

        boolean valid = switch (type) {
            case "text" -> value instanceof CharSequence;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "object" -> value instanceof Map<?, ?>;
            case "array" -> value instanceof Collection<?> || value.getClass().isArray();
            case "unknown" -> true;
            default -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException("Generic parameter value has invalid type: " + genericParameterName);
        }
    }

    protected static void validateGenericParameterType(String type) {
        switch (type) {
            case "text", "number", "boolean", "object", "array", "unknown":
                return;
            default:
                throw new IllegalArgumentException("Unsupported generic parameter type: " + type);
        }
    }

    protected static void validateGenericParameterPathTypes(Map<String, AssetAttributeConfigurationEntry> attributes,
                                                            Map<String, AssetAttributeConfigurationGenericParameter> genericParameters) {
        for (Map.Entry<String, AssetAttributeConfigurationGenericParameter> genericParameterEntry : genericParameters.entrySet()) {
            String genericParameterName = genericParameterEntry.getKey();
            AssetAttributeConfigurationGenericParameter genericParameter = genericParameterEntry.getValue();
            for (String path : genericParameter.getPaths()) {
                validateGenericParameterPathType(attributes, genericParameterName, genericParameter.getType(), path);
            }
        }
    }

    protected static void validateGenericParameterPathType(Map<String, AssetAttributeConfigurationEntry> attributes,
                                                           String genericParameterName,
                                                           String genericParameterType,
                                                           String path) {
        String[] pathParts = validateGenericParameterDocumentPath(path);
        AssetAttributeConfigurationEntry attributeConfiguration = attributes.get(pathParts[1]);
        if (attributeConfiguration == null || attributeConfiguration.getMeta() == null) {
            return;
        }

        String[] metaPathParts = new String[pathParts.length - 2];
        System.arraycopy(pathParts, 2, metaPathParts, 0, metaPathParts.length);

        String pathType = getGenericParameterType(attributeConfiguration.getMeta(), metaPathParts);
        if (!"unknown".equals(pathType) && !pathType.equals(genericParameterType)) {
            throw new IllegalArgumentException("Generic parameter type does not match path schema: " + genericParameterName);
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

    protected static String getGenericParameterType(MetaMap meta, String[] pathParts) {
        Optional<MetaItemDescriptor<?>> metaItemDescriptor = ValueUtil.getMetaItemDescriptor(pathParts[1]);
        if (pathParts.length == 2) {
            return metaItemDescriptor
                .map(MetaItemDescriptor::getType)
                .map(AssetAttributeConfigurationService::getGenericParameterType)
                .orElse("unknown");
        }

        if (metaItemDescriptor.isEmpty()) {
            return "unknown";
        }

        Object metaItemValue = meta.get(pathParts[1])
            .flatMap(metaItem -> metaItem.getValue(Object.class))
            .orElse(null);
        Class<?> metaItemType = getConcreteMetaValueType(metaItemDescriptor.get().getType().getType(), metaItemValue);
        return getNestedPathType(metaItemType, pathParts, 2)
            .map(AssetAttributeConfigurationService::getGenericParameterType)
            .orElse("unknown");
    }

    protected static Class<?> getConcreteMetaValueType(Class<?> descriptorType, Object value) {
        if (descriptorType == null || descriptorType == Object.class || value == null) {
            return descriptorType;
        }

        if (descriptorType.isAssignableFrom(value.getClass()) && descriptorType != value.getClass()) {
            return value.getClass();
        }

        if (AgentLink.class.isAssignableFrom(descriptorType)) {
            return getAgentLinkValueClass(value).orElse(descriptorType);
        }

        return descriptorType;
    }

    protected static Optional<Class<?>> getAgentLinkValueClass(Object value) {
        if (value instanceof AgentLink<?>) {
            return Optional.of(value.getClass());
        }

        if (!(value instanceof Map<?, ?> valueMap) || !(valueMap.get("type") instanceof String type) || TextUtil.isNullOrEmpty(type)) {
            return Optional.empty();
        }

        try {
            AgentLink<?> agentLink = ValueUtil.JSON.convertValue(value, AgentLink.class);
            return agentLink != null ? Optional.of(agentLink.getClass()) : Optional.empty();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    protected static Optional<Class<?>> getNestedPathType(Class<?> rootType, String[] pathParts, int pathPartIndex) {
        Class<?> currentType = rootType;
        for (int i = pathPartIndex; i < pathParts.length; i++) {
            if (currentType == null || currentType == Object.class || Map.class.isAssignableFrom(currentType)) {
                return Optional.of(Object.class);
            }

            if (Collection.class.isAssignableFrom(currentType) || currentType.isArray()) {
                return Optional.of(Object.class);
            }

            Optional<Field> field = getField(currentType, pathParts[i]);
            if (field.isEmpty()) {
                return Optional.empty();
            }

            currentType = field.get().getType();
        }

        return Optional.ofNullable(currentType);
    }

    protected static Optional<Field> getField(Class<?> type, String fieldName) {
        Class<?> currentType = type;
        while (currentType != null && currentType != Object.class) {
            try {
                return Optional.of(currentType.getDeclaredField(fieldName));
            } catch (NoSuchFieldException ignored) {
                currentType = currentType.getSuperclass();
            }
        }

        return Optional.empty();
    }

    protected static String getGenericParameterType(ValueDescriptor<?> valueDescriptor) {
        if (valueDescriptor.getArrayDimensions() != null && valueDescriptor.getArrayDimensions() > 0) {
            return "array";
        }

        return getGenericParameterType(valueDescriptor.getType());
    }

    protected static String getGenericParameterType(Class<?> type) {
        if (type == null || type == Object.class) {
            return "unknown";
        }
        if (ValueUtil.isString(type) || type.isEnum()) {
            return "text";
        }
        if (ValueUtil.isNumber(type)
            || type == byte.class
            || type == short.class
            || type == int.class
            || type == long.class
            || type == float.class
            || type == double.class) {
            return "number";
        }
        if (ValueUtil.isBoolean(type) || type == boolean.class) {
            return "boolean";
        }
        if (Map.class.isAssignableFrom(type)) {
            return "object";
        }
        if (Collection.class.isAssignableFrom(type) || type.isArray()) {
            return "array";
        }
        return "object";
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

        for (Map.Entry<String, AssetAttributeConfigurationEntry> attributeEntry : configuration.getAttributes().entrySet()) {
            validateAttributeEntry(attributeEntry.getKey(), attributeEntry.getValue());
        }

        if (configuration.getGenericParameters() != null) {
            for (Map.Entry<String, AssetAttributeConfigurationGenericParameter> genericParameterEntry : configuration.getGenericParameters().entrySet()) {
                validateGenericParameter(genericParameterEntry.getKey(), genericParameterEntry.getValue());
            }
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

    protected static Map<String, AssetAttributeConfigurationEntry> cloneConfigurationAttributes(Map<String, AssetAttributeConfigurationEntry> attributes) {
        Map<String, AssetAttributeConfigurationEntry> clonedAttributes = new LinkedHashMap<>();
        for (Map.Entry<String, AssetAttributeConfigurationEntry> attribute : attributes.entrySet()) {
            clonedAttributes.put(
                attribute.getKey(),
                new AssetAttributeConfigurationEntry(attribute.getValue().getType(), cloneMeta(attribute.getValue().getMeta()))
            );
        }
        return clonedAttributes;
    }

    protected static MetaMap cloneMeta(MetaMap meta) {
        MetaMap clonedMeta = ValueUtil.clone(meta);
        return clonedMeta != null ? clonedMeta : new MetaMap(meta.values());
    }
}
