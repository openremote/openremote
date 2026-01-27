/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.util;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.*;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openremote.model.util.JSONSchemaUtil.*;

public class JSONSchemaUtilTest {

  @BeforeEach
  void setup() {
    ValueUtil.doInitialise();
  }

  private static InputStream loadResourceAsStream(String resourcePath) {
    return JSONSchemaUtilTest.class.getResourceAsStream(
        "/org/openremote/model/util/" + resourcePath + ".json");
  }

  static class Title {}

  @Test
  public void shouldHaveTitle() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "Title",
                "additionalProperties": true
            }""");

    JsonNode actual = ValueUtil.getSchema(Title.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonSchemaTitle(value = "Test Title", i18n = false)
  static class ItemType {}

  static class MembersShouldNotHaveTitle {
    public Map<String, String> test;
    public ItemType[] test1;
  }

  @Test
  public void shouldNotHaveTitle() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            """);

    JsonNode actual = ValueUtil.getSchema(MembersShouldNotHaveTitle.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @Test
  public void shouldRemapByte() throws IOException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(loadResourceAsStream(java.lang.Byte.class.getName()));
    JsonNode actual = ValueUtil.getSchema(java.lang.Byte.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  static class AdditionalProperties {}

  @Test
  public void shouldHaveAdditionalPropertiesTrue() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "Additional Properties",
                "additionalProperties": true
            }""");

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

    @JsonSchemaSupplier(
        supplier = SchemaNodeMapper.SCHEMA_SUPPLIER_NAME_PATTERN_PROPERTIES_ANY_KEY_ANY_TYPE)
    public Boolean test4;
  }

  @Test
  public void shouldRemapTypes() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(RemapTypes.class);
    assertEquals(expected.toString(), actual.toString(), false);
  }

  @ParameterizedTest
  @ValueSource(
      classes = {
        org.openremote.model.value.ValueType.BooleanMap.class,
        org.openremote.model.value.ValueType.DoubleMap.class,
        org.openremote.model.value.ValueType.IntegerMap.class,
        org.openremote.model.value.ValueType.ObjectMap.class,
        org.openremote.model.value.ValueType.StringMap.class,
        org.openremote.model.value.ValueType.MultivaluedStringMap.class,
      })
  public void shouldHandleMapTypes() throws IOException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(loadResourceAsStream(java.lang.Byte.class.getName()));
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
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

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
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(Primitives.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  static class AnnotationsForFields {
    @JsonSchemaTitle(value = "test", i18n = false)
    @JsonSchemaDescription(value = "test", i18n = false)
    @JsonSchemaFormat("test")
    @JsonSchemaDefault("false")
    @JsonSchemaExamples({"test"})
    public Boolean all;
  }

  @Test
  public void shouldApplyCustomAnnotationsForFields()
      throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(AnnotationsForFields.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonSchemaTitle(value = "test", i18n = false)
  @JsonSchemaDescription(value = "test", i18n = false)
  @JsonSchemaFormat("test")
  @JsonSchemaDefault("{}")
  @JsonSchemaExamples({"test"})
  static class AnnotationsForTypes {}

  @Test
  public void shouldApplyCustomAnnotationsForTypes() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(AnnotationsForTypes.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonSchemaTitle("test")
  @JsonSchemaDescription("test")
  static class I18nAnnotations {}

  @Test
  public void shouldApplyI18nAnnotations() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "I 18n Annotations",
                "i18n": "org.openremote.model.util.JSONSchemaUtilTest.I18nAnnotations",
                "additionalProperties": true
            }""");

    JsonNode actual = ValueUtil.getSchema(I18nAnnotations.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonSchemaTitle(value = "test", i18n = false)
  @JsonSchemaDescription("Translated description")
  static class I18nAnnotationsPartiallyDisabled {}

  @Test
  public void shouldApplyI18nAnnotationsPartiallyDisabled()
      throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "title": "test",
                "i18n": "org.openremote.model.util.JSONSchemaUtilTest.I18nAnnotationsPartiallyDisabled",
                "additionalProperties": true
            }""");

    JsonNode actual = ValueUtil.getSchema(I18nAnnotationsPartiallyDisabled.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonTypeInfo(
      property = "type",
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY)
  @JsonSubTypes({
    @JsonSubTypes.Type(SubType.class),
    @JsonSubTypes.Type(SubTypeSuperclass.class),
  })
  abstract static class PolymorphicType<T extends PolymorphicType<?>> implements Serializable {}

  @JsonTypeName("SubType")
  static class SubType extends PolymorphicType<SubType> {}

  @JsonTypeName("SubTypeSuperclass")
  static class SubTypeSuperclass extends SubType {}

  @Test
  public void shouldHaveSubtypesWithDefaultTypeProperty()
      throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(PolymorphicType.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonTypeInfo(
      property = "customType",
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY)
  @JsonSubTypes({
    @JsonSubTypes.Type(CustomSubType.class),
    @JsonSubTypes.Type(CustomSubTypeSuperclass.class),
  })
  abstract static class CustomPolymorphicType<T extends CustomPolymorphicType<?>>
      implements Serializable {}

  @JsonTypeName("SubType")
  static class CustomSubType extends CustomPolymorphicType<CustomSubType> {}

  @JsonTypeName("SubTypeSuperclass")
  static class CustomSubTypeSuperclass extends CustomSubType {}

  @Test
  public void shouldHaveSubtypesWithCustomTypeProperty()
      throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "definitions": {
                    "CustomSubType": {
                        "title": "Custom Sub Type",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "customType": {
                                "const": "SubType",
                                "default": "SubType"
                            }
                        },
                        "required": [
                            "customType"
                        ],
                        "discriminator": {
                            "propertyName": "customType"
                        }
                    },
                    "CustomSubTypeSuperclass": {
                        "title": "Custom Sub Type Superclass",
                        "type": "object",
                        "additionalProperties": true,
                        "properties": {
                            "customType": {
                                "const": "SubTypeSuperclass",
                                "default": "SubTypeSuperclass"
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
                    { "$ref": "#/definitions/CustomSubType" },
                    { "$ref": "#/definitions/CustomSubTypeSuperclass" }
                ],
                "type": "object",
                "additionalProperties": true,
                "title": "Custom Polymorphic Type"
            }""");

    JsonNode actual = ValueUtil.getSchema(CustomPolymorphicType.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonTypeInfo(
      property = "type",
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
  @JsonSubTypes({
    @JsonSubTypes.Type(ExternalSubType.class),
    @JsonSubTypes.Type(ExternalSubTypeSuperclass.class),
  })
  abstract static class ExternalPolymorphicType<T extends ExternalPolymorphicType<?>>
      implements Serializable {}

  static class ExternalSubType extends ExternalPolymorphicType<ExternalSubType> {}

  static class ExternalSubTypeSuperclass extends ExternalSubType {}

  @Test
  public void shouldSetEnumTypeForExternalProperty() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(ExternalPolymorphicType.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  @JsonTypeName("ReflectedPolymorphicType")
  @JsonTypeInfo(
      property = "type",
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY)
  abstract static class ReflectedPolymorphicType<T extends ReflectedPolymorphicType<?>>
      implements Serializable {}

  @JsonTypeName("ResolvedSubType")
  static class ResolvedSubType extends ReflectedPolymorphicType<ResolvedSubType> {}

  @Test
  public void shouldResolveSubtypesThroughReflections()
      throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(ReflectedPolymorphicType.class);
    assertEquals(expected.toString(), actual.toString(), true);
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
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(JavaTimeJacksonModule.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }

  enum TypeOption {
    INTEGER(int.class),
    STRING(String.class),
    LONG(long.class),
    FLOAT(Float.class);

    TypeOption(Class<?> javaType) {}
  }

  @Test
  public void shouldGenerateEnum() throws JsonProcessingException, JSONException {
    JsonNode expected =
        ValueUtil.JSON.readTree(
            """
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
            }""");

    JsonNode actual = ValueUtil.getSchema(TypeOption.class);
    assertEquals(expected.toString(), actual.toString(), true);
  }
}
