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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.hibernate.internal.util.SerializationHelper;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TsIgnore;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for working with values and JSON
 */
@SuppressWarnings({"unchecked", "deprecation"})
@TsIgnore
public class Values {

    private static final Logger LOG = Logger.getLogger(Values.class.getName());
    public static final ObjectMapper JSON;

    static {
        JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false) // see https://github.com/FasterXML/jackson-databind/issues/1547
            .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
            //.registerModule(new ORModelModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

        JSON.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

        SimpleFilterProvider filters = new SimpleFilterProvider();
        filters.setFailOnUnknownId(false);
        JSON.setFilterProvider(filters);
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
    protected static <T> Optional<T> getValue(Object value, Class<T> type, boolean coerce) {
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
                return Optional.of(JSON.convertValue(value, type));
            } catch (Exception e) {
                if (value instanceof String) {
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

    public static <T> T[] reverseArray(T[] array, Class<T> clazz) {
        if (array == null) {
            return null;
        }

        List<T> list = Arrays.asList(array);
        Collections.reverse(list);
        return list.toArray(createArray(0, clazz));
    }

    public static <T> T[] createArray(int size, Class<T> clazz) {
        return (T[]) Array.newInstance(clazz, size);
    }

    public static ObjectNode createJsonObject() {
        return Values.JSON.createObjectNode();
    }

    public static List<Object> createObjectList() {
        return new ArrayList<>();
    }

    /**
     * @param o A timestamp string as 'HH:mm:ss' or 'HH:mm'.
     * @return Epoch time or 0 if there is a problem parsing the timestamp string.
     */
    public static long parseTimestamp(Object o) {
        String timestamp = "";
        try {
            timestamp = o.toString();
        } catch (Exception e) {
            return (0L);
        }
        SimpleDateFormat sdf;
        if (timestamp.length() == 8) {
            sdf = new SimpleDateFormat("HH:mm:ss");
        } else if (timestamp.length() == 5) {
            sdf = new SimpleDateFormat("HH:mm");
        } else {
            return (0L);
        }
        try {
            return (sdf.parse(timestamp).getTime());
        } catch (ParseException e) {
            return (0L);
        }
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'HH:mm' or <code>null</code> if the timestamp is <= 0.
     */
    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'HH:mm:ss' or <code>null</code> if the timestamp is <= 0.
     */
    public static String formatTimestampWithSeconds(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'EEE' or <code>null</code> if the timestamp is <= 0.
     */
    public static String formatDayOfWeek(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param o       A timestamp string as 'HH:mm' or '-'.
     * @param minutes The minutes to increment/decrement from timestamp.
     * @return Timestamp string as 'HH:mm', modified with the given minutes or the current time + 60 minutes if the
     * given timestamp was '-' or the given timestamp couldn't be parsed.
     */
    public static String shiftTime(Object o, int minutes) {
        String timestamp = o.toString();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date;
        if (timestamp != null && timestamp.length() >= 1 && timestamp.startsWith("-")) {
            date = new Date();
            date.setTime(date.getTime() + 60 * 60000);
        } else {
            try {
                date = sdf.parse(timestamp);
                date.setTime(date.getTime() + minutes * 60000);
            } catch (ParseException ex) {
                date = new Date();
                date.setTime(date.getTime() + 60 * 60000);
            }
        }
        return (sdf.format(date));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @SafeVarargs
    public static <T> List<T> joinCollections(Collection<T>... collections) {
        if (collections == null || collections.length == 0) {
            return Collections.emptyList();
        }

        List<T> newCollection = null;

        for (Collection<T> collection : collections) {
            if (collection == null) {
                continue;
            }

            if (newCollection == null) {
                newCollection = new ArrayList<>(collection);
            } else {
                newCollection.addAll(collection);
            }
        }
        return newCollection;
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
}
