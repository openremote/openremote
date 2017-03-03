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
package org.openremote.container.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import elemental.json.*;

import java.io.IOException;

public class ElementalJsonModule extends SimpleModule {

    private static class ElementalJsonDeserializer<T extends JsonValue> extends StdDeserializer<T> {

        public ElementalJsonDeserializer() {
            super(JsonValue.class);
        }

        @Override
        public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JsonProcessingException {
            return (T) Json.instance().parse(jsonParser.getCodec().readTree(jsonParser).toString());
        }
    }

    private static class ElementalJsonSerializer extends JsonSerializer<JsonValue> {
        @Override
        public void serialize(JsonValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            if (value instanceof JsonNull) {
                gen.writeNull();
            } else {
                gen.writeRawValue(value.toJson());
            }
        }
    }

    public ElementalJsonModule() {
        super("ElementalJsonModule", new Version(1, 0, 0, "latest", null, null));
        ElementalJsonSerializer serializer = new ElementalJsonSerializer();
        ElementalJsonDeserializer deserializer = new ElementalJsonDeserializer();
        this.addSerializer(JsonValue.class, serializer);
        this.addDeserializer(JsonObject.class, deserializer);
        this.addDeserializer(JsonString.class, deserializer);
        this.addDeserializer(JsonNumber.class, deserializer);
        this.addDeserializer(JsonArray.class, deserializer);
        this.addDeserializer(JsonValue.class, deserializer);
        this.addDeserializer(JsonNull.class, deserializer);
    }
}
