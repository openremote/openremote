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


import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.CustomDefinition.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import org.reflections.Reflections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JSONSchemaUtil {
//
//    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE)
//    static class PatternPropertiesAnyKeyAnyType {}
//    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE)
//    static class PatternPropertiesSimpleKeyAnyType {}
//    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_ANY_TYPE)
//    static class AnyType {}
//    @JsonSchemaInject(jsonSupplierViaLookup = JSONSchemaUtil.SCHEMA_SUPPLIER_NAME_STRING_TYPE)
//    static class StringType {}
//
//    private JSONSchemaUtil() {}
//
//    public static final String SCHEMA_SUPPLIER_NAME_ANY_TYPE = "anyType";
//    public static final String SCHEMA_SUPPLIER_NAME_STRING_TYPE = "stringType";
//    public static final String SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE = "patternPropertiesAnyKeyAnyType";
//    public static final String SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE = "patternPropertiesSimpleKeyAnyType";
//    public static final String PATTERN_PROPERTIES_MATCH_ANY = ".+";
//    public static final String PATTERN_PROPERTIES_MATCH_SIMPLE = "^[a-zA-Z][a-zA-Z0-9]*";
//    public static final String TYPE_NULL = "null";
//    public static final String TYPE_NUMBER = "number";
//    public static final String TYPE_INTEGER = "integer";
//    public static final String TYPE_BOOLEAN = "boolean";
//    public static final String TYPE_STRING = "string";
//    public static final String TYPE_ARRAY = "array";
//    public static final String TYPE_OBJECT = "object";
//    public static final String[] TYPES_ALL = new String[]{
//        TYPE_NULL,
//        TYPE_NUMBER,
//        TYPE_INTEGER,
//        TYPE_BOOLEAN,
//        TYPE_STRING,
//        TYPE_ARRAY,
//        TYPE_OBJECT
//    };
//
//    public static JsonNode getSchemaPatternPropertiesAnyKeyAnyType() {
//        return getSchemaPatternProperties(PATTERN_PROPERTIES_MATCH_ANY, TYPES_ALL);
//    }
//
//    public static JsonNode getSchemaPatternPropertiesSimpleKeyAnyType() {
//        return getSchemaPatternProperties(PATTERN_PROPERTIES_MATCH_SIMPLE, TYPES_ALL);
//    }
//
//    public static JsonNode getSchemaPatternPropertiesAnyType(String keyPattern) {
//        return getSchemaPatternProperties(keyPattern, TYPES_ALL);
//    }
//
//    public static JsonNode getSchemaPatternProperties(String keyPattern, String...types) {
//        ObjectNode node = ValueUtil.JSON.createObjectNode();
//        node.put("type", "object");
//        ObjectNode patternNode = node.putObject("patternProperties").putObject(keyPattern);
//        patternNode.set("type", getSchemaType(false, types));
//        return node;
//    }
//
//    public static JsonNode getSchemaType(boolean wrapped, String...types) {
//        JsonNode typesNode;
//
//        if (types.length == 1) {
//            typesNode = new TextNode(types[0]);
//        } else {
//            ArrayNode arrNode = ValueUtil.JSON.createArrayNode();
//            Arrays.stream(types).forEach(arrNode::add);
//            typesNode = arrNode;
//        }
//
//        return wrapped ? ValueUtil.JSON.createObjectNode().set("type", typesNode) : typesNode;
//    }
//
//    public static JsonSchemaConfig getJsonSchemaConfig() {
//        return JsonSchemaConfig.create(
//            false,                  // autoGenerateTitleForProperties
//            Optional.empty(),       // defaultArrayFormat
//            false,                  // useOneOfForOption
//            false,                  // useOneOfForNullables
//            false,                  // usePropertyOrdering
//            false,                  // hidePolymorphismTypeProperty
//            false,                  // disableWarnings
//            false,                  // useMinLengthForNotNull
//            false,                  // useTypeIdForDefinitionName
//            Collections.emptyMap(), // customType2FormatMapping
//            false,                  // useMultipleEditorSelectViaProperty
//            Collections.emptySet(), // uniqueItemClasses
//            Map.of(
//                Object.class, AnyType.class,
//                ObjectNode.class, PatternPropertiesSimpleKeyAnyType.class
//            ),                      // classTypeReMapping
//            Map.of(
//                SCHEMA_SUPPLIER_NAME_ANY_TYPE, () -> getSchemaType(true, TYPES_ALL),
//                SCHEMA_SUPPLIER_NAME_STRING_TYPE, () -> getSchemaType(true, TYPE_STRING),
//                SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE, JSONSchemaUtil::getSchemaPatternPropertiesAnyKeyAnyType,
//                SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE, JSONSchemaUtil::getSchemaPatternPropertiesSimpleKeyAnyType
//            ),                      // jsonSuppliers
//            null,                   // subclassesResolver
//            false,                  // failOnUnknownProperties
//            null,                   // javaxValidationGroups
//            null,                   // jsonSchemaDraft
//            true                    // disableRefTitle
//        ).withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);
//    }


    public static class SchemaNodeFactory {

        private static final JsonNodeFactory NF = JsonNodeFactory.instance;

        public static final String[] TYPES_ALL = {
            "null", "number", "integer", "boolean", "string", "array", "object"
        };

        public static ObjectNode getSchemaPatternProperties(String keyPattern, String... types) {
            ObjectNode node = NF.objectNode();
            node.put("type", "object");
            ObjectNode patternNode = node.putObject("patternProperties").putObject(keyPattern);
            patternNode.set("type", getTypesNode(Arrays.asList(types)));
            return node;
        }

        public static ObjectNode getSchemaPatternPropertiesAnyKeyAnyType() {
            return getSchemaPatternProperties(".+", TYPES_ALL);
        }

        public static ObjectNode getSchemaPatternPropertiesSimpleKeyAnyType() {
            return getSchemaPatternProperties("^[a-zA-Z][a-zA-Z0-9]*", TYPES_ALL);
        }

        public static JsonNode getTypesNode(List<String> types) {
            if (types.size() == 1) {
                return NF.textNode(types.getFirst());
            }
            ArrayNode arr = NF.arrayNode();
            types.forEach(arr::add);
            return arr;
        }

        public static ObjectNode getSchemaType(String... types) {
            return NF.objectNode().set("type", getTypesNode(Arrays.asList(types)));
        }
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaTitle {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaDescription {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaFormat {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaDefault {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaExamples {
        String[] value();
    }

    public static class CustomModule implements Module {

        private static final ConcurrentHashMap<Class<?>, List<ResolvedType>> subtypeCache = new ConcurrentHashMap<>();
        private static final Reflections reflections = new Reflections("org.openremote");

        @Override
        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {

            // Class type remapping equivalents
            builder.forTypesInGeneral()
                .withCustomDefinitionProvider((javaType, context) -> {
                    Class<?> erasedType = javaType.getErasedType();
                    if (erasedType.equals(Object.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaType(SchemaNodeFactory.TYPES_ALL));
                    }
                    if (erasedType.equals(ObjectNode.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaPatternPropertiesSimpleKeyAnyType());
                    }
                    return null;
                });

            // Class subtype resolver for extended classes
            builder.forTypesInGeneral()
                .withSubtypeResolver((declaredType, context) -> findSubtypes(declaredType, context));

            // Defaults + Examples injection from annotations
            builder.forFields().withInstanceAttributeOverride((attrs, scope) -> {
                applyTitle(scope.getAnnotation(JsonSchemaTitle.class), attrs);
                applyDescription(scope.getAnnotation(JsonSchemaDescription.class), attrs);
                applyFormat(scope.getAnnotation(JsonSchemaFormat.class), attrs);
                applyDefaults(scope.getAnnotation(JsonSchemaDefault.class), attrs);
                applyExamples(scope.getAnnotation(JsonSchemaExamples.class), attrs);
            });

            builder.forTypesInGeneral().withTypeAttributeOverride((attrs, scope, context) -> {
                applyTitle(scope.getType().getErasedType().getAnnotation(JsonSchemaTitle.class), attrs);
                applyDescription(scope.getType().getErasedType().getAnnotation(JsonSchemaDescription.class), attrs);
                applyFormat(scope.getType().getErasedType().getAnnotation(JsonSchemaFormat.class), attrs);
                applyDefaults(scope.getType().getErasedType().getAnnotation(JsonSchemaDefault.class), attrs);
                applyExamples(scope.getType().getErasedType().getAnnotation(JsonSchemaExamples.class), attrs);
            });
        }

        private void applyTitle(JsonSchemaTitle title, ObjectNode attrs) {
            if (title != null && !title.value().isEmpty()) {
                attrs.set("title", new TextNode(title.value()));
            }
        }

        private void applyDescription(JsonSchemaDescription description, ObjectNode attrs) {
            if (description != null && !description.value().isEmpty()) {
                attrs.set("description", new TextNode(description.value()));
            }
        }

        private void applyFormat(JsonSchemaFormat format, ObjectNode attrs) {
            if (format != null && !format.value().isEmpty()) {
                attrs.set("format", new TextNode(format.value()));
            }
        }

        private void applyDefaults(JsonSchemaDefault defaults, ObjectNode attrs) {
            if (defaults != null && !defaults.value().isEmpty()) {
                attrs.set("default", new TextNode(defaults.value()));
            }
        }

        private void applyExamples(JsonSchemaExamples examples, ObjectNode attrs) {
            if (examples != null && examples.value().length > 0) {
                ArrayNode arr = attrs.arrayNode();
                Arrays.stream(examples.value()).forEach(arr::add);
                attrs.set("examples", arr);
            }
        }

        private void applyTranslations() {
            // TODO: apply translations on the schema using the FQCN as translation key and extract the keys to the
            // translation files during compilation by implementing an i18n processor.
        }

        private static List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
            Class<?> rawType = declaredType.getErasedType();

            // Only attempt subtype discovery if @JsonTypeInfo is present
            if (!rawType.isAnnotationPresent(JsonTypeInfo.class)) {
                return null;
            }

            // Cached lookup
            return subtypeCache.computeIfAbsent(rawType, baseType -> {
                Set<Class<?>> found = reflections.getSubTypesOf(baseType)
                    .stream()
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .collect(Collectors.toSet());

                return found.stream()
                    .map(sub -> context.getTypeContext().resolveSubtype(declaredType, sub))
                    .collect(Collectors.toList());
            });
        }
    }

    public static SchemaGeneratorConfig getJsonSchemaConfig(ObjectMapper mapper) {
        return new SchemaGeneratorConfigBuilder(mapper, SchemaVersion.DRAFT_7, new OptionPreset(
                Option.SCHEMA_VERSION_INDICATOR,
                Option.FLATTENED_ENUMS,
//                Option.VALUES_FROM_CONSTANT_FIELDS,
                Option.VALUES_FROM_CONSTANT_FIELDS,
                Option.PUBLIC_NONSTATIC_FIELDS,
                Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
                Option.ALLOF_CLEANUP_AT_THE_END,
                Option.DEFINITIONS_FOR_ALL_OBJECTS,
                Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END
        )).with(new JacksonModule(
                JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                JacksonOption.ALWAYS_REF_SUBTYPES,
                JacksonOption.INLINE_TRANSFORMED_SUBTYPES
        )).with(new CustomModule()).build();
    }
}
