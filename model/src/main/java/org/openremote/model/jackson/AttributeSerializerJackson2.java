/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.openremote.model.attribute.Attribute;

public class AttributeSerializerJackson2 extends StdSerializer<Attribute<?>> {

  public AttributeSerializerJackson2() {
    super((Class<Attribute<?>>) (Class<?>) Attribute.class);
  }

  @Override
  public void serialize(Attribute<?> value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("name", value.getName());
    if (value.getType() != null) {
      gen.writeStringField("type", value.getType().getName());
    }
    // Always write meta and value (even when null/empty) to match the Jackson 3 model serializer
    provider.defaultSerializeField("meta", value.getMeta(), gen);
    provider.defaultSerializeField("value", value.getValue().orElse(null), gen);
    value
        .getTimestamp()
        .ifPresent(
            timestamp -> {
              try {
                gen.writeNumberField("timestamp", timestamp);
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
            });
    gen.writeEndObject();
  }
}
