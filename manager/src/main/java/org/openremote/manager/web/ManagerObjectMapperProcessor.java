/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.swagger.v3.oas.integration.api.ObjectMapperProcessor;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.ServerVariable;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Applies the manager's JSON configuration to the Swagger object mappers, which introspect the API model classes and
 * write the OpenAPI document. Swagger uses Jackson 2 so these settings mirror the Jackson 3 model mapper.
 */
public class ManagerObjectMapperProcessor implements ObjectMapperProcessor {

    private abstract static class ServerVariableMixin {
        @JsonProperty("default")
        List<String> _default;
    }

    private abstract static class StringSchemaMixin {
        @JsonProperty("enum")
        protected List<String> _enum;
    }

    // Swagger does not recognise Jackson 3 nodes, so their internal fields would otherwise become schema properties
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    private abstract static class JsonNodeMixin {
    }

    public static void configure(ObjectMapper objectMapper) {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        objectMapper.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        // Registered explicitly as module auto discovery fails in the Swagger Gradle plugin classloader
        objectMapper.registerModule(new ParameterNamesModule());
        objectMapper.addMixIn(StringSchema.class, StringSchemaMixin.class);
        objectMapper.addMixIn(ServerVariable.class, ServerVariableMixin.class);
        objectMapper.addMixIn(JsonNode.class, JsonNodeMixin.class);
    }

    @Override
    public void processJsonObjectMapper(ObjectMapper objectMapper) {
        configure(objectMapper);
    }

    @Override
    public void processYamlObjectMapper(ObjectMapper objectMapper) {
        configure(objectMapper);
    }

    @Override
    public void processOutputJsonObjectMapper(ObjectMapper objectMapper) {
        configure(objectMapper);
    }

    @Override
    public void processOutputYamlObjectMapper(ObjectMapper objectMapper) {
        configure(objectMapper);
    }
}
