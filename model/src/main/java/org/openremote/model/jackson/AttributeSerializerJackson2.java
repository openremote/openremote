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
package org.openremote.model.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.openremote.model.attribute.Attribute;

import java.io.IOException;

public class AttributeSerializerJackson2 extends StdSerializer<Attribute<?>> {

    public AttributeSerializerJackson2() {
        super((Class<Attribute<?>>) (Class<?>) Attribute.class);
    }

    @Override
    public void serialize(Attribute<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName());
        if (value.getType() != null) {
            gen.writeStringField("type", value.getType().getName());
        }
        if (value.getMeta() != null && !value.getMeta().isEmpty()) {
            gen.writeObjectField("meta", value.getMeta());
        }
        value.getValue().ifPresent(v -> {
            try {
                gen.writeObjectField("value", v);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        value.getTimestamp().ifPresent(timestamp -> {
            try {
                gen.writeNumberField("timestamp", timestamp);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        gen.writeEndObject();
    }
}
