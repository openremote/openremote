/*
 * Copyright 2021, OpenRemote Inc.
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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

public class JSONSchemaUtil {

    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE)
    static class PatternPropertiesAnyKeyAnyType {}
    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE)
    static class PatternPropertiesSimpleKeyAnyType {}
    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_ANY_TYPE)
    static class AnyType {}

    private JSONSchemaUtil() {}

    public static final String SCHEMA_SUPPLIER_NAME_ANY_TYPE = "anyType";
    public static final String SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE = "patternPropertiesAnyKeyAnyType";
    public static final String SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE = "patternPropertiesSimpleKeyAnyType";
    public static final String PATTERN_PROPERTIES_MATCH_ANY = ".+";
    public static final String PATTERN_PROPERTIES_MATCH_SIMPLE = "^[a-zA-Z][a-zA-Z0-9]*";
    public static final String TYPE_NULL = "null";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_OBJECT = "object";
    public static final String[] TYPES_ALL = new String[]{
        TYPE_NULL,
        TYPE_NUMBER,
        TYPE_INTEGER,
        TYPE_BOOLEAN,
        TYPE_STRING,
        TYPE_ARRAY,
        TYPE_OBJECT
    };

    public static JsonNode getSchemaPatternPropertiesAnyKeyAnyType() {
        return getSchemaPatternProperties(PATTERN_PROPERTIES_MATCH_ANY, TYPES_ALL);
    }

    public static JsonNode getSchemaPatternPropertiesSimpleKeyAnyType() {
        return getSchemaPatternProperties(PATTERN_PROPERTIES_MATCH_SIMPLE, TYPES_ALL);
    }

    public static JsonNode getSchemaPatternPropertiesAnyType(String keyPattern) {
        return getSchemaPatternProperties(keyPattern, TYPES_ALL);
    }

    public static JsonNode getSchemaPatternProperties(String keyPattern, String...types) {
        ObjectNode node = ValueUtil.JSON.createObjectNode();
        node.put("type", "object");
        ObjectNode patternNode = node.putObject("patternProperties").putObject(keyPattern);
        patternNode.set("type", getSchemaType(false, types));
        return node;
    }

    public static JsonNode getSchemaType(boolean wrapped, String...types) {
        JsonNode typesNode;

        if (types.length == 1) {
            typesNode = new TextNode(types[0]);
        } else {
            ArrayNode arrNode = ValueUtil.JSON.createArrayNode();
            Arrays.stream(types).forEach(arrNode::add);
            typesNode = arrNode;
        }

        return wrapped ? ValueUtil.JSON.createObjectNode().set("type", typesNode) : typesNode;
    }

    public static JsonSchemaConfig getJsonSchemaConfig() {
        return JsonSchemaConfig.create(
            false,
            Optional.empty(),
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            Collections.emptyMap(),
            false,
            Collections.emptySet(),
            new HashMap<Class<?>, Class<?>>(){{
                put(Object.class, AnyType.class);
                put(ObjectNode.class, PatternPropertiesSimpleKeyAnyType.class);
            }},
            new HashMap<String, Supplier<JsonNode>>() {{
                put(SCHEMA_SUPPLIER_NAME_ANY_TYPE, () -> getSchemaType(true, TYPES_ALL));
                put(SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE, JSONSchemaUtil::getSchemaPatternPropertiesAnyKeyAnyType);
                put(SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE, JSONSchemaUtil::getSchemaPatternPropertiesSimpleKeyAnyType);
            }},
            null,
            false,
            null,
            null,
            true
        ).withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);
    }
}
