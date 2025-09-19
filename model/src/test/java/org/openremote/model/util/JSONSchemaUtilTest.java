package org.openremote.model.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openremote.model.util.JSONSchemaUtil.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.time.*;
import java.util.Date;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class JSONSchemaUtilTest {

    @BeforeEach
    void setup() {
        ValueUtil.doInitialise();
    }

    private static URL loadResource(String resourcePath) {
        return JSONSchemaUtilTest.class.getResource("/org/openremote/model/util/" + resourcePath + ".json");
    }

    // TODO: expand this with subtypes
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
        assertEquals(expected.toString(), actual.toString(), false);
    }

    @Test
    public void shouldRemapByte() throws IOException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree(loadResource(java.lang.Byte.class.getName()));
        JsonNode actual = ValueUtil.getSchema(java.lang.Byte.class);
        assertEquals(expected.toString(), actual.toString(), false);
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
        assertEquals(expected.toString(), actual.toString(), false);
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
        System.out.println(actual);
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
        JsonNode expected = ValueUtil.JSON.readTree(loadResource(java.lang.Byte.class.getName()));
        JsonNode actual = ValueUtil.getSchema(java.lang.Byte.class);
        assertEquals(expected.toString(), actual.toString(), false);
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
    public void shouldHandleJacksonResolvers() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {
                    "renamed": {
                        "type": "boolean"
                    },
                    "renamed1": {
                        "type": "boolean"
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
        assertEquals(expected.toString(), actual.toString(), false);
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
        assertEquals(expected.toString(), actual.toString(), false);
    }

    static class AnnotationsForFields {
        @JsonSchemaTitle("test")
        @JsonSchemaDescription("test")
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
                        "examples":["test"],
                        "i18n":"org.openremote.model.util.JSONSchemaUtilTest.AnnotationsForFields.all"
                    }
                },
                "title": "Annotations For Fields",
                "additionalProperties": true
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(AnnotationsForFields.class);
        assertEquals(expected.toString(), actual.toString(), false);
    }

    @JsonSchemaTitle("test")
    @JsonSchemaDescription("test")
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
                ],
                "i18n": "org.openremote.model.util.JSONSchemaUtilTest.AnnotationsForTypes"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(AnnotationsForTypes.class);
        assertEquals(expected.toString(), actual.toString(), false);
    }

    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(TestAgentLinkAnnotationResolving.class),
            @JsonSubTypes.Type(TestAgentLinkSuperclassAnnotationResolving.class),
    })
    abstract static class TestAgentLink<T extends TestAgentLink<?>> implements Serializable {

        @JsonSchemaFormat("or-agent-id")
        protected String id;

        @JsonSerialize
        protected String getType() {
            return getClass().getSimpleName();
        }
    }
    static class TestAgentLinkAnnotationResolving extends TestAgentLink<TestAgentLinkAnnotationResolving> { }
    static class TestAgentLinkSuperclassAnnotationResolving extends TestAgentLinkAnnotationResolving { }

    @Test
    public void shouldHaveSubtypesWithDefaultTypeProperty() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "TestAgentLinkAnnotationResolving": {
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "JSONSchemaUtilTest$TestAgentLinkAnnotationResolving",
                                "default": "JSONSchemaUtilTest$TestAgentLinkAnnotationResolving"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    },
                    "TestAgentLinkSuperclassAnnotationResolving": {
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "JSONSchemaUtilTest$TestAgentLinkSuperclassAnnotationResolving",
                                "default": "JSONSchemaUtilTest$TestAgentLinkSuperclassAnnotationResolving"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    }
                },
                "oneOf": [
                    { "$ref": "#/definitions/TestAgentLinkAnnotationResolving" },
                    { "$ref": "#/definitions/TestAgentLinkSuperclassAnnotationResolving" }
                ],
                "title": "Test Agent Link"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(TestAgentLink.class);
        assertEquals(expected.toString(), actual.toString(), false);
    }

    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY)
    abstract static class TestAgentLinkReflections<T extends TestAgentLinkReflections<?>> implements Serializable {

        @JsonSchemaFormat("or-agent-id")
        protected String id;

        @JsonSerialize
        protected String getType() {
            return getClass().getSimpleName();
        }
    }
    static class TestAgentLinkReflectionResolving extends TestAgentLinkReflections<TestAgentLinkReflectionResolving> { }

    @Test
    public void shouldResolveSubtypesReflections() throws JsonProcessingException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree("""
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "TestAgentLinkReflectionResolving": {
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "type": {
                                "const": "JSONSchemaUtilTest$TestAgentLinkReflectionResolving",
                                "default": "JSONSchemaUtilTest$TestAgentLinkReflectionResolving"
                            }
                        },
                        "required": [
                            "type"
                        ]
                    }
                },
                "oneOf": [
                    { "$ref": "#/definitions/TestAgentLinkReflectionResolving" }
                ],
                "title": "Test Agent Link Reflections"
            }"""
        );

        JsonNode actual = ValueUtil.getSchema(TestAgentLinkReflections.class);
        assertEquals(expected.toString(), actual.toString(), false);
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
        // instant variants
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
        assertEquals(expected.toString(), actual.toString(), false);
    }
}
