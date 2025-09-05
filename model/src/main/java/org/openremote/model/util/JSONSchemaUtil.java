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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.node.*;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.impl.module.SimpleTypeModule;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JsonSubTypesResolver;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import org.reflections.Reflections;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JSONSchemaUtil {

    public static class SchemaNodeFactory {

        public static class AnyType {}

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
            ObjectNode node = NF.objectNode();
            node.put("title", "Any Type");
            node.set("type", getTypesNode(Arrays.asList(types)));
            node.put("additionalProperties", true);
            return node;
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

    private static void setFormat(ObjectNode node, String format) {
        node.put("format", format);
    }

    public static class CustomModule implements Module {

        private static final ConcurrentHashMap<Class<?>, List<ResolvedType>> subtypeCache = new ConcurrentHashMap<>();
        private static final Reflections reflections = new Reflections("org.openremote");

        @Override
        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
            // Set title on root of schema
            JSONSchemaTitleProvider titleProvider = new JSONSchemaTitleProvider();
            builder.forTypesInGeneral()
                .withCustomDefinitionProvider(titleProvider)
                .withTypeAttributeOverride(titleProvider);

            // Primitive types cannot be null thus they are always required
            builder.forFields().withRequiredCheck((f) -> f.getType().getErasedType().isPrimitive());

            // Remap Byte to type integer, see https://github.com/victools/jsonschema-generator/blob/995a71eaf7a9a05cc2e335f8a7821b4a9019fa1b/CHANGELOG.md?plain=1#L530
            builder.with(new SimpleTypeModule().withIntegerType(Byte.class));

            // Apply additionalProperties true to all object values that haven't already set additionalProperties
            builder.forTypesInGeneral().withTypeAttributeOverride((attrs, typeScope, context) -> {
                if (attrs.has("type") && !attrs.has("additionalProperties") && Objects.equals(attrs.get("type").textValue(), "object")) {
                    attrs.put("additionalProperties", Boolean.TRUE);
                }
            });

            // General direct class type remapping
            builder.forTypesInGeneral()
                .withDefinitionNamingStrategy(new DefaultSchemaDefinitionNamingStrategy() {
                    @Override
                    public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
                        TypeContext typeContext = generationContext.getTypeContext();
                        ResolvedType type = key.getType();
                        Class<?> erasedType = type.getErasedType();
                        if (erasedType.equals(Object.class)) {
                            return typeContext.getSimpleTypeDescription(typeContext.resolve(SchemaNodeFactory.AnyType.class));
                        }
                        return typeContext.getSimpleTypeDescription(type);
                    }
                })
                .withCustomDefinitionProvider((resolvedType, context) -> {
                    Class<?> erasedType = resolvedType.getErasedType();
                    // Does not behave like before where this is the fallback if a class could not be resolved
                    if (erasedType.equals(Object.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaType(SchemaNodeFactory.TYPES_ALL));
                    }
                    if (erasedType.equals(ObjectNode.class)) {
                        return new CustomDefinition(SchemaNodeFactory.getSchemaPatternPropertiesSimpleKeyAnyType());
                    }
                    // Value type parameter "Object" is not handled on HashMap<String,Object> by MAP_VALUES_AS_ADDITIONAL_PROPERTIES
                    // TODO: ideally we add a reference to the AnyType definition rather than always inlining
                    if (erasedType.getSuperclass() != null
                        && erasedType.getSuperclass().equals(HashMap.class)
                        && erasedType.getGenericSuperclass() instanceof ParameterizedType t
                        && t.getActualTypeArguments()[1].equals(Object.class)
                    ) {
                        return new CustomDefinition(JsonNodeFactory.instance.objectNode()
                            .put("type", "object")
                            .set("additionalProperties", SchemaNodeFactory.getSchemaType(SchemaNodeFactory.TYPES_ALL))
                        );
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

            // Apply Jackson serializers
            builder.forTypesInGeneral().withCustomDefinitionProvider((resolvedType, context) -> {
                ObjectNode node = JsonNodeFactory.instance.objectNode();
                ObjectMapper mapper = builder.getObjectMapper();
                JavaType javaType = mapper.constructType(resolvedType.getErasedType());

                try {
                    JsonFormatVisitorWrapper visitor = new JsonFormatVisitorWrapper.Base() {
                        @Override
                        public JsonStringFormatVisitor expectStringFormat(JavaType type) throws JsonMappingException {
                            node.put("type", "string");
                            return new JsonStringFormatVisitor() {
                                public void format(JsonValueFormat format) {
                                    setFormat(node, format.toString());
                                }
                                // TODO:
                                public void enumTypes(Set<String> enums) {}
                            };
                        }

                        @Override
                        public JsonNumberFormatVisitor expectNumberFormat(JavaType type) throws JsonMappingException {
                            node.put("type", "number");
                            return new JsonNumberFormatVisitor() {
                                public void numberType(JsonParser.NumberType numberType) {}
                                public void format(JsonValueFormat format) {
                                    setFormat(node, format.toString());
                                }
                                // TODO:
                                public void enumTypes(Set<String> enums) {}
                            };
                        }

                        @Override
                        public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) throws JsonMappingException {
                            node.put("type", "integer");
                            return new JsonIntegerFormatVisitor() {
                                public void numberType(JsonParser.NumberType numberType) {}
                                public void format(JsonValueFormat format) {
                                    setFormat(node, format.toString());
                                }
                                // TODO:
                                public void enumTypes(Set<String> enums) {}
                            };
                        }

                        @Override
                        public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) throws JsonMappingException {
                            node.put("type", "boolean");
                            return new JsonBooleanFormatVisitor() {
                                public void format(JsonValueFormat format) {
                                    setFormat(node, format.toString());
                                }
                                // TODO:
                                public void enumTypes(Set<String> enums) {}
                            };
                        }
                    };

                    // Let Jackson traverse the type and call the visitor
                    mapper.acceptJsonFormatVisitor(javaType, visitor);

                    if (node.has("type")) {
                        return new CustomDefinition(node, CustomDefinition.DefinitionType.INLINE, CustomDefinition.AttributeInclusion.NO);
                    }
                } catch (Exception ignored) {}
                return null;
            });
        }

        private static class JSONSchemaTitleProvider implements CustomDefinitionProviderV2, TypeAttributeOverrideV2 {
            private ResolvedType rootType;

            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
                if (this.rootType == null) {
                    this.rootType = javaType;
                }
                return null;
            }

            /**
             * Matches <a href="https://github.com/mbknor/mbknor-jackson-jsonSchema/blob/e370f80d5dd20eb9396455ab2ddfd7083d0e25fb/src/main/scala/com/kjetland/jackson/jsonSchema/JsonSchemaGenerator.scala#L1355">
             *     mbknor-jackson-jsonSchema
             * </a>
             * @param attrs node to modify (the part that may be referenced multiple times)
             * @param scope the type representation associated with the JSON Schema node
             * @param context generation context
             */
            @Override
            public void overrideTypeAttributes(ObjectNode attrs, TypeScope scope, SchemaGenerationContext context) {
                if (this.rootType == scope.getType() && !attrs.has(context.getKeyword(SchemaKeyword.TAG_TITLE))) {
                    String rawName = rootType.getErasedType().getSimpleName();
                    // Code found here: http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
                    String v = rawName.replaceAll(
                        String.format("%s|%s|%s",
                            "(?<=[A-Z])(?=[A-Z][a-z])",
                            "(?<=[^A-Z])(?=[A-Z])",
                            "(?<=[A-Za-z])(?=[^A-Za-z])"
                        ),
                        " ");
                    // Make the first letter uppercase
                    attrs.put(context.getKeyword(SchemaKeyword.TAG_TITLE), v.substring(0,1).toUpperCase() + v.substring(1));
                }
            }

            @Override
            public void resetAfterSchemaGenerationFinished() {
                this.rootType = null;
            }
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
                JsonNode props = attrs.get(context.getKeyword(SchemaKeyword.TAG_PROPERTIES));
                if (props instanceof ObjectNode propsObj) {
                    // Remove type property on type property for subtypes to enable definition merging
                    propsObj.remove("type");
                }
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
            Option.ADDITIONAL_FIXED_TYPES,
            Option.FLATTENED_ENUMS,
            Option.VALUES_FROM_CONSTANT_FIELDS,
            Option.PUBLIC_NONSTATIC_FIELDS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
            Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES,
            Option.ALLOF_CLEANUP_AT_THE_END,
            Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END
        ))
        .with(new JacksonModule(
            JacksonOption.ALWAYS_REF_SUBTYPES,
            // Disable subtype lookup in Jackson module, as we handle this ourselves to replace anyOf with oneOf
            JacksonOption.SKIP_SUBTYPE_LOOKUP,
            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
            JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY,
            JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE
        ))
        .with(new JakartaValidationModule(
            JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
            JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        ))
        .with(new CustomModule())
        .build();
    }
}
