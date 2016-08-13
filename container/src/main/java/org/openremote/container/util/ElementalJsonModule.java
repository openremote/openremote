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
import elemental.json.*;

import javax.ws.rs.NotSupportedException;
import java.io.IOException;

public class ElementalJsonModule extends SimpleModule {
    private static class ElementalJsonDeserializer<T extends JsonValue> extends JsonDeserializer<T> {
        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return (T)genDeserialise(p, ctxt);
        }
    }
//
//    private static class ElementalJsonDeserializer extends JsonDeserializer<JsonValue> {
//        @Override
//        public JsonValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
//            return deserialize(p, ctxt);
//        }
//    }

    private static class ElementalJsonSerializer extends JsonSerializer<JsonValue> {
        @Override
        public void serialize(JsonValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeRawValue(value.toJson());
        }
    }

    protected static String JsonParserReader(JsonParser parser) throws IOException {
        JsonToken startToken = parser.getCurrentToken();
        JsonToken endToken = startToken.equals(JsonToken.START_OBJECT) ? JsonToken.END_OBJECT : JsonToken.END_ARRAY;

        if (!startToken.equals(JsonToken.START_ARRAY) && !startToken.equals(JsonToken.START_OBJECT)) {
            throw new RuntimeException("Only Objects and Arrays can be processed");
        }
        JsonToken token = startToken;
        int counter = 1;
        String str = parser.getText();
        JsonToken prevToken = token;
        token = parser.nextToken();

        while(token != null && counter > 0) {
            if (token.equals(endToken)) {
                counter--;
            } else if (token.equals(startToken)) {
                counter++;
            }

            String strFormat;
            if (token.equals(JsonToken.VALUE_STRING)) {
                strFormat = "\"%1$s\"";
            } else if (token.equals(JsonToken.FIELD_NAME)) {
                strFormat = "\"%1$s\":";
            } else {
                strFormat = "%1$s";
            }

            str += String.format(strFormat, parser.getText());
            prevToken = token;
            token = parser.nextToken();

            if (token != null && !prevToken.equals(JsonToken.FIELD_NAME) && !prevToken.equals(JsonToken.START_ARRAY) && !prevToken.equals(JsonToken.START_OBJECT) && !token.equals(JsonToken.END_ARRAY) && !token.equals(JsonToken.END_OBJECT)) {
                str += ",";
            }
        }

        return str;
    }

    protected static JsonValue genDeserialise(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        JsonToken token = parser.getCurrentToken();
        token = token == null || token.equals(JsonToken.FIELD_NAME) ? parser.nextValue() : token;
        switch(token) {
            case START_OBJECT:
                String objStr = JsonParserReader(parser);
                return Json.parse(objStr);
            case START_ARRAY:
                String arrStr = JsonParserReader(parser);
                return Json.parse(arrStr);
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

    public ElementalJsonModule() {
        super("ElementalJsonModule", new Version(1,0,0, "latest", null, null));
        ElementalJsonSerializer serializer = new ElementalJsonSerializer();
        ElementalJsonDeserializer deserializer = new ElementalJsonDeserializer();
        this.addSerializer(JsonValue.class, serializer);
        this.addDeserializer(JsonValue.class, deserializer);
        this.addDeserializer(JsonObject.class, deserializer);
        this.addDeserializer(JsonString.class, deserializer);
        this.addDeserializer(JsonNumber.class, deserializer);
        this.addDeserializer(JsonArray.class, deserializer);
    }
}
