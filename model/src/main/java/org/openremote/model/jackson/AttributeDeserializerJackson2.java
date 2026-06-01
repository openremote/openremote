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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;

public class AttributeDeserializerJackson2 extends StdDeserializer<Attribute<?>> {

    public AttributeDeserializerJackson2() {
        super(Attribute.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Attribute<?> deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        if (!parser.isExpectedStartObjectToken()) {
            throw JsonMappingException.from(parser, "Attribute must be an object");
        }

        String attributeName = parser.currentName();
        JsonNode node = parser.getCodec().readTree(parser);

        if (node.hasNonNull("name")) {
            String name = node.get("name").asText();
            if (attributeName != null && !attributeName.equals(name)) {
                throw JsonMappingException.from(parser, "Attribute name doesn't match attribute map key");
            }
            attributeName = name;
        }

        if (attributeName == null) {
            throw JsonMappingException.from(parser, "Attribute name is missing");
        }

        ValueDescriptor<?> valueDescriptor = null;
        if (node.hasNonNull("type")) {
            valueDescriptor = ValueUtil.getValueDescriptor(node.get("type").asText()).orElse(null);
        }

        Attribute attribute = valueDescriptor != null ? new Attribute(attributeName, valueDescriptor) : new Attribute(attributeName);

        if (node.hasNonNull("meta")) {
            attribute.setMeta(parser.getCodec().treeToValue(node.get("meta"), MetaMap.class));
        }

        if (node.hasNonNull("value")) {
            Class<?> valueType = valueDescriptor != null ? valueDescriptor.getType() : Object.class;
            attribute.setValue(convertValue(parser, node.get("value"), valueType));
        }

        if (node.hasNonNull("timestamp")) {
            attribute.setTimestamp(node.get("timestamp").asLong());
        }

        return attribute;
    }

    protected Object convertValue(JsonParser parser, JsonNode valueNode, Class<?> valueType) throws JsonMappingException {
        try {
            return ValueUtil.JSON.readValue(valueNode.toString(), ValueUtil.JSON.constructType(valueType));
        } catch (tools.jackson.core.JacksonException e) {
            throw JsonMappingException.from(parser, "Failed to deserialize attribute value as " + valueType.getName(), e);
        }
    }
}
