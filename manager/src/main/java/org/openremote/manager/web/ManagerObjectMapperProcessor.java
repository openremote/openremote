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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.integration.api.ObjectMapperProcessor;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.ServerVariable;
import org.openremote.model.util.ValueUtil;

import java.util.List;

/**
 * Applies the manager's Jackson configuration when Swagger generates an OpenAPI document outside the running manager.
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

    public static void configure(ObjectMapper objectMapper) {
        ValueUtil.configureObjectMapper(objectMapper);
        objectMapper.addMixIn(StringSchema.class, StringSchemaMixin.class);
        objectMapper.addMixIn(ServerVariable.class, ServerVariableMixin.class);
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
