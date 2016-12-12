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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import elemental.json.Json;
import org.openremote.model.Attributes;

import java.io.IOException;

public class AttributesJsonModule extends SimpleModule {

    private static class AttributesDeserializer extends StdDeserializer<Attributes> {

        public AttributesDeserializer() {
            this(null);
        }

        public AttributesDeserializer(Class<Attributes> t) {
            super(t);
        }

        @Override
        public Attributes deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new Attributes(Json.parse(p.getText()));
        }
    }

    private static class AttributesSerializer extends StdSerializer<Attributes> {

        public AttributesSerializer() {
            this(null);
        }

        public AttributesSerializer(Class<Attributes> t) {
            super(t);
        }

        @Override
        public void serialize(Attributes value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.getJsonObject());
        }
    }


    public AttributesJsonModule() {
        super("AttributesJsonModule", new Version(1, 0, 0, "latest", null, null));
        AttributesSerializer serializer = new AttributesSerializer();
        AttributesDeserializer deserializer = new AttributesDeserializer();
        this.addSerializer(Attributes.class, serializer);
        this.addDeserializer(Attributes.class, deserializer);
    }
}
