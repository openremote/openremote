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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.openremote.model.util.ValueUtil;

public class JsonNodeDeserializerJackson2<T extends tools.jackson.databind.JsonNode>
    extends StdDeserializer<T> {

  protected final Class<T> targetType;

  public JsonNodeDeserializerJackson2(Class<T> targetType) {
    super(targetType);
    this.targetType = targetType;
  }

  @Override
  public T deserialize(JsonParser parser, DeserializationContext context)
      throws IOException, JsonProcessingException {
    JsonNode node = parser.getCodec().readTree(parser);

    try {
      return ValueUtil.JSON.readValue(node.toString(), targetType);
    } catch (tools.jackson.core.JacksonException e) {
      throw JsonMappingException.from(
          parser, "Failed to deserialize JSON node as " + targetType.getName(), e);
    }
  }
}
