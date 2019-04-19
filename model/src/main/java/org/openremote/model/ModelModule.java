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
package org.openremote.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.openremote.model.geo.Position;
import org.openremote.model.value.*;

import java.io.IOException;

public class ModelModule extends SimpleModule {

    public static class ValueJsonDeserializer<T extends Value> extends StdDeserializer<T> {

        public ValueJsonDeserializer() {
            super(Value.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            try {
                return (T) Values.instance().parse(jsonParser.getCodec().readTree(jsonParser).toString()).
                    orElseThrow(() -> new IOException("Empty JSON data"));
            } catch (ValueException ex) {
                throw new IOException(ex);
            }
        }
    }

    public static class ValueJsonSerializer extends JsonSerializer<Value> {
        @Override
        public void serialize(Value value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                try {
                    gen.writeRawValue(value.toJson());
                } catch (ValueException ex) {
                    throw new IOException(ex);
                }
            }
        }
    }

    public static class PositionSerializer extends JsonSerializer<Position> {

        @Override
        public void serialize(Position value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeArray(value.getValues(), 0, value.getValues().length);
        }
    }

    public static class PositionDeserializer extends JsonDeserializer<Position> {

        @Override
        public Position deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            double[] values = p.getCodec().readValue(p, double[].class);
            return new Position(values[0], values[1]);
        }
    }


    @SuppressWarnings("unchecked")
    public ModelModule() {
        super("ModelValueModule", new Version(1, 0, 0, "latest", null, null));
        ValueJsonSerializer valueSerializer = new ValueJsonSerializer();
        ValueJsonDeserializer valueDeserializer = new ValueJsonDeserializer();
        this.addSerializer(Value.class, valueSerializer);
        this.addDeserializer(Value.class, valueDeserializer);
        this.addDeserializer(ObjectValue.class, valueDeserializer);
        this.addDeserializer(ArrayValue.class, valueDeserializer);
        this.addDeserializer(StringValue.class, valueDeserializer);
        this.addDeserializer(NumberValue.class, valueDeserializer);
        this.addDeserializer(BooleanValue.class, valueDeserializer);
        PositionSerializer positionSerializer = new PositionSerializer();
        PositionDeserializer positionDeserializer = new PositionDeserializer();
        this.addSerializer(Position.class, positionSerializer);
        this.addDeserializer(Position.class, positionDeserializer);
    }
}
