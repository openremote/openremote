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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.util.ValueUtil;

public class AgentDescriptorDeserializerJackson2 extends StdDeserializer<AgentDescriptor<?, ?, ?>> {

  public AgentDescriptorDeserializerJackson2() {
    super(AgentDescriptor.class);
  }

  @Override
  public AgentDescriptor<?, ?, ?> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);

    if (node == null || node.isNull()) {
      return null;
    }

    JsonNode nameNode = node.get("name");
    if (nameNode == null || nameNode.isNull()) {
      throw JsonMappingException.from(parser, "Agent descriptor name is missing");
    }

    AssetDescriptor<?> descriptor = ValueUtil.getAssetDescriptor(nameNode.asText()).orElse(null);
    if (descriptor instanceof AgentDescriptor<?, ?, ?> agentDescriptor) {
      return agentDescriptor;
    }

    throw JsonMappingException.from(parser, "Unknown agent descriptor: " + nameNode.asText());
  }
}
