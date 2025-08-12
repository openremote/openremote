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
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import org.reflections.Reflections;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JSONSchemaUtil {

    public static class SchemaNodeFactory {

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

        private static final JsonNodeFactory NF = JsonNodeFactory.instance;

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
        String keyword() default "label"; // Expected to be "label" by JSON Forms
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaDescription {
        String keyword() default "description";
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaFormat {
        String keyword() default "format";
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaDefault {
        String keyword() default "default";
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaExamples {
        String keyword() default "examples";
        String[] value();
    }

    public static class CustomModule implements Module {

        private static final ConcurrentHashMap<Class<?>, List<ResolvedType>> subtypeCache = new ConcurrentHashMap<>();
        private static final Reflections reflections = new Reflections("org.openremote");

        @Override
        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {

            // Class type remapping
            builder.forTypesInGeneral()
                .withCustomDefinitionProvider((resolvedType, context) -> {
                    Class<?> erasedType = resolvedType.getErasedType();
                    if (erasedType.equals(Object.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaType(SchemaNodeFactory.TYPES_ALL));
                    }
                    if (erasedType.equals(ObjectNode.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaPatternPropertiesSimpleKeyAnyType());
                    }
                    return null;
                });

            // Class subtype resolver for extended classes
            builder.forTypesInGeneral().withSubtypeResolver(CustomModule::findSubtypes);

            // Custom annotations injection
            builder.forTypesInGeneral().withTypeAttributeOverride((attrs, typeScope, context) -> {
                Class<?> erasedType = typeScope.getType().getErasedType();
                ObjectMapper mapper = context.getGeneratorConfig().getObjectMapper();

                ObjectNode targetNode;

                // If there is an allOf array, inject into the first object inside it to allow for cleanup with `Option.ALLOF_CLEANUP_AT_THE_END`
                JsonNode allOfNode = attrs.get("allOf");
                if (allOfNode instanceof ArrayNode allOf && !allOf.isEmpty()) {
                    targetNode = (ObjectNode) allOf.get(0);
                } else {
                    targetNode = attrs;
                }

                applyTypeAnnotation(erasedType, JsonSchemaTitle.class, targetNode, mapper);
                applyTypeAnnotation(erasedType, JsonSchemaDescription.class, targetNode, mapper);
                applyTypeAnnotation(erasedType, JsonSchemaFormat.class, targetNode, mapper);
                applyTypeAnnotation(erasedType, JsonSchemaDefault.class, targetNode, mapper);
                applyTypeAnnotation(erasedType, JsonSchemaExamples.class, targetNode, mapper);
            });

            builder.forFields().withInstanceAttributeOverride((attrs, fieldScope, context) -> {
                ObjectMapper mapper = context.getGeneratorConfig().getObjectMapper();

                applyFieldAnnotation(fieldScope, JsonSchemaTitle.class, attrs, mapper);
                applyFieldAnnotation(fieldScope, JsonSchemaDescription.class, attrs, mapper);
                applyFieldAnnotation(fieldScope, JsonSchemaFormat.class, attrs, mapper);
                applyFieldAnnotation(fieldScope, JsonSchemaDefault.class, attrs, mapper);
                applyFieldAnnotation(fieldScope, JsonSchemaExamples.class, attrs, mapper);
            });
        }

        private static <A extends Annotation> void applyTypeAnnotation(
                Class<?> type,
                Class<A> annotationClass,
                ObjectNode schema,
                ObjectMapper mapper
        ) {
            A annotation = type.getDeclaredAnnotation(annotationClass);
            if (annotation == null) return;

            try {
                String keyword = (String) annotationClass.getMethod("keyword").invoke(annotation);
                Method valueMethod = annotationClass.getMethod("value");
                Class<?> returnType = valueMethod.getReturnType();
                Object value = valueMethod.invoke(annotation);

                if (returnType.isArray()) {
                    ArrayNode arrayNode = schema.putArray(keyword);
                    for (Object element : (Object[]) value) {
                        arrayNode.add(parseJsonOrString(mapper, element.toString()));
                    }
                } else {
                    schema.set(keyword, parseJsonOrString(mapper, value.toString()));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to apply annotation " + annotationClass.getSimpleName(), e);
            }
        }

        private static <A extends Annotation> void applyFieldAnnotation(
                FieldScope fieldScope,
                Class<A> annotationClass,
                ObjectNode schema,
                ObjectMapper mapper
        ) {
            A annotation = fieldScope.getAnnotation(annotationClass);
            if (annotation == null) return;

            try {
                String keyword = (String) annotationClass.getMethod("keyword").invoke(annotation);
                Method valueMethod = annotationClass.getMethod("value");
                Class<?> returnType = valueMethod.getReturnType();
                Object value = valueMethod.invoke(annotation);

                if (returnType.isArray()) {
                    ArrayNode arrayNode = schema.putArray(keyword);
                    for (Object element : (Object[]) value) {
                        arrayNode.add(parseJsonOrString(mapper, element.toString()));
                    }
                } else {
                    schema.set(keyword, parseJsonOrString(mapper, value.toString()));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to apply annotation " + annotationClass.getSimpleName(), e);
            }
        }

        private static JsonNode parseJsonOrString(ObjectMapper mapper, String input) {
            try {
                return mapper.readTree(input);
            } catch (Exception e) {
                return new TextNode(input);
            }
        }

//        private void applyTranslations() {
//            // TODO: apply translations on the schema using the FQCN as translation key and extract the keys to the
//            // translation files during compilation by implementing an i18n processor.
//        }

        private static List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
            Class<?> rawType = declaredType.getErasedType();

            // Only attempt subtype discovery if @JsonTypeInfo is present
            if (!rawType.isAnnotationPresent(JsonTypeInfo.class)) {
                return null;
            }

//            // Only attempt subtype discovery if @JsonTypeInfo is present and no subtypes have been defined
//            if (!rawType.isAnnotationPresent(JsonTypeInfo.class) || rawType.isAnnotationPresent(JsonSubTypes.class)) {
//                return null;
//            }

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
            Option.ADDITIONAL_FIXED_TYPES,
            Option.FLATTENED_ENUMS,
            Option.VALUES_FROM_CONSTANT_FIELDS,
            Option.PUBLIC_NONSTATIC_FIELDS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
            Option.ALLOF_CLEANUP_AT_THE_END,
            // Option.DEFINITIONS_FOR_ALL_OBJECTS,
            Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END
        ))
        .with(new JacksonModule(
            JacksonOption.ALWAYS_REF_SUBTYPES,
            JacksonOption.INLINE_TRANSFORMED_SUBTYPES,
            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED
        ))
        .with(new JakartaValidationModule(
            JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
            JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        ))
        .with(new CustomModule())
        .build();
    }
}
