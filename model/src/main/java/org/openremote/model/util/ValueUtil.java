/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openremote.model.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import org.hibernate.internal.util.SerializationHelper;
import org.openremote.model.AssetModelProvider;
import org.openremote.model.ModelDescriptor;
import org.openremote.model.ModelDescriptors;
import org.openremote.model.StandardModelProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.*;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.persistence.Entity;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.openremote.model.syslog.SyslogCategory.MODEL_AND_VALUES;

/**
 * Utilities for working with values/JSON and asset model
 * <p>
 * Custom descriptors can be added by simply adding new {@link Asset}/{@link Agent} sub types and following the discovery
 * rules described in {@link StandardModelProvider}; alternatively a custom {@link AssetModelProvider} implementation
 * can be created and discovered with the {@link ServiceLoader} or manually added to this class via
 * {@link ValueUtil#getModelProviders()} collection.
 */
@SuppressWarnings({"unchecked", "deprecation"})
@TsIgnore
public class ValueUtil {

    /**
     * Copied from: https://puredanger.github.io/tech.puredanger.com/2006/11/29/writing-a-class-hierarchy-comparator/
     */
    protected static class ClassHierarchyComparator implements Comparator<Class<?>> {

        public int compare(Class<?> c1, Class<?> c2) {
            if(c1 == null) {
                if(c2 == null) {
                    return 0;
                } else {
                    // Sort nulls first
                    return 1;
                }
            } else if(c2 == null) {
                // Sort nulls first
                return -1;
            }

            // At this point, we know that c1 and c2 are not null
            if(c1.equals(c2)) {
                return 0;
            }

            // At this point, c1 and c2 are not null and not equal, here we
            // compare them to see which is "higher" in the class hierarchy
            boolean c1Lower = c2.isAssignableFrom(c1);
            boolean c2Lower = c1.isAssignableFrom(c2);

            if(c1Lower && !c2Lower) {
                return 1;
            } else if(c2Lower && !c1Lower) {
                return -1;
            }

            // Doesn't matter, sort consistently on classname
            return c1.getName().compareTo(c2.getName());
        }
    }

    // Preload the Standard model provider so it takes priority over others
    public static Logger LOG = SyslogCategory.getLogger(MODEL_AND_VALUES, ValueUtil.class);
    public static ObjectMapper JSON;
    protected static List<AssetModelProvider> assetModelProviders = new ArrayList<>(Collections.singletonList(new StandardModelProvider()));
    protected static Map<Class<? extends Asset<?>>, AssetTypeInfo> assetInfoMap;
    protected static Map<String, Class<? extends Asset<?>>> assetTypeMap;
    protected static Map<String, Class<? extends AgentLink<?>>> agentLinkMap;
    protected static List<MetaItemDescriptor<?>> metaItemDescriptors;
    protected static List<ValueDescriptor<?>> valueDescriptors;
    protected static Validator validator;
    protected static JsonSchemaGenerator generator;

    static {
        // Find all service loader registered asset model providers
        ServiceLoader.load(AssetModelProvider.class).forEach(ValueUtil.assetModelProviders::add);
        initialise();
    }

    public static ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
        objectMapper
            .setConstructorDetector(ConstructorDetector.DEFAULT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false) // see https://github.com/FasterXML/jackson-databind/issues/1547
            .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.DEFAULT));

        objectMapper.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

        SimpleFilterProvider filters = new SimpleFilterProvider();
        filters.setFailOnUnknownId(false);

        objectMapper.setFilterProvider(filters);
        return objectMapper;
    }

    public static final String NULL_LITERAL = "null";

    public static Optional<JsonNode> parse(String jsonString) {
        if (TextUtil.isNullOrEmpty(jsonString) || NULL_LITERAL.equals(jsonString)) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.readTree(jsonString));
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to parse JSON string: " + jsonString, e);
        }
        return Optional.empty();
    }

    public static <T> Optional<T> parse(String jsonString, Type type) {
        if (NULL_LITERAL.equals(jsonString)) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.readValue(jsonString, JSON.constructType(type)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse JSON", e);
        }
        return Optional.empty();
    }

    public static <T> Optional<T> parse(String jsonString, TypeReference<T> type) {
        return parse(jsonString, JSON.getTypeFactory().constructType(type));
    }

    public static <T> Optional<T> parse(String jsonString, Class<T> type) {
        return parse(jsonString, JSON.constructType(type));
    }

    public static Optional<String> asJSON(Object object) {
        try {
            return Optional.of(asJSONOrThrow(object));
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Failed to convert object to JSON string", e);
            return Optional.empty();
        }
    }

    public static String asJSONOrThrow(Object object) throws JsonProcessingException {
        if (object == null) {
            return NULL_LITERAL;
        }
        return JSON.writeValueAsString(object);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Optional<T> getValue(Object value, Class<T> type, boolean coerce) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Optional) {
            Optional opt = (Optional)value;
            if (!opt.isPresent()) {
                return opt;
            }
            value = opt.get();
        }

        if (type.isAssignableFrom(value.getClass())) {
            return Optional.of((T) value);
        }

        if (value instanceof String && !coerce) {
            return Optional.empty();
        }

        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (Number.class.isAssignableFrom(type)) {
                if (type == Number.class && !coerce) {
                    return Optional.ofNullable((T)node.numberValue());
                } else if (type == Integer.class && (node.isInt() || coerce)) {
                    return Optional.of((T) Integer.valueOf(node.asInt()));
                } else if (type == Double.class && (node.isDouble() || coerce)) {
                    return Optional.of((T) Double.valueOf(node.asDouble()));
                } else if (type == Long.class && (node.isLong() || coerce)) {
                    return Optional.of((T) Long.valueOf(node.asLong()));
                } else if (type == BigDecimal.class && (node.isBigDecimal() || coerce)) {
                    return Optional.of((T) node.decimalValue());
                } else if (type == BigInteger.class && (node.isBigInteger() || coerce)) {
                    return Optional.of((T) node.bigIntegerValue());
                } else if (type == Short.class && (node.isShort() || coerce)) {
                    return Optional.of((T) Short.valueOf(node.shortValue()));
                }
            }
            if (String.class == type) {
                if (node.isTextual()) {
                    return Optional.of((T) node.asText());
                }
                if (coerce) {
                    return Optional.of((T) node.toString());
                }
            }
            if (Boolean.class == type && (node.isBoolean() || coerce)) {
                if (!node.isBoolean() && node.isTextual()) {
                    if ("TRUE".equalsIgnoreCase(node.textValue()) || "1".equalsIgnoreCase(node.textValue()) || "ON".equalsIgnoreCase(node.textValue())) {
                        return Optional.of((T) Boolean.TRUE);
                    }
                    return Optional.of((T) Boolean.FALSE);
                }
                return Optional.of((T) Boolean.valueOf(node.asBoolean()));
            }
            if ((type.isArray() && node.isArray()) || (!node.isArray() && node.isObject())) {
                try {
                    return Optional.of(((JsonNode) value).traverse().readValueAs(type));
                } catch (Exception ignored) {
                }
            }
        }

        if (coerce) {
            try {
                if (value instanceof List && type.isArray()) {
                    Class<?> innerType = type.getComponentType();
                    List list = ((List) value);

                    if (list.isEmpty()) {
                        return Optional.of((T)Array.newInstance(type.getComponentType(), 0));
                    }

                    Object[] arr = (Object[])Array.newInstance(type.getComponentType(), list.size());
                    IntStream.range(0, list.size()).forEach(i -> {
                        Object o = list.get(i);
                        if (o != null && !innerType.isAssignableFrom(o.getClass())) {
                            o = getValue(o, innerType, true).orElse(null);
                        }
                        arr[i] = o;
                    });
                    return Optional.of((T)arr);
                }

                return Optional.of(JSON.convertValue(value, type));
            } catch (Exception e) {
                if (value instanceof String) {

                    if (!((String) value).startsWith("\"")) {}

                    // Try and parse the value
                    return parse((String)value, type);
                }

                LOG.log(Level.INFO, "Failed to coerce value to requested type: input=" + value.getClass() + ", output=" + type, e);
                return Optional.empty();
            }
        }

        LOG.info("Failed to get value as requested type: input=" + value.getClass() + ", output=" + type);
        return Optional.empty();
    }

    public static <T> Optional<T> getValue(Object value, Class<T> type) {
        return getValue(value, type, false);
    }

    /**
     * Basic type coercion/casting of values; utilises Jackson's underlying type coercion/casting mechanism
     */
    public static <T> Optional<T> getValueCoerced(Object value, Class<T> type) {
        return getValue(value, type, true);
    }

    public static Optional<String> getString(Object value) {
        return getValue(value, String.class);
    }

    public static Optional<String> getStringCoerced(Object value) {
        return getValueCoerced(value, String.class);
    }

    public static Optional<Boolean> getBoolean(Object value) {
        return getValue(value, Boolean.class);
    }

    public static Optional<Boolean> getBooleanCoerced(Object value) {
        return getValueCoerced(value, Boolean.class);
    }

    public static Optional<Integer> getInteger(Object value) {
        return getValue(value, Integer.class);
    }

    public static Optional<Integer> getIntegerCoerced(Object value) {
        return getValueCoerced(value, Integer.class);
    }

    public static Optional<Double> getDouble(Object value) {
        return getValue(value, Double.class);
    }

    public static Optional<Double> getDoubleCoerced(Object value) {
        return getValueCoerced(value, Double.class);
    }

    public static Optional<Long> getLong(Object value) {
        return getValue(value, Long.class);
    }

    public static Optional<Long> getLongCoerced(Object value) {
        return getValueCoerced(value, Long.class);
    }

    public static Optional<ObjectNode> asJSONObject(Object value) {
        return getValue(value, ObjectNode.class);
    }

    public static Optional<ArrayNode> asJSONArray(Object value) {
        return getValue(value, ArrayNode.class);
    }

    public static ObjectNode createJsonObject() {
        return ValueUtil.JSON.createObjectNode();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static <T> T convert(Object object, Class<T> targetType) {
        if (object == null) {
            return null;
        }
        if (targetType == object.getClass()) {
            return (T)object;
        }
        if (targetType == String.class) {
            if (object instanceof TextNode) {
                return (T) ((TextNode)object).textValue();
            }
            return (T) asJSON(object).orElse(null);
        }
        return JSON.convertValue(object, targetType);
    }

    public static boolean isArray(Class<?> clazz) {
        return clazz.isArray() || clazz == ArrayNode.class;
    }

    public static boolean isBoolean(Class<?> clazz) {
        return clazz == Boolean.class || clazz == BooleanNode.class;
    }

    public static boolean isNumber(Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz) || NumericNode.class.isAssignableFrom(clazz);
    }

    public static boolean isString(Class<?> clazz) {
        return String.class.isAssignableFrom(clazz) || TextNode.class.isAssignableFrom(clazz) || BinaryNode.class.isAssignableFrom(clazz);
    }

    public static boolean isObject(Class<?> clazz) {
        return !isArray(clazz) && !isBoolean(clazz) && !isNumber(clazz) && !isString(clazz);
    }

    public static Class<?> getArrayClass(Class<?> componentType) throws ClassNotFoundException {
        ClassLoader classLoader = componentType.getClassLoader();
        String name;
        if (componentType.isArray()) {
            // just add a leading "["
            name = "[" + componentType.getName();
        } else if (componentType == boolean.class) {
            name = "[Z";
        } else if (componentType == byte.class) {
            name = "[B";
        } else if (componentType == char.class) {
            name = "[C";
        } else if (componentType == double.class) {
            name = "[D";
        } else if (componentType == float.class) {
            name = "[F";
        } else if (componentType == int.class) {
            name = "[I";
        } else if (componentType == long.class) {
            name = "[J";
        } else if (componentType == short.class) {
            name = "[S";
        } else {
            // must be an object non-array class
            name = "[L" + componentType.getName() + ";";
        }
        return Class.forName(name);
    }

    /**
     * Apply the specified set of {@link ValueFilter}s to the specified value
     */
    public static Object applyValueFilters(Object value, ValueFilter...filters) {

        if (filters == null) {
            return value;
        }

        if (value == null) {
            return null;
        }

        LOG.finest("Applying value filters to value of type: " + value.getClass().getName());

        for (ValueFilter filter : filters) {

            value = filter.filter(value);

            if (value == null) {
                break;
            }
        }

        return value;
    }

    public static <T> T clone(T object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Serializable) {
            try {
                return (T) SerializationHelper.clone((Serializable) object);
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to clone using standard java serialisation, falling back to jackson object of type: " + object.getClass(), e);
            }
        }

        try {
            return JSON.readValue(JSON.writeValueAsBytes(object), (Class<T>) object.getClass());
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to clone object of type: " + object.getClass(), e);
        }

        return null;
    }

    public static <T> TypeReference<Attribute<T>> getRef(Class<T> clazz) {
        return new TypeReference<Attribute<T>>() {};
    }

    public static Object findFirstNonNullElement(Collection<?> collection) {
        for (java.lang.Object element : collection) {
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    public static Map.Entry<?,?> findFirstNonNullEntry(Map<?,?> map) {
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                return entry;
            }
        }
        return null;
    }

    public static AssetTypeInfo[] getAssetInfos(String parentType) {
        return assetInfoMap.values().toArray(new AssetTypeInfo[0]);
    }

    public static Class<? extends Asset<?>>[] getAssetClasses(String parentType) {
        return assetTypeMap.values().toArray(new Class[0]);
    }

    public static Optional<AssetTypeInfo> getAssetInfo(Class<? extends Asset<?>> assetType) {
        return Optional.ofNullable(assetInfoMap.get(assetType));
    }

    public static Optional<AssetTypeInfo> getAssetInfo(String assetType) {
        Class<? extends Asset<?>> assetClass = assetTypeMap.get(assetType);
        return Optional.ofNullable(assetClass != null ? assetInfoMap.get(assetClass) : null);
    }

    // TODO: Implement ability to restrict which asset types are allowed to be added to a given parent type
    public static AssetDescriptor<?>[] getAssetDescriptors(String parentType) {
        return Arrays.stream(getAssetInfos(parentType)).map(AssetTypeInfo::getAssetDescriptor).toArray(AssetDescriptor[]::new);
    }

    public static <T extends Asset<?>> Optional<AssetDescriptor<T>> getAssetDescriptor(Class<T> assetType) {
        return getAssetInfo(assetType).map(assetInfo -> (AssetDescriptor<T>)assetInfo.getAssetDescriptor());
    }

    public static Optional<AssetDescriptor<?>> getAssetDescriptor(String assetType) {
        return getAssetInfo(assetType).map(AssetTypeInfo::getAssetDescriptor);
    }

    public static <T extends Agent<T, ?, ?>> Optional<AgentDescriptor<T, ?, ?>> getAgentDescriptor(Class<T> agentType) {
        return getAssetDescriptor(agentType)
            .map(assetDescriptor -> assetDescriptor instanceof AgentDescriptor ? (AgentDescriptor<T, ?, ?>)assetDescriptor : null);
    }

    public static Optional<AgentDescriptor<?, ?, ?>> getAgentDescriptor(String agentType) {
        return getAssetDescriptor(agentType)
            .map(assetDescriptor -> assetDescriptor instanceof AgentDescriptor ? (AgentDescriptor<?, ?, ?>)assetDescriptor : null);
    }

    public static MetaItemDescriptor<?>[] getMetaItemDescriptors() {
        return metaItemDescriptors.toArray(new MetaItemDescriptor<?>[0]);
    }

    public static Optional<MetaItemDescriptor<?>[]> getMetaItemDescriptors(Class<? extends Asset<?>> assetType) {
        return getAssetInfo(assetType).map(AssetTypeInfo::getMetaItemDescriptors);
    }

    public static Optional<MetaItemDescriptor<?>[]> getMetaItemDescriptors(String assetType) {
        return getAssetInfo(assetType).map(AssetTypeInfo::getMetaItemDescriptors);
    }

    public static Optional<MetaItemDescriptor<?>> getMetaItemDescriptor(String name) {
        if (TextUtil.isNullOrEmpty(name)) return Optional.empty();

        return metaItemDescriptors.stream().filter(mid -> mid.getName().equals(name)).findFirst();
    }

    public static ValueDescriptor<?>[] getValueDescriptors() {
        return valueDescriptors.toArray(new ValueDescriptor<?>[0]);
    }

    public static Optional<ValueDescriptor<?>[]> getValueDescriptors(Class<? extends Asset<?>> assetType) {
        return getAssetInfo(assetType).map(AssetTypeInfo::getValueDescriptors);
    }

    public static Optional<ValueDescriptor<?>[]> getValueDescriptors(String assetType) {
        return getAssetInfo(assetType).map(AssetTypeInfo::getValueDescriptors);
    }

    public static Optional<ValueDescriptor<?>> getValueDescriptor(String name) {
        if (TextUtil.isNullOrEmpty(name)) return Optional.empty();

        int arrayDimensions = 0;

        while(name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            arrayDimensions++;
        }

        String finalName = name;
        int finalArrayDimensions = arrayDimensions;
        return valueDescriptors.stream().filter(vd -> vd.getName().equals(finalName)).findFirst().map(vd -> {
            int dims = finalArrayDimensions;
            while(dims > 0) {
                vd = vd.asArray();
                dims--;
            }
            return vd;
        });
    }

    public static ValueDescriptor<?> getValueDescriptorForValue(Object value) {
        if (value == null) {
            return ValueDescriptor.UNKNOWN;
        }

        Class<?> valueClass = value.getClass();
        boolean isArray = valueClass.isArray();
        valueClass = isArray ? valueClass.getComponentType() : valueClass;
        ValueDescriptor<?> valueDescriptor = ValueDescriptor.UNKNOWN;

        if (valueClass == Boolean.class) valueDescriptor = ValueType.BOOLEAN;
        else if (valueClass == String.class) valueDescriptor = ValueType.TEXT;
        else if (valueClass == Integer.class) valueDescriptor = ValueType.INTEGER;
        else if (valueClass == Long.class) valueDescriptor = ValueType.LONG;
        else if (valueClass == Double.class || valueClass == Float.class) valueDescriptor = ValueType.NUMBER;
        else if (valueClass == BigInteger.class) valueDescriptor = ValueType.BIG_INTEGER;
        else if (valueClass == BigDecimal.class) valueDescriptor = ValueType.BIG_NUMBER;
        else if (valueClass == Byte.class) valueDescriptor = ValueType.BYTE;
        else if (Map.class.isAssignableFrom(valueClass)) valueDescriptor = ValueType.JSON_OBJECT;

        return isArray ? valueDescriptor.asArray() : valueDescriptor;
    }

    public static List<AssetModelProvider> getModelProviders() {
        return assetModelProviders;
    }

    public static void initialise() throws IllegalStateException {
        try {
            doInitialise();
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, "Failed to initialise the asset model", e);
            throw e;
        }
    }

    /**
     * Initialise the asset model and throw an {@link IllegalStateException} exception if a problem is detected; this
     * can be called by applications at startup to fail hard and fast if the asset model is un-usable
     */
    protected static void doInitialise() throws IllegalStateException {
        assetInfoMap = new HashMap<>();
        assetTypeMap = new HashMap<>();
        agentLinkMap = new HashMap<>();
        metaItemDescriptors = new ArrayList<>();
        valueDescriptors = new ArrayList<>();
        generator = null;

        // Provide basic Object Mapper and enhance once asset model is initialised
        JSON = configureObjectMapper(new ObjectMapper());

        LOG.info("Initialising asset model...");
        Map<Class<? extends Asset<?>>, List<NameHolder>> assetDescriptorProviders = new TreeMap<>(new ClassHierarchyComparator());
        //noinspection RedundantCast
        assetDescriptorProviders.put((Class<? extends Asset<?>>)(Class<?>)Asset.class, new ArrayList<>(getDescriptorFields(Asset.class)));

        getModelProviders().forEach(assetModelProvider -> {
            LOG.fine("Processing asset model provider: " + assetModelProvider.getClass().getSimpleName());
            LOG.fine("Auto scan = " + assetModelProvider.useAutoScan());

            if (assetModelProvider.useAutoScan()) {

                Set<Class<? extends Asset<?>>> assetClasses = getAssetClasses(assetModelProvider);
                LOG.fine("Found " + assetClasses.size() + " asset class(es)");

                assetClasses.forEach(assetClass ->
                    assetDescriptorProviders.computeIfAbsent(assetClass, aClass ->
                        new ArrayList<>(getDescriptorFields(aClass))));

                ModelDescriptors modelDescriptors = assetModelProvider.getClass().getAnnotation(ModelDescriptors.class);
                if (modelDescriptors != null) {
                    for (ModelDescriptor modelDescriptor : modelDescriptors.value()) {
                        Class<? extends Asset<?>> assetClass = (Class<? extends Asset<?>>)modelDescriptor.assetType();

                        assetDescriptorProviders.compute(assetClass, (aClass, list) -> {
                            if (list == null) {
                                list = new ArrayList<>();
                            }

                            list.addAll(getDescriptorFields(modelDescriptor.provider()));
                            return list;
                        });
                    }
                }
            }

            if (assetModelProvider.getAssetDescriptors() != null) {
                for (AssetDescriptor<?> assetDescriptor : assetModelProvider.getAssetDescriptors()) {
                    Class<? extends Asset<?>> assetClass = assetDescriptor.getType();

                    assetDescriptorProviders.compute(assetClass, (aClass, list) -> {
                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        list.add(assetDescriptor);
                        return list;
                    });
                }
            }

            if (assetModelProvider.getAttributeDescriptors() != null) {
                assetModelProvider.getAttributeDescriptors().forEach((assetClass, attributeDescriptors) ->
                    assetDescriptorProviders.compute(assetClass, (aClass, list) -> {
                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        list.addAll(attributeDescriptors);
                        return list;
                    }));
            }

            if (assetModelProvider.getMetaItemDescriptors() != null) {
                assetModelProvider.getMetaItemDescriptors().forEach((assetClass, metaDescriptors) ->
                    assetDescriptorProviders.compute(assetClass, (aClass, list) -> {
                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        list.addAll(metaDescriptors);
                        return list;
                    }));
            }

            if (assetModelProvider.getValueDescriptors() != null) {
                assetModelProvider.getValueDescriptors().forEach((assetClass, valueDescriptors) ->
                    assetDescriptorProviders.compute(assetClass, (aClass, list) -> {
                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        list.addAll(valueDescriptors);
                        return list;
                    }));
            }
        });

        // Build each asset info checking that no conflicts occur
        Map<Class<? extends Asset<?>>, List<NameHolder>> copy = new HashMap<>(assetDescriptorProviders);
        assetDescriptorProviders.forEach((assetClass, descriptors) -> {

            // Skip abstract classes as a start point - they should be in the class hierarchy of concrete class
            if (!Modifier.isAbstract(assetClass.getModifiers())) {

                AssetTypeInfo assetInfo = buildAssetInfo(assetClass, copy);
                assetInfoMap.put(assetClass, assetInfo);
                assetTypeMap.put(assetInfo.getAssetDescriptor().getName(), assetClass);

                if (assetInfo.getAssetDescriptor() instanceof AgentDescriptor) {
                    AgentDescriptor<?,?,?> agentDescriptor = (AgentDescriptor<?,?,?>)assetInfo.getAssetDescriptor();
                    String agentLinkName = agentDescriptor.getAgentLinkClass().getSimpleName();
                    if (agentLinkMap.containsKey(agentLinkName) && agentLinkMap.get(agentLinkName) != agentDescriptor.getAgentLinkClass()) {
                        throw new IllegalStateException("AgentLink simple class name must be unique, duplicate found for: " + agentDescriptor.getAgentLinkClass());
                    }
                    agentLinkMap.put(agentLinkName, agentDescriptor.getAgentLinkClass());
                }
            }
        });

        // Check each value type implements serializable interface
        List<ValueDescriptor<?>> nonSerializableValueDescriptors = new ArrayList<>();
        valueDescriptors.forEach(vd -> {
            if (!Serializable.class.isAssignableFrom(vd.getType())) {
                nonSerializableValueDescriptors.add(vd);
            }
        });

        if (!nonSerializableValueDescriptors.isEmpty()) {
            String vds = nonSerializableValueDescriptors.stream().map(ValueDescriptor::toString).collect(Collectors.joining(",\n"));
            throw new IllegalStateException("One or more value types do not implement java.io.Serializable: " + vds);
        }

        // Call on finished on each provider
        assetModelProviders.forEach(AssetModelProvider::onAssetModelFinished);

        // Add agent link sub types to object mapper (need to avoid circular dependency)
        NamedType[] agentLinkSubTypes = Arrays.stream(getAgentLinkClasses())
            .map(agentLinkClass -> new NamedType(
                agentLinkClass,
                agentLinkClass.getSimpleName()
            )).toArray(NamedType[]::new);
        JSON.registerSubtypes(agentLinkSubTypes);

        doSchemaInit();
    }

    protected static void doSchemaInit() {
        generator = new JsonSchemaGenerator(JSON, JSONSchemaUtil.getJsonSchemaConfig());
    }


    protected static Class<?>[] getAgentLinkClasses() {
        return Arrays.stream(getAssetDescriptors(null))
            .filter(descriptor -> descriptor instanceof AgentDescriptor)
            .map(descriptor ->
                ((AgentDescriptor<?, ?, ?>) descriptor).getAgentLinkClass()
            )
            .distinct()
            .toArray(Class<?>[]::new);
    }

    /**
     * Validates the supplied object using standard JSR-380 bean validation; therefore any type passed in here must
     * follow the JSR-380 annotation requirements.
     */
    // TODO: Implement validation using javax bean validation JSR-380
    public static <T> Set<ConstraintViolation<T>> validate(@NotNull T obj, Class<?>... groups) {

        Validator validator = getValidator();
        return validator.validate(obj, groups);
    }

    public static Validator getValidator() {
        if (validator == null) {
            validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
        }

        return validator;
    }

    /**
     * Returns the schema for the specified type
     */
    public static JsonNode getSchema(Class<?> clazz) {
        if (generator == null) {
            return JSON.createObjectNode();
        }
        return generator.generateJsonSchema(clazz);
    }

    public static void initialiseAssetAttributes(Asset<?> asset) throws IllegalStateException {
        AssetTypeInfo assetInfo = getAssetInfo(asset.getType()).orElseThrow(() -> new IllegalStateException("Cannot get asset model info for requested asset type: " + asset.getType()));
        asset.getAttributes().addOrReplace(
            Arrays.stream(assetInfo.getAttributeDescriptors())
            .filter(attributeDescriptor -> !attributeDescriptor.isOptional())
            .map(Attribute::new)
            .collect(Collectors.toList())
        );
    }

    public static Object[] getObjectFieldValues(Object object, String[] fieldNames) {
        return Arrays.stream(fieldNames).map(fieldName -> getObjectFieldValue(object, fieldName)).toArray();
    }

    public static Object[] getObjectFieldValues(Object object, Field[] fields) {
        return Arrays.stream(fields).map(field -> getObjectFieldValue(object, field)).toArray();
    }

    public static Object getObjectFieldValue(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            return getObjectFieldValue(object, field);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Specified field could not be found: class=" + object.getClass().getSimpleName() + ", field=" + fieldName);
        }
    }
    public static Object getObjectFieldValue(Object object, Field field) {
        field.setAccessible(true);
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to get field value: class=" + object.getClass().getSimpleName() + ", field=" + field.getName());
        }
    }

    public static boolean objectsEquals(Object a, Object b) {
        if (!Objects.equals(a, b)) {
            Object subject = a != null ? a : b;
            Object subordinate = a != null ? b : a;
            if (subject.getClass().isArray()) {
                Class<?> arrayType = subject.getClass().getComponentType();
                if (arrayType.isPrimitive()) {
                    if (arrayType == boolean.class) {
                        return Arrays.equals((boolean[]) subject, (boolean[]) subordinate);
                    }
                    if (arrayType == int.class) {
                        return Arrays.equals((int[]) subject, (int[]) subordinate);
                    }
                    if (arrayType == double.class) {
                        return Arrays.equals((double[]) subject, (double[]) subordinate);
                    }
                    if (arrayType == float.class) {
                        return Arrays.equals((float[]) subject, (float[]) subordinate);
                    }
                    if (arrayType == long.class) {
                        return Arrays.equals((long[]) subject, (long[]) subordinate);
                    }
                    if (arrayType == short.class) {
                        return Arrays.equals((short[]) subject, (short[]) subordinate);
                    }
                    if (arrayType == byte.class) {
                        return Arrays.equals((byte[]) subject, (byte[]) subordinate);
                    }
                    if (arrayType == char.class) {
                        return Arrays.equals((char[]) subject, (char[]) subordinate);
                    }
                    return false;
                }
                return Arrays.deepEquals((Object[])subject,(Object[])subordinate);
            }
            return false;
        }
        return true;
    }

    protected static boolean isGetter(Method method) {
        if (Modifier.isPublic(method.getModifiers()) &&
            method.getParameterTypes().length == 0) {
            if (method.getName().matches("^get[A-Z].*") &&
                !method.getReturnType().equals(void.class))
                return true;
            if (method.getName().matches("^is[A-Z].*") &&
                method.getReturnType().equals(boolean.class))
                return true;
        }
        return false;
    }

    protected static Set<Class<? extends Asset<?>>> getAssetClasses(AssetModelProvider assetModelProvider) {

        Set<Class<? extends Asset<?>>> assetClasses;

        // Search for concrete asset classes in the same JAR as the provided AssetModelProvider
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forClass(assetModelProvider.getClass()))
            .setScanners(
                new SubTypesScanner(true)
            ));

        LOG.fine("Scanning for Asset classes");

        assetClasses = reflections.getSubTypesOf(Asset.class).stream()
            .map(assetClass -> (Class<? extends Asset<?>>)assetClass)
            .filter(assetClass -> assetClass.getAnnotation(ModelIgnore.class) == null)
            .collect(Collectors.toSet());

        LOG.fine("Found asset class count = " + assetClasses.size());

        return assetClasses;
    }

    /**
     * Extract public static field values that are of type {@link AssetDescriptor}, {@link AttributeDescriptor}, {@link MetaItemDescriptor} or {@link ValueDescriptor}.
     */
    protected static List<NameHolder> getDescriptorFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field ->
                isStatic(field.getModifiers())
                && isPublic(field.getModifiers())
                && (AssetDescriptor.class.isAssignableFrom(field.getType())
                    || AttributeDescriptor.class.isAssignableFrom(field.getType())
                    || MetaItemDescriptor.class.isAssignableFrom(field.getType())
                    || ValueDescriptor.class.isAssignableFrom(field.getType())))
            .map(field -> {
                try {
                    return (NameHolder)field.get(null);
                } catch (IllegalAccessException e) {
                    String msg = "Failed to extract descriptor fields from class: " + type.getName();
                    LOG.log(Level.SEVERE, msg, e);
                    throw new IllegalStateException(msg);
                }
            })
            .collect(Collectors.toList());
    }

    protected static AssetTypeInfo buildAssetInfo(Class<? extends Asset<?>> assetClass, Map<Class<? extends Asset<?>>, List<NameHolder>> classDescriptorMap) throws IllegalStateException {

        Class<?> currentClass = assetClass;
        List<Class<?>> classTree = new ArrayList<>();

        while (Asset.class.isAssignableFrom(currentClass)) {
            classTree.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }

        // Check asset class has JPA entity annotation for JPA polymorphism
        if (assetClass.getAnnotation(Entity.class) == null) {
            throw new IllegalStateException("Asset class must have @Entity JPA annotation for polymorphic JPA support: " + assetClass);
        }

        if (Arrays.stream(assetClass.getDeclaredConstructors()).noneMatch(ctor -> ctor.getParameterCount() == 0)) {
            throw new IllegalStateException("Asset class must have a no args constructor for JPA support: " + assetClass);
        }

        // Order from Asset class downwards
        Collections.reverse(classTree);

        AtomicReference<AssetDescriptor<?>> assetDescriptor = new AtomicReference<>();
        Set<AttributeDescriptor<?>> attributeDescriptors = new HashSet<>(10);
        List<MetaItemDescriptor<?>> metaItemDescriptors = new ArrayList<>(50);
        List<ValueDescriptor<?>> valueDescriptors = new ArrayList<>(50);

        classTree.forEach(aClass -> {
            List<NameHolder> descriptors = classDescriptorMap.get(aClass);
            if (descriptors != null) {
                descriptors.forEach(descriptor -> {
                    if (aClass == assetClass && descriptor instanceof AssetDescriptor) {
                        if (assetDescriptor.get() != null) {
                            throw new IllegalStateException("Duplicate asset descriptor found: asset type=" + assetClass +", descriptor=" + assetDescriptor.get() + ", duplicate descriptor=" + descriptor);
                        }
                        assetDescriptor.set((AssetDescriptor<?>) descriptor);
                    } else if (descriptor instanceof AttributeDescriptor) {
                        attributeDescriptors.stream().filter(d -> d.equals(descriptor)).findFirst()
                            .ifPresent(existingDescriptor -> {
                                if (!existingDescriptor.getType().equals(((AttributeDescriptor<?>) descriptor).getType())) {
                                    throw new IllegalStateException("Attribute descriptor override cannot change the value type found: asset type=" + assetClass + ", descriptor=" + existingDescriptor + ", duplicate descriptor=" + descriptor);
                                }
                                attributeDescriptors.remove(existingDescriptor);
                            });
                        attributeDescriptors.add((AttributeDescriptor<?>) descriptor);
                    } else if (descriptor instanceof MetaItemDescriptor) {
                        int index = ValueUtil.metaItemDescriptors.indexOf(descriptor);
                        if (index >= 0 && ValueUtil.metaItemDescriptors.get(index) != descriptor) {
                            throw new IllegalStateException("Duplicate meta item descriptor found: asset type=" + assetClass +", descriptor=" + metaItemDescriptors.get(index) + ", duplicate descriptor=" + descriptor);
                        }
                        metaItemDescriptors.add((MetaItemDescriptor<?>) descriptor);
                        if (!ValueUtil.metaItemDescriptors.contains(descriptor)) {
                            ValueUtil.metaItemDescriptors.add((MetaItemDescriptor<?>) descriptor);
                        }
                    } else if (descriptor instanceof ValueDescriptor) {
                        ValueDescriptor<?> valueDescriptor = (ValueDescriptor<?>)descriptor;
                        // Only store basic value type ignore array type for value descriptor as any value descriptor can be an array value descriptor

                        valueDescriptor = valueDescriptor.asNonArray();

                        int index = valueDescriptors.indexOf(descriptor);
                        if (index >= 0 && valueDescriptors.get(index).getType() != valueDescriptor.getType()) {
                            throw new IllegalStateException("Duplicate value descriptor found: asset type=" + assetClass +", descriptor=" + valueDescriptors.get(index) + ", duplicate descriptor=" + descriptor);
                        }
                        valueDescriptors.add(valueDescriptor);
                        if (!ValueUtil.valueDescriptors.contains(descriptor)) {
                            ValueUtil.valueDescriptors.add((ValueDescriptor<?>) descriptor);
                        }
                    }
                });
            }
        });

        if (assetDescriptor.get() == null || assetDescriptor.get().getType() != assetClass) {
            throw new IllegalStateException("Asset descriptor not found or is not for this asset type: asset type=" + assetClass +", descriptor=" + assetDescriptor.get());
        }

        return new AssetTypeInfo(
            assetDescriptor.get(),
            attributeDescriptors.toArray(new AttributeDescriptor<?>[0]),
            metaItemDescriptors.toArray(new MetaItemDescriptor<?>[0]),
            valueDescriptors.toArray(new ValueDescriptor<?>[0]));
    }
}
