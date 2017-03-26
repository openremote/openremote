/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import elemental.json.*;

import java.util.HashMap;
import java.util.Map;

public class JsonUtil {

    @SuppressWarnings("unchecked")
    public static <T> T convert(ObjectMapper objectMapper, Class<T> targetType, Object object) {
        Map<String, Object> props = objectMapper.convertValue(object, Map.class);
        return objectMapper.convertValue(props, targetType);
    }

    /**
     * Compare two {@link JsonValue} instances by value. Use this
     * and {@link #hashCode} instead of {@link JsonValue#jsEquals}.
     */
    public static boolean equals(JsonValue a, JsonValue b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;
        if (b == null)
            return false;
        if (a instanceof JsonObject && b instanceof JsonObject) {
            Map<String, JsonValue> mapA = asMap((JsonObject) a);
            Map<String, JsonValue> mapB = asMap((JsonObject) b);
            if (!mapA.keySet().equals(mapB.keySet()))
                return false;
            for (Map.Entry<String, JsonValue> entry : mapA.entrySet()) {
                JsonValue mapAValue = entry.getValue();
                JsonValue mapBValue = mapB.get(entry.getKey());
                if (!equals(mapAValue, mapBValue)) {
                    return false;
                }
            }
            return true;
        } else if (a instanceof JsonArray && b instanceof JsonArray) {
            JsonArray arrayA = (JsonArray) a;
            JsonArray arrayB = (JsonArray) b;
            if (arrayA.length() != arrayB.length())
                return false;
            for (int i = 0; i < arrayA.length(); i++) {
                JsonValue valueA = arrayA.get(i);
                JsonValue valueB = arrayB.get(i);
                if (!equals(valueA, valueB)) {
                    return false;
                }
            }
            return true;
        } else if (a instanceof JsonNull && b instanceof JsonNull) {
            return true;
        } else if (a instanceof JsonBoolean && b instanceof JsonBoolean) {
            JsonBoolean booleanA = (JsonBoolean) a;
            JsonBoolean booleanB = (JsonBoolean) b;
            return booleanA.getBoolean() == booleanB.getBoolean();
        } else if (a instanceof JsonNumber && b instanceof JsonNumber) {
            JsonNumber numberA = (JsonNumber) a;
            JsonNumber numberB = (JsonNumber) b;
            return numberA.getNumber() == numberB.getNumber();
        } else if (a instanceof JsonString && b instanceof JsonString) {
            JsonString stringA = (JsonString) a;
            JsonString stringB = (JsonString) b;
            return stringA.getString().equals(stringB.getString());
        }
        return false;
    }

    public static Map<String, JsonValue> asMap(JsonObject jsonObject) {
        Map<String, JsonValue> map = new HashMap<>();
        for (int i = 0; i < jsonObject.keys().length; i++) {
            String key = jsonObject.keys()[i];
            JsonValue value = jsonObject.get(key);
            map.put(key, value);
        }
        return map;
    }

    public static int hashCode(JsonValue jsonValue) {
        int result = 31;
        if (jsonValue == null)
            return result;
        if (jsonValue instanceof JsonObject) {
            result = result * 3;
            Map<String, JsonValue> map = asMap((JsonObject) jsonValue);
            for (Map.Entry<String, JsonValue> entry : map.entrySet()) {
                result = result * 3 + entry.getKey().hashCode();
                result = result * 3 + hashCode(entry.getValue());
            }
        } else if (jsonValue instanceof JsonArray) {
            JsonArray array = (JsonArray) jsonValue;
            result = result * 5;
            for (int i = 0; i < array.length(); i++) {
                result = result * 5 + (i + hashCode(array.get(i)));
            }
        } else if (jsonValue instanceof JsonBoolean) {
            JsonBoolean booleanA = (JsonBoolean) jsonValue;
            result = result * 7 + (booleanA.getBoolean() ? 1 : 0);
        } else if (jsonValue instanceof JsonNumber) {
            JsonNumber number = (JsonNumber) jsonValue;
            result = result * 11 + Double.hashCode(number.getNumber());
        } else if (jsonValue instanceof JsonString) {
            JsonString string = (JsonString) jsonValue;
            result = result * 13 + string.getString().hashCode();
        }
        return result;
    }
}
