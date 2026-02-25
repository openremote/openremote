package org.openremote.model.util;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openremote.model.query.filter.*;
import org.openremote.model.util.JSONSchemaUtil.*;
import org.openremote.model.value.JsonPathFilter;
import org.openremote.model.value.MathExpressionValueFilter;
import org.openremote.model.value.RegexValueFilter;
import org.openremote.model.value.SubStringValueFilter;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.*;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class JSONSchemaUtilTest {

    private static final Reflections reflections = new Reflections("org.openremote");

    @BeforeEach
    void setup() {
        ValueUtil.doInitialise();
    }

    private static InputStream loadResourceAsStream(String resourcePath) {
        return JSONSchemaUtilTest.class.getResourceAsStream("/org/openremote/model/util/" + resourcePath + ".json");
    }

    static class Title {}

    @Test
    public void shouldHaveTitle() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "Title",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(Title.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonSchemaTitle(value = "Test Title", i18n = false)
    static class ItemType { }

    static class MembersShouldNotHaveTitle {
        public Map<String, String> test;
        public ItemType[] test1;
    }

    @Test
    public void shouldNotHaveTitle() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "test": { "type": "object", "additionalProperties": { "type": "string" } },
                    "test1": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "title": "Test Title",
                        "additionalProperties": true
                      }
                    }
                  },
                  "title": "Members Should Not Have Title",
                  "additionalProperties": true
                }
            """
        );

        JsonNode actual = ValueUtil.getSchema(MembersShouldNotHaveTitle.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @Test
    public void shouldRemapByte() throws IOException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree(loadResourceAsStream(java.lang.Byte.class.getName()));
        JsonNode actual = ValueUtil.getSchema(java.lang.Byte.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    static class AdditionalProperties {}

    @Test
    public void shouldHaveAdditionalPropertiesTrue() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "Additional Properties",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(AdditionalProperties.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    static class RemapTypes {
        @JsonSchemaTypeRemap(type = String.class)
        public boolean test1;
        @JsonSchemaTypeRemap(type = boolean.class)
        public String test2;
        @JsonSchemaTypeRemap(type = Date.class)
        public boolean test3;
        @JsonSchemaSupplier(supplier = SchemaNodeMapper.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE)
        public Boolean test4;
    }

    @Test
    public void shouldRemapTypes() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema":"http://json-schema.org/draft-07/schema#",
                "type":"object",
                "properties": {
                    "test1":{"type":"string"},
                    "test2":{"type":"boolean"},
                    "test3":{"type":"integer","format":"utc-millisec"},
                    "test4":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}}}
                },
                "required":["test1","test3"],
                "title":"Remap Types",
                "additionalProperties":true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(RemapTypes.class);
        assertEquals(expected.toString(), actual.toString(), false);
    }

    @ParameterizedTest
    @ValueSource(classes = {
            org.openremote.model.value.ValueType.BooleanMap.class,
            org.openremote.model.value.ValueType.DoubleMap.class,
            org.openremote.model.value.ValueType.IntegerMap.class,
            org.openremote.model.value.ValueType.ObjectMap.class,
            org.openremote.model.value.ValueType.StringMap.class,
            org.openremote.model.value.ValueType.MultivaluedStringMap.class,
    })
    public void shouldHandleMapTypes() throws IOException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree(loadResourceAsStream(java.lang.Byte.class.getName()));
        JsonNode actual = ValueUtil.getSchema(java.lang.Byte.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    static class JacksonAnnotations {
        @JsonPropertyDescription("This property should have a description.")
        public Boolean test1;
        @JsonProperty("renamed")
        public Boolean test2;
        @JsonProperty(value = "renamed1", required = true)
        public Boolean test3;
    }

    @Test
    public void shouldHandleJacksonAnnotations() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {
                    "renamed": {
                        "type": "boolean"
                    },
                    "renamed1": {
                        "type": "boolean",
                        "readOnly": true,
                        "writeOnly": true
                    },
                    "test1": {
                        "type": "boolean",
                        "description": "This property should have a description."
                    }
                },
                "required": [ "renamed1" ],
                "title": "Jackson Annotations",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(JacksonAnnotations.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    static class Primitives {
        public boolean test1;
        public int test2;
        public long test3;
        public float test4;
        public double test5;
        public byte test6;
        public char test7;
    }

    @Test
    public void shouldHaveRequiredPrimitives() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {
                    "test1": { "type": "boolean" },
                    "test2": { "type": "integer" },
                    "test3": { "type": "integer" },
                    "test4": { "type": "number" },
                    "test5": { "type": "number" },
                    "test6": { "type": "integer" },
                    "test7": { "type": "string" }
                },
                "required": [
                    "test1",
                    "test2",
                    "test3",
                    "test4",
                    "test5",
                    "test6",
                    "test7"
                ],
                "title": "Primitives",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(Primitives.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    static class AnnotationsForFields {
        @JsonSchemaTitle(value = "test", i18n = false)
        @JsonSchemaDescription(value = "test", i18n = false)
        @JsonSchemaFormat("test")
        @JsonSchemaDefault("false")
        @JsonSchemaExamples({ "test" })
        public Boolean all;
    }

    @Test
    public void shouldApplyCustomAnnotationsForFields() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {
                    "all":{
                        "type":"boolean",
                        "title":"test",
                        "description":"test",
                        "default": false,
                        "format":"test",
                        "examples":["test"]
                    }
                },
                "title": "Annotations For Fields",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(AnnotationsForFields.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonSchemaTitle(value = "test", i18n = false)
    @JsonSchemaDescription(value = "test", i18n = false)
    @JsonSchemaFormat("test")
    @JsonSchemaDefault("{}")
    @JsonSchemaExamples({ "test" })
    static class AnnotationsForTypes {
    }

    @Test
    public void shouldApplyCustomAnnotationsForTypes() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "test",
                "additionalProperties": true,
                "description": "test",
                "format": "test",
                "default": {},
                "examples": [
                    "test"
                ]
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(AnnotationsForTypes.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonSchemaTitle("test")
    @JsonSchemaDescription("test")
    static class I18nAnnotations {
    }

    @Test
    public void shouldApplyI18nAnnotations() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "I 18n Annotations",
                "i18n": "org.openremote.model.util.JSONSchemaUtilTest.I18nAnnotations",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(I18nAnnotations.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonSchemaTitle(value = "test", i18n = false)
    @JsonSchemaDescription("Translated description")
    static class I18nAnnotationsPartiallyDisabled {
    }

    @Test
    public void shouldApplyI18nAnnotationsPartiallyDisabled() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "test",
                "i18n": "org.openremote.model.util.JSONSchemaUtilTest.I18nAnnotationsPartiallyDisabled",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(I18nAnnotationsPartiallyDisabled.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(SubType.class),
            @JsonSubTypes.Type(SubTypeSuperclass.class),
    })
    abstract static class PolymorphicType<T extends PolymorphicType<?>> implements Serializable { }
    @JsonTypeName("SubType")
    static class SubType extends PolymorphicType<SubType> { }
    @JsonTypeName("SubTypeSuperclass")
    static class SubTypeSuperclass extends SubType { }

    @Test
    public void shouldHaveSubtypesWithDefaultTypeProperty() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "SubType": {
                        "title": "Sub Type",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "SubType",
                                "default": "SubType"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    },
                    "SubTypeSuperclass": {
                        "title": "Sub Type Superclass",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "SubTypeSuperclass",
                                "default": "SubTypeSuperclass"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    }
                },
                "oneOf": [
                    { "$ref": "#/definitions/SubType" },
                    { "$ref": "#/definitions/SubTypeSuperclass" }
                ],
                "type": "object",
                "additionalProperties": true,
                "title": "Polymorphic Type"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(PolymorphicType.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonTypeInfo(property = "customType", use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(SubTypeWithCustomProperty.class),
            @JsonSubTypes.Type(SubTypeSuperClassWithCustomProperty.class),
    })
    abstract static class PolymorphicTypeWithCustomProperty<T extends PolymorphicTypeWithCustomProperty<?>> implements Serializable { }
    @JsonTypeName("SubTypeWithCustomProperty")
    static class SubTypeWithCustomProperty extends PolymorphicTypeWithCustomProperty<SubTypeWithCustomProperty> { }
    @JsonTypeName("SubTypeSuperClassWithCustomProperty")
    static class SubTypeSuperClassWithCustomProperty extends SubTypeWithCustomProperty { }

    @Test
    public void shouldHaveSubtypesWithCustomTypeProperty() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "SubTypeWithCustomProperty": {
                        "title": "Sub Type With Custom Property",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "customType": {
                                "const": "SubTypeWithCustomProperty",
                                "default": "SubTypeWithCustomProperty"
                            }
                        },
                        "required": [
                            "customType"
                        ],
                        "discriminator": {
                            "propertyName": "customType"
                        }
                    },
                    "SubTypeSuperClassWithCustomProperty": {
                        "title": "Sub Type Super Class With Custom Property",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "customType": {
                                "const": "SubTypeSuperClassWithCustomProperty",
                                "default": "SubTypeSuperClassWithCustomProperty"
                            }
                        },
                        "required": [
                            "customType"
                        ],
                        "discriminator": {
                            "propertyName": "customType"
                        }
                    }
                },
                "oneOf": [
                    { "$ref": "#/definitions/SubTypeWithCustomProperty" },
                    { "$ref": "#/definitions/SubTypeSuperClassWithCustomProperty" }
                ],
                "type": "object",
                "additionalProperties": true,
                "title": "Polymorphic Type With Custom Property"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(PolymorphicTypeWithCustomProperty.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    // Note: JsonTypeInfo.As.EXISTING_PROPERTY doesn't necessarily change the behavior mainly the
    // "customType" property on the abstract class matters.
    @JsonTypeInfo(property = PolymorphicTypeWithCustomExistingProperty.VALUE_KEY_CUSTOM_TYPE, use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(name=SubTypeWithCustomExistingProperty.SUB_CUSTOM_TYPE, value=SubTypeWithCustomExistingProperty.class),
            @JsonSubTypes.Type(name=SubTypeSuperClassWithCustomExistingProperty.SUPER_CUSTOM_TYPE, value=SubTypeSuperClassWithCustomExistingProperty.class),
    })
    abstract static class PolymorphicTypeWithCustomExistingProperty<T extends PolymorphicTypeWithCustomExistingProperty<?>> implements Serializable {
        public static final String VALUE_KEY_CUSTOM_TYPE = "customType";
        @JsonProperty(VALUE_KEY_CUSTOM_TYPE)
        protected String customType;
        public String getCustomType() {
            return customType;
        }
    }
    @JsonTypeName(SubTypeWithCustomExistingProperty.SUB_CUSTOM_TYPE)
    static class SubTypeWithCustomExistingProperty extends PolymorphicTypeWithCustomExistingProperty<SubTypeWithCustomExistingProperty> {
        public static final String SUB_CUSTOM_TYPE = "sub";
    }
    @JsonTypeName(SubTypeSuperClassWithCustomExistingProperty.SUPER_CUSTOM_TYPE)
    static class SubTypeSuperClassWithCustomExistingProperty extends SubTypeWithCustomExistingProperty {
        public static final String SUPER_CUSTOM_TYPE = "super";
    }

    @Test
    public void shouldHaveSubtypesWithExistingCustomTypeProperty() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "SubTypeWithCustomExistingProperty": {
                        "title": "Sub Type With Custom Existing Property",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "customType": {
                                "const": "sub",
                                "default": "sub"
                            }
                        },
                        "required": [
                            "customType"
                        ],
                        "discriminator": {
                            "propertyName": "customType"
                        }
                    },
                    "SubTypeSuperClassWithCustomExistingProperty": {
                        "title": "Sub Type Super Class With Custom Existing Property",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "customType": {
                                "const": "super",
                                "default": "super"
                            }
                        },
                        "required": [
                            "customType"
                        ],
                        "discriminator": {
                            "propertyName": "customType"
                        }
                    }
                },
                "oneOf": [
                    { "$ref": "#/definitions/SubTypeWithCustomExistingProperty" },
                    { "$ref": "#/definitions/SubTypeSuperClassWithCustomExistingProperty" }
                ],
                "type": "object",
                "additionalProperties": true,
                "title": "Polymorphic Type With Custom Existing Property"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(PolymorphicTypeWithCustomExistingProperty.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(ExternalSubType.class),
            @JsonSubTypes.Type(ExternalSubTypeSuperclass.class),
    })
    abstract static class ExternalPolymorphicType<T extends ExternalPolymorphicType<?>> implements Serializable {}
    static class ExternalSubType extends ExternalPolymorphicType<ExternalSubType> { }
    static class ExternalSubTypeSuperclass extends ExternalSubType { }

    @Test
    public void shouldSetEnumTypeForExternalProperty() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "ExternalSubType": {
                        "title": "External Sub Type",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "ExternalSubType",
                                "default": "ExternalSubType"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    },
                    "ExternalSubTypeSuperclass": {
                        "title": "External Sub Type Superclass",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "ExternalSubTypeSuperclass",
                                "default": "ExternalSubTypeSuperclass"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    }
                },
                "oneOf": [
                    { "$ref": "#/definitions/ExternalSubType" },
                    { "$ref": "#/definitions/ExternalSubTypeSuperclass" }
                ],
                "properties": {
                    "type": {
                        "enum": [
                            "ExternalSubType",
                            "ExternalSubTypeSuperclass"
                        ]
                    }
                },
                "type": "object",
                "additionalProperties": true,
                "title": "External Polymorphic Type"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(ExternalPolymorphicType.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @JsonTypeName("ReflectedPolymorphicType")
    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
    abstract static class ReflectedPolymorphicType<T extends ReflectedPolymorphicType<?>> implements Serializable {}
    @JsonTypeName("ResolvedSubType")
    static class ResolvedSubType extends ReflectedPolymorphicType<ResolvedSubType> { }

    @Test
    public void shouldResolveSubtypesThroughReflections() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "ResolvedSubType": {
                        "title": "Resolved Sub Type",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "ResolvedSubType",
                                "default": "ResolvedSubType"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    }
                },
                "title": "Reflected Polymorphic Type",
                "oneOf": [
                    { "$ref": "#/definitions/ResolvedSubType" }
                ],
                "type": "object",
                "additionalProperties": true,
                "properties": {
                    "type": {
                        "const": "ReflectedPolymorphicType"
                    }
                },
                "required": [
                    "type"
                ]
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(ReflectedPolymorphicType.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    @Test
    public void shouldNotHaveAllOf() {
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(JsonTypeInfo.class)) {
            System.out.println(clazz);
            System.out.println(ValueUtil.getSchema(clazz).toString());
//            if (clazz.equals(CalendarEventPredicate.class)) continue;
            if (clazz.equals(ValueEmptyPredicate.class)) continue;
            if (clazz.equals(ValueAnyPredicate.class)) continue;
            if (clazz.equals(RegexValueFilter.class)) continue;
            if (clazz.equals(MathExpressionValueFilter.class)) continue;
            if (clazz.equals(JsonPathFilter.class)) continue;
            if (clazz.equals(SubStringValueFilter.class)) continue;
            if (clazz.equals(RadialGeofencePredicate.class)) continue;
            if (clazz.equals(GeoJSONGeofencePredicate.class)) continue;
            if (clazz.equals(RectangularGeofencePredicate.class)) continue;
            assertFalse(ValueUtil.getSchema(clazz).toString().contains("allOf"));
        }
    }

    static class JavaTimeJacksonModule {
        public Duration duration;
        public LocalDateTime localDateTime;
        public LocalDate localDate;
        public LocalTime localTime;
        public MonthDay monthDay;
        public OffsetTime offsetTime;
        public Period period;
        public Year year;
        public YearMonth yearMonth;
        public ZoneId zoneId;
        public ZoneOffset zoneOffset;
        // Instant variants
        public Instant instant;
        public OffsetDateTime offsetDateTime;
        public ZonedDateTime zonedDateTime;
    }

    @Test
    public void shouldApplyJacksonSerializers() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {
                    "duration": {
                        "type": "integer",
                        "format": "utc-millisec"
                    },
                    "instant": {
                        "type": "integer",
                        "format": "utc-millisec"
                    },
                    "localDate": { "type": "string" },
                    "localDateTime": { "type": "string" },
                    "localTime": { "type": "string" },
                    "monthDay": {
                        "title": "Month Day",
                        "type": "object",
                        "properties": {
                            "month": {
                                "type": "integer"
                            }
                        },
                        "required": [
                            "month"
                        ],
                        "additionalProperties": true
                    },
                    "offsetDateTime": {
                        "type": "integer",
                        "format": "utc-millisec"
                    },
                    "offsetTime": { "type": "string" },
                    "period": { "type": "string" },
                    "year": { "type": "integer" },
                    "yearMonth": {
                        "title": "Year Month",
                        "type": "object",
                        "properties": {
                            "month": {
                                "type": "integer"
                            },
                            "year": {
                                "type": "integer"
                            }
                        },
                        "required": [
                            "month",
                            "year"
                        ],
                        "additionalProperties": true
                    },
                    "zoneId": { "type": "string" },
                    "zoneOffset": { "type": "string" },
                    "zonedDateTime": {
                        "type": "integer",
                        "format": "utc-millisec"
                    }
                },
                "title": "Java Time Jackson Module",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(JavaTimeJacksonModule.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }

    enum TypeOption {
        INTEGER(int.class),
        STRING(String.class),
        LONG(long.class),
        FLOAT(Float.class);

        TypeOption(Class<?> javaType) {
        }
    }

    @Test
    public void shouldGenerateEnum() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "string",
                "enum": [
                    "INTEGER",
                    "STRING",
                    "LONG",
                    "FLOAT"
                ],
                "title": "Type Option"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(TypeOption.class);
        assertEquals(expected.toString(), actual.toString(), true);
    }
}
