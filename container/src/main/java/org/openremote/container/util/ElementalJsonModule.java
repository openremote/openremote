/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.util;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import elemental.json.Json;
import elemental.json.JsonValue;

import java.io.IOException;

public class ElementalJsonModule extends SimpleModule {
    private static class ElementalJsonDeserializer extends JsonDeserializer<JsonValue> {
        @Override
        public JsonValue deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            JsonToken token = parser.getCurrentToken();
            token = token == null || token.equals(JsonToken.FIELD_NAME) ? parser.nextValue() : token;
            switch(token) {
                case START_OBJECT:
                    return Json.parse(parser.getText());
                case START_ARRAY:
                    return Json.parse(parser.getText());
                case VALUE_FALSE:
                    return Json.create(false);
                case VALUE_TRUE:
                    return Json.create(true);
                case VALUE_NULL:
                    return Json.createNull();
                case VALUE_NUMBER_FLOAT:
                    return Json.create(parser.getFloatValue());
                case VALUE_NUMBER_INT:
                    return Json.create(parser.getIntValue());
                case VALUE_STRING:
                    return Json.create(parser.getValueAsString());
                case VALUE_EMBEDDED_OBJECT:
                    throw new JsonParseException("Elemental Json Parser doesn't support Embedded Object Deserialization", JsonLocation.NA);
                default:
                    throw new JsonParseException("Elemental Json Parser doesn't support this token type", JsonLocation.NA);
            }
        }
    }

    private static class ElementalJsonSerializer extends JsonSerializer<JsonValue> {
        @Override
        public void serialize(JsonValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeRawValue(value.toJson());
        }
    }

    public ElementalJsonModule() {
        super("ElementalJsonModule", new Version(1,0,0, "latest", null, null));
        this.addDeserializer(JsonValue.class, new ElementalJsonDeserializer());
        this.addSerializer(JsonValue.class, new ElementalJsonSerializer());
    }
}
