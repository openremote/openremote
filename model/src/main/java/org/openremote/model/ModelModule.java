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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetDescriptorImpl;
import org.openremote.model.attribute.*;
import org.openremote.model.geo.Position;
import org.openremote.model.util.AssetModelUtil;
import org.openremote.model.value.*;

import java.io.IOException;
import java.util.function.Function;

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

    public static class AttributeValueDescriptorSerializer extends JsonSerializer<Position> {

        @Override
        public void serialize(Position value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeArray(value.getValues(), 0, value.getValues().length);
        }
    }

    public static class DescriptorDeserializer<T, U extends T> extends JsonDeserializer<T> {

        protected Function<String, T> descriptorNameFinder;
        protected Class<U> implClass;

        public DescriptorDeserializer(Function<String, T> descriptorNameFinder, Class<U> implClass) {
            this.descriptorNameFinder = descriptorNameFinder;
            this.implClass = implClass;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            if (node instanceof TextNode) {
                return descriptorNameFinder.apply(node.textValue());
            } else {
                return p.getCodec().treeToValue(node, implClass);
            }
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
        this.addDeserializer(AssetDescriptor.class, new DescriptorDeserializer<>(
            name -> AssetModelUtil.getAssetDescriptor(name).orElse(null),
            AssetDescriptorImpl.class
        ));
        this.addDeserializer(AttributeDescriptor.class, new DescriptorDeserializer<>(
            name -> AssetModelUtil.getAttributeDescriptor(name).orElse(null),
            AttributeDescriptorImpl.class
        ));
        this.addDeserializer(AttributeValueDescriptor.class, new DescriptorDeserializer<>(
            name -> AssetModelUtil.getAttributeValueDescriptor(name).orElse(null),
            AttributeValueDescriptorImpl.class
        ));
        this.addDeserializer(MetaItemDescriptor.class, new DescriptorDeserializer<>(
            urn -> AssetModelUtil.getMetaItemDescriptor(urn).orElse(null),
            MetaItemDescriptorImpl.class
        ));
    }
}
