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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JsonSubTypesResolver;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import org.reflections.Reflections;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
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
        public static final String[] TYPES_ALL = {
            "null", "number", "integer", "boolean", "string", "array", "object"
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
            return getSchemaPatternProperties(PATTERN_PROPERTIES_MATCH_ANY, TYPES_ALL);
        }

        public static ObjectNode getSchemaPatternPropertiesSimpleKeyAnyType() {
            return getSchemaPatternProperties(PATTERN_PROPERTIES_MATCH_SIMPLE, TYPES_ALL);
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
        String keyword() default "title";
        String value();
        /* Whether to put the title on the root of the schema even when the class is wrapped in an array. */
        boolean container() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaDescription {
        String keyword() default "description";
        String value();
        boolean container() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaFormat {
        String keyword() default "format";
        String value();
        boolean container() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaDefault {
        String keyword() default "default";
        String value();
        boolean container() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaExamples {
        String keyword() default "examples";
        String[] value();
        boolean container() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaTypeRemap {
        Class<?> type();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JsonSchemaSupplier {
        String supplier();
    }

    public static class CustomModule implements Module {

        private static final ConcurrentHashMap<Class<?>, List<ResolvedType>> subtypeCache = new ConcurrentHashMap<>();
        private static final Reflections reflections = new Reflections("org.openremote");

        @Override
        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {

            // Apply additionalProperties true to all object values
            builder.forTypesInGeneral().withTypeAttributeOverride((attrs, typeScope, context) -> {
                if (attrs.has("type") && Objects.equals(attrs.get("type").textValue(), "object")) {
                    attrs.put("additionalProperties", Boolean.TRUE);
                }
            });

            // General direct class type remapping
            builder.forTypesInGeneral()
                .withCustomDefinitionProvider((resolvedType, context) -> {
                    Class<?> erasedType = resolvedType.getErasedType();
                    // Does not behave like before where this is the fallback if a class could not be resolved
                    if (erasedType.equals(Object.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaType(SchemaNodeFactory.TYPES_ALL));
                    }
                    if (erasedType.equals(ObjectNode.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaPatternPropertiesSimpleKeyAnyType());
                    }
                    return null;
                });

            // Field type remapping
            builder.forFields()
                // direct class to class mapping through annotations
                .withTargetTypeOverridesResolver(this::remapFieldType)
                // remapping using supplier through annotations
                .withCustomDefinitionProvider((fieldScope, context) -> {
                    JsonSchemaSupplier ann = fieldScope.getAnnotation(JsonSchemaSupplier.class);
                    if (ann != null) {
                        try {
                            switch (ann.getClass().getMethod("supplier").invoke(ann).toString()) {
                                case SchemaNodeFactory.SCHEMA_SUPPLIER_NAME_ANY_TYPE:
                                    return new CustomPropertyDefinition(SchemaNodeFactory.getSchemaType(SchemaNodeFactory.TYPES_ALL));
                                case SchemaNodeFactory.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE:
                                    return new CustomPropertyDefinition(SchemaNodeFactory.getSchemaPatternPropertiesAnyKeyAnyType());
                                case SchemaNodeFactory.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_SIMPLE_KEY_ANY_TYPE:
                                    return new CustomPropertyDefinition(SchemaNodeFactory.getSchemaPatternPropertiesSimpleKeyAnyType());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to apply " + ann.getClass().getSimpleName(), e);
                        }
                    }
                    return null;
                });

            // Class subtype resolver for abstract classes
            builder.forTypesInGeneral().withCustomDefinitionProvider((resolvedType, context) -> {
                List<ResolvedType> subTypes = findSubtypes(resolvedType, context);
                if (subTypes == null || subTypes.isEmpty()) {
                    return null;
                }

                ObjectNode definition = context.getGeneratorConfig().createObjectNode();
                ArrayNode oneOfArray = definition.withArray(context.getKeyword(SchemaKeyword.TAG_ONEOF));

                for (ResolvedType subType : subTypes) {
                    oneOfArray.add(context.createDefinitionReference(subType));
                }

                return new CustomDefinition(definition, CustomDefinition.DefinitionType.STANDARD, CustomDefinition.AttributeInclusion.NO);
            });

            // Set the default keyword for subtypes so AJV in the frontend can tell jsonforms/core to consider the
            // subtype schema valid
            builder.forTypesInGeneral().withTypeAttributeOverride((attrs, typeScope, context) -> {
                Class<?> erasedType = typeScope.getType().getErasedType();
                if (erasedType.getSuperclass() == Object.class) {
                    return;
                }
                addDefaultToDiscriminator(attrs, context);
            });

            // Effectively disable const generation (on the root of subtypes)
            builder.forTypesInGeneral().withEnumResolver(typeScope -> null);

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

                // Avoids annotation also being applied to the `items` in an array. See https://victools.github.io/jsonschema-generator/#generator-individual-configurations
                if (!fieldScope.isFakeContainerItemScope()) {
                    applyFieldAnnotation(fieldScope, JsonSchemaTitle.class, attrs, mapper);
                    applyFieldAnnotation(fieldScope, JsonSchemaDescription.class, attrs, mapper);
                    applyFieldAnnotation(fieldScope, JsonSchemaFormat.class, attrs, mapper);
                    applyFieldAnnotation(fieldScope, JsonSchemaDefault.class, attrs, mapper);
                    applyFieldAnnotation(fieldScope, JsonSchemaExamples.class, attrs, mapper);
                }
            });
        }

        /**
         * Find and modify the main subtype object under the {@code allOf} keyword of a subtype to add a {@code default}
         * property alongside the {@code const} discriminator property.
         * @param attrs The {@link ObjectNode} representation of the subtype
         * @param context The schema generator {@link SchemaGenerationContext}
         */
        private void addDefaultToDiscriminator(ObjectNode attrs, SchemaGenerationContext context) {
            JsonNode allOfNode = attrs.get(context.getKeyword(SchemaKeyword.TAG_ALLOF));
            if (!(allOfNode instanceof ArrayNode allOf)) {
                return;
            }

            for (JsonNode node : allOf) {
                JsonNode props = node.get(context.getKeyword(SchemaKeyword.TAG_PROPERTIES));
                if (!(props instanceof ObjectNode propsObj)) {
                    continue;
                }

                JsonNode typeNode = propsObj.get(context.getKeyword(SchemaKeyword.TAG_TYPE));
                if (typeNode instanceof ObjectNode typeProp) {

                    String constKey = context.getKeyword(SchemaKeyword.TAG_CONST);
                    String defaultKey = context.getKeyword(SchemaKeyword.TAG_DEFAULT);
                    if (typeProp.has(constKey) && !typeProp.has(defaultKey)) {
                        typeProp.put(defaultKey, typeProp.get(constKey).asText());
                    }
                }
            }
        }

        private List<ResolvedType> remapFieldType(FieldScope fieldScope) {
            JsonSchemaTypeRemap ann = fieldScope.getAnnotation(JsonSchemaTypeRemap.class);
            if (ann != null) {
                try {
                    return Collections.singletonList(fieldScope.getContext().resolve((Type) ann.getClass().getMethod("type").invoke(ann)));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to apply " + ann.getClass().getSimpleName(), e);
                }
            }
            return null;
        }

        private static <A extends Annotation> void applyTypeAnnotation(
                Class<?> type,
                Class<A> annotationClass,
                ObjectNode schema,
                ObjectMapper mapper
        ) {
            A annotation;

            if (type.isArray() && type.getComponentType() != null) {
                annotation = type.getComponentType().getDeclaredAnnotation(annotationClass);

                // TODO: handle titles separately here (make plural instead.)
            } else {
                annotation = type.getDeclaredAnnotation(annotationClass);
            }

            if (annotation != null) {
                applyAnnotation(annotationClass, annotation, schema, mapper);
            }
        }

        private static <A extends Annotation> void applyFieldAnnotation(
                FieldScope fieldScope,
                Class<A> annotationClass,
                ObjectNode schema,
                ObjectMapper mapper
        ) {
            A annotation = fieldScope.getAnnotation(annotationClass);
            if (annotation != null) {
                applyAnnotation(annotationClass, annotation, schema, mapper);
            };
        }

        private static <A extends Annotation> void applyAnnotation(Class<?> annotationClass, A annotation, ObjectNode schema, ObjectMapper mapper) {
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

        private static List<ResolvedType> findSubtypes(ResolvedType resolvedType, SchemaGenerationContext context) {
            Class<?> rawType = resolvedType.getErasedType();

            // Only attempt subtype discovery if @JsonTypeInfo is present
            if (!rawType.isAnnotationPresent(JsonTypeInfo.class)) {
                return null;
            }

            // Reuse Jackson Module JsonSubTypesResolver to get explicitly declared types
            if (rawType.isAnnotationPresent(JsonSubTypes.class)) {
                return new JsonSubTypesResolver().findSubtypes(resolvedType, context);
            }

            // Cached lookup
            return subtypeCache.computeIfAbsent(rawType, baseType -> {
                Set<Class<?>> found = reflections.getSubTypesOf(baseType)
                    .stream()
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .collect(Collectors.toSet());

                return found.stream()
                    .map(sub -> context.getTypeContext().resolveSubtype(resolvedType, sub))
                    .collect(Collectors.toList());
            });
        }
    }

    public static SchemaGeneratorConfig getJsonSchemaConfig(ObjectMapper mapper) {
        return new SchemaGeneratorConfigBuilder(mapper, SchemaVersion.DRAFT_7, new OptionPreset(
            Option.SCHEMA_VERSION_INDICATOR,
            Option.FLATTENED_ENUMS,
            Option.VALUES_FROM_CONSTANT_FIELDS,
            Option.PUBLIC_NONSTATIC_FIELDS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
            Option.ALLOF_CLEANUP_AT_THE_END,
            Option.ADDITIONAL_FIXED_TYPES,
            // Option.DEFINITIONS_FOR_ALL_OBJECTS,
            Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END
        ))
        .with(new JacksonModule(
            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
            JacksonOption.ALWAYS_REF_SUBTYPES,
            // Disable subtype lookup in Jackson module, as we handle this ourselves to replace anyOf with oneOf
            JacksonOption.SKIP_SUBTYPE_LOOKUP
        ))
        .with(new JakartaValidationModule(
            JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS,
            JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED
        ))
        .with(new CustomModule()).build();
    }
}
