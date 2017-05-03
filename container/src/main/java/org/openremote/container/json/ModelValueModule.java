/*
 * Copyright 2017, OpenRemote Inc.
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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.openremote.model.value.*;

import java.io.IOException;

public class ModelValueModule extends SimpleModule {

    private static class ValueJsonDeserializer<T extends Value> extends StdDeserializer<T> {

        public ValueJsonDeserializer() {
            super(Value.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            try {
                return (T) Values.instance().parse(jsonParser.getCodec().readTree(jsonParser).toString());
            } catch (ValueException ex) {
                throw new IOException(ex);
            }
        }
    }

    private static class ValueJsonSerializer extends JsonSerializer<Value> {
        @Override
        public void serialize(Value value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null)
                return;
            if (value instanceof Null) {
                gen.writeNull();
            } else {
                try {
                    gen.writeRawValue(value.toJson());
                } catch (ValueException ex) {
                    throw new IOException(ex);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public ModelValueModule() {
        super("ModelValueModule", new Version(1, 0, 0, "latest", null, null));
        ValueJsonSerializer serializer = new ValueJsonSerializer();
        ValueJsonDeserializer deserializer = new ValueJsonDeserializer();
        this.addSerializer(Value.class, serializer);
        this.addDeserializer(ObjectValue.class, deserializer);
        this.addDeserializer(ArrayValue.class, deserializer);
        this.addDeserializer(Null.class, deserializer);
        this.addDeserializer(StringValue.class, deserializer);
        this.addDeserializer(NumberValue.class, deserializer);
        this.addDeserializer(BooleanValue.class, deserializer);
    }
}
