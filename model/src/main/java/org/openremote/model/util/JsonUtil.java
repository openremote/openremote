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

import com.google.gwt.core.shared.GWT;
import elemental.json.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Elemental issues:
 * <p>
 * <ul>
 * <li>Equality checking for {@link JsonObject} does not use {@link JsonValue#jsEquals(JsonValue)}
 * {@link JsonValue#jsEquals(JsonValue)} isn't null safe
 * <li>Coerces types into other types implicitly
 * <li>Returns primitives rather than objects, relates to coercion (e.g. {@link JsonValue#asBoolean()})
 * <li>Strange boxing/unboxing behaviour which means a primitive isn't always recognised as the correct type
 * when calling {@link JsonValue#getType()}
 * <li>For primitives {@link JsonObject#get(String)} has different behaviour to for example
 * {@link JsonObject#getBoolean(String)}
 * <li>JsonValue of primitive with default value is null (on client side) <a href="https://github.com/gwtproject/gwt/issues/9484">
 *     https://github.com/gwtproject/gwt/issues/9484</a>
 * <li>Primitives on client side will cause CCS exception if casting to JsonValue (this affects {@link Optional#get()})
 * <p>
 *
 * When working with JsonValue it is advised to use the {@link #replaceJsonNull(JsonValue)} methods which eliminate
 * JsonNull and instead use plain old null, method returns an optional. If JsonNull didn't exist then obviously
 * could just use {@link Optional#ofNullable(Object)} directly.
 *
 */
public class JsonUtil {

    // JRE implementation uses same reference for all JSON Null objects
    // so reference equality checking is fine and works on client also
    protected static JsonNull JSON_NULL = Json.createNull();

    /**
     * Compare two {@link JsonObject} instances and optionally exclude certain keys (if object A contains an ignoredKey
     * but object B doesn't (or vice versa) then the objects can still be considered equal if all non-ignored keys are
     * present and the values of the non ignored keys are considered equal)
     */
    public static boolean equals(JsonObject a, JsonObject b, List<String> ignoreKeys) {
        if (ignoreKeys == null || ignoreKeys.size() == 0) {
            return equals(a, b);
        }

        Map<String, JsonValue> mapA = asMap(a);
        Map<String, JsonValue> mapB = asMap(b);
        List<String> mapAKeys = mapA.keySet().stream().filter(k -> !ignoreKeys.contains(k)).collect(Collectors.toList());
        List<String> mapBKeys = mapB.keySet().stream().filter(k -> !ignoreKeys.contains(k)).collect(Collectors.toList());

        if (mapAKeys.size() != mapBKeys.size()) {
            return false;
        }

        if (!mapAKeys.containsAll(mapBKeys)) {
            return false;
        }

        for (Map.Entry<String, JsonValue> entry : mapA.entrySet()) {
            if (ignoreKeys.contains(entry.getKey())) {
                continue;
            }

            JsonValue mapAValue = entry.getValue();
            JsonValue mapBValue = mapB.get(entry.getKey());
            if (!equals(mapAValue, mapBValue)) {
                return false;
            }
        }

        return true;
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
            Map<String, JsonValue> mapA = asMap((JsonObject)a);
            Map<String, JsonValue> mapB = asMap((JsonObject)b);
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

    public static boolean isOfType(JsonValue jsonValue, JsonType type) {
        return type != null && getJsonValueType(jsonValue) == type;
    }

    public static boolean isOfTypeString(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.STRING);
    }

    public static boolean isOfTypeNumber(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.NUMBER);
    }

    public static boolean isOfTypeBoolean(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.BOOLEAN);
    }

    public static boolean isOfTypeJsonArray(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.ARRAY);
    }

    public static boolean isOfTypeJsonObject(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.OBJECT);
    }

    public static boolean isOfTypeDecimal(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.NUMBER) && !isInteger(jsonValue.asNumber());
    }

    public static boolean isOfTypeInteger(JsonValue jsonValue) {
        return isOfType(jsonValue, JsonType.NUMBER) && isInteger(jsonValue.asNumber());
    }

    public static boolean isTrue(JsonValue jsonValue) {
        return asBoolean(jsonValue)
            .orElse(false);
    }

    public static boolean isFalse(JsonValue jsonValue) {
        return !isTrue(jsonValue);
    }

    public static boolean isInteger(JsonValue jsonValue) {
        return asDecimal(jsonValue)
            .map(JsonUtil::isInteger)
            .orElse(false);
    }

    // TODO: Should this only return a value for JsonType.STRING values or should it work for any data type?
    public static Optional<String> asString(JsonValue value) {
        JsonValue jsonValue = replaceJsonNull(value);
        return Optional.ofNullable(jsonValue != null ? jsonValue.asString() : null);
    }

    public static Optional<String> asString(Optional<JsonValue> value) {
        return asString(unwrapJsonValue(value));
    }

    public static Optional<Double> asDecimal(JsonValue value) {
        return Optional.ofNullable(isOfTypeNumber(value) ? ((JsonNumber)value).getNumber() : null);
    }

    public static Optional<Double> asDecimal(Optional<JsonValue> value) {
        return asDecimal(unwrapJsonValue(value));
    }

    public static Optional<Boolean> asBoolean(JsonValue value) {
        return Optional.ofNullable(isOfTypeBoolean(value) ? ((JsonBoolean)value).getBoolean() : null);
    }

    public static Optional<Boolean> asBoolean(Optional<JsonValue> value) {
        return asBoolean(unwrapJsonValue(value));
    }

    public static Optional<JsonArray> asJsonArray(JsonValue value) {
        return Optional.ofNullable(isOfTypeJsonArray(value) ? (JsonArray)value : null);
    }

    public static Optional<JsonArray> asJsonArray(Optional<JsonValue> value) {
        return asJsonArray(unwrapJsonValue(value));
    }

    public static Optional<JsonObject> asJsonObject(JsonValue value) {
        return Optional.ofNullable(isOfTypeJsonObject(value) ? (JsonObject)value : null);
    }

    public static Optional<JsonObject> asJsonObject(Optional<JsonValue> value) {
        return asJsonObject(unwrapJsonValue(value));
    }

    public static Optional<Integer> asInteger(JsonValue value) {
        return asDecimal(value)
            .map(dbl -> isInteger(dbl) ? (int)Math.floor(dbl) : null);
    }

    public static Optional<Integer> asInteger(Optional<JsonValue> value) {
        return asInteger(unwrapJsonValue(value));
    }

    public static String asJson(JsonValue value) {
        return value == null ? "" : value.toJson();
    }

    public static String asJson(Optional<JsonValue> value) {
        return asJson(unwrapJsonValue(value));
    }

    protected static boolean isInteger(double value) {
        return (value == Math.floor(value)) && !Double.isInfinite(value);
    }

    // --------------------------------------------
    // BELOW ARE JUST TO DEAL WITH CRAPPY ELEMENTAL
    // --------------------------------------------

    public static JsonValue asJsonValue(Boolean bool) {
        return bool == null ? null : Json.create(bool);
    }

    public static JsonValue asJsonValue(String str) {
        return str == null ? null : Json.create(str);
    }

    public static JsonValue asJsonValue(Double dbl) {
        return dbl == null ? null : Json.create(dbl);
    }

    public static JsonValue asJsonValue(Integer integer) {
        return integer == null ? null : Json.create(integer);
    }

    /**
     * Prevent JsonNull instances from being used; when the type is removed
     * from elemental then this will be un-necessary.
     * <p>
     * All instances of JsonNull have been removed from the codebase
     * but this is here in case others get introduced.
     */
    // TODO: Fix elemental and remove this
    public static JsonValue replaceJsonNull(JsonValue jsonValue) {
        return jsonValue == JSON_NULL ? null : jsonValue;
    }

    /**
     * Fixes issues relating to un-boxed primitives on client side
     * should be called by client side code rather than using the
     * JsonValue directly.
     */
    // TODO: Fix elemental and remove this
    public static JsonValue sanitizeJsonValue(JsonValue jsonValue) {
        return fixBoxingUnBoxing(jsonValue);
    }

    /**
     * Fixes issues relating to un-boxed primitives on client side
     * should be called by client side code rather than using the
     * JsonValue directly.
     */
    // TODO: Fix elemental and remove this
    public static JsonValue sanitizeJsonValue(Optional<JsonValue> jsonValue) {
        return unwrapJsonValue(jsonValue);
    }

    /**
     * This ensures that primitives are correctly boxed into JsonValue type
     */
    // TODO: Fix elemental and remove this
    protected static JsonValue fixBoxingUnBoxing(Object obj) {
        if (obj == JSON_NULL) {
            return null;
        }

        JsonValue jsonValue = null;

        if (obj instanceof JsonValue) {
            // Server side will only ever reach here
            jsonValue = (JsonValue)obj;
        } else if(obj instanceof String) {
            // Elemental doesn't box strings and primitive
            // strings cannot be cast to JsonValue so box
            jsonValue = boxString((String)obj);
        } else if (obj instanceof Double) {
            jsonValue = Json.create((Double)obj);
        } else if (obj instanceof Boolean) {
            jsonValue = Json.create((Boolean)obj);
        }

        return jsonValue;
    }

    /**
     * JsJsonValue implementation is not tolerant of boxed primitives for
     * getType call (it assumes everything is un-boxed).
     * <p>
     * This replacement method is tolerant of boxed primitives.
     */
    // TODO: Fix elemental and remove this
    public static JsonType getJsonValueType(JsonValue jsonValue) {
        jsonValue = sanitizeJsonValue(jsonValue);

        if (jsonValue == null) {
            return JsonType.NULL;
        }

        if (!GWT.isScript()) {
            return jsonValue.getType();
        } else {
            String jsType = getJsNativeType(jsonValue);
            switch (jsType) {
                case "number":
                    return JsonType.NUMBER;
                case "boolean":
                    return JsonType.BOOLEAN;
                case "string":
                    return JsonType.STRING;
                default:
                    return jsonValue.getType();
            }
        }
    }

    // TODO: Fix elemental and remove this
    @SuppressWarnings({"ConstantConditions", "OptionalIsPresent"})
    protected static JsonValue unwrapJsonValue(Optional<JsonValue> jsonValueOptional) {
        return jsonValueOptional.isPresent() ? fixBoxingUnBoxing(jsonValueOptional.get()) : null;
    }

    // TODO: Fix elemental and remove this
    @SuppressWarnings("EqualityComparisonWithCoercionJS")
    private native static String getJsNativeType(Object o) /*-{
        var type = typeof o;
        if (type == 'object') {
            return typeof (o.valueOf());
        } else {
            return type;
        }
    }-*/;

    // TODO: Fix elemental and remove this
    private native static JsonString boxString(String str) /*-{
        return Object(str);
    }-*/;
}
