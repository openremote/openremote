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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemDescriptor;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class MetaMapDeserializerJackson2 extends StdDeserializer<MetaMap> {

    public MetaMapDeserializerJackson2() {
        super(MetaMap.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MetaMap deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw JsonMappingException.from(parser, "MetaMap must be an object");
        }

        MetaMap metaMap = new MetaMap();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode valueNode = unwrapMetaValue(field.getValue());
            MetaItemDescriptor<?> descriptor = ValueUtil.getMetaItemDescriptor(name).orElse(null);
            MetaItem metaItem = descriptor != null
                ? new MetaItem(descriptor.getName(), descriptor.getType())
                : new MetaItem(name);

            if (valueNode != null && !valueNode.isNull()) {
                Class<?> valueType = descriptor != null ? descriptor.getType().getType() : Object.class;
                metaItem.setValue(parser.getCodec().treeToValue(valueNode, valueType));
            }
            metaMap.addOrReplace(metaItem);
        }
        return metaMap;
    }

    protected JsonNode unwrapMetaValue(JsonNode node) {
        if (node != null && node.isObject() && node.has("value")) {
            return node.get("value");
        }
        return node;
    }
}
