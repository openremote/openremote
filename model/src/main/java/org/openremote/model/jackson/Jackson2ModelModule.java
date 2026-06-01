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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.impl.ColourRGB;

/**
 * Registers Jackson 2 serializers and deserializers for model types that are now primarily handled by Jackson 3.
 */
public class Jackson2ModelModule extends SimpleModule {

    @SuppressWarnings("unchecked")
    public Jackson2ModelModule() {
        super("Jackson2ModelModule");

        addSerializer((Class<Attribute<?>>) (Class<?>) Attribute.class, new AttributeSerializerJackson2());
        addDeserializer((Class<Attribute<?>>) (Class<?>) Attribute.class, new AttributeDeserializerJackson2());
        addDeserializer((Class<AssetDescriptor<?>>) (Class<?>) AssetDescriptor.class, new AssetDescriptorDeserializerJackson2());
        addDeserializer((Class<AgentDescriptor<?, ?, ?>>) (Class<?>) AgentDescriptor.class, new AgentDescriptorDeserializerJackson2());
        addSerializer((Class<AgentLink<?>>) (Class<?>) AgentLink.class, new AgentLinkSerializerJackson2());
        addDeserializer((Class<AgentLink<?>>) (Class<?>) AgentLink.class, new AgentLinkDeserializerJackson2());
        addSerializer(tools.jackson.databind.JsonNode.class, new JsonNodeSerializerJackson2<>(tools.jackson.databind.JsonNode.class));
        addDeserializer(tools.jackson.databind.JsonNode.class, new JsonNodeDeserializerJackson2<>(tools.jackson.databind.JsonNode.class));
        addSerializer(tools.jackson.databind.node.ObjectNode.class, new JsonNodeSerializerJackson2<>(tools.jackson.databind.node.ObjectNode.class));
        addDeserializer(tools.jackson.databind.node.ObjectNode.class, new JsonNodeDeserializerJackson2<>(tools.jackson.databind.node.ObjectNode.class));
        addSerializer((Class<MetaItem<?>>) (Class<?>) MetaItem.class, new MetaItemSerializerJackson2());
        addDeserializer(MetaMap.class, new MetaMapDeserializerJackson2());
        addDeserializer(ColourRGB.class, new ColourRGBDeserializerJackson2());
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(
                DeserializationConfig config,
                BeanDescription beanDesc,
                JsonDeserializer<?> deserializer
            ) {
                if (ValueDescriptor.class.isAssignableFrom(beanDesc.getBeanClass())) {
                    return new ValueDescriptor.ValueDescriptorDeserializerJackson2(deserializer);
                }
                return deserializer;
            }
        });
    }
}
