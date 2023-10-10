/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.attribute;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@JsonDeserialize(using = MetaMap.MetaObjectDeserializer.class)
public class MetaMap extends NamedMap<MetaItem<?>> {
    /**
     * Deserialise a {@link MetaMap} that is represented as a JSON object where each key is the name of a
     * {@link MetaItemDescriptor}
     */
    protected static class MetaObjectDeserializer extends StdDeserializer<MetaMap> {

        public MetaObjectDeserializer() {
            super(MetaMap.class);
        }

        @Override
        public MetaMap deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            List<MetaItem<?>> list = deserialiseMetaMap(jp, ctxt, (metaItemName) -> ValueUtil.getMetaItemDescriptor(metaItemName)
                .map(MetaItemDescriptor::getType).orElse(null));

            MetaMap metaMap = new MetaMap();
            metaMap.putAll(list);
            return metaMap;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<MetaItem<?>> deserialiseMetaMap(JsonParser jp, DeserializationContext ctxt, Function<String, ValueDescriptor<?>> metaNameToValueDescriptorFunction) throws IOException {
        if (!jp.isExpectedStartObjectToken()) {
            throw new InvalidFormatException(jp, "MetaMap must be an object", jp.nextValue(), MetaMap.class);
        }

        List<MetaItem<?>> list = new ArrayList<>();

        while(jp.nextToken() != JsonToken.END_OBJECT) {
            if(jp.currentToken() == JsonToken.FIELD_NAME) {
                jp.nextToken();
            }
            if (jp.currentToken() == JsonToken.VALUE_NULL) {
                continue;
            }

            String metaItemName = jp.getCurrentName();
            ValueDescriptor<?> valueDescriptor = metaNameToValueDescriptorFunction.apply(metaItemName);

            MetaItem metaItem;
            if (valueDescriptor != null) {
                metaItem = new MetaItem(metaItemName, valueDescriptor);
            } else {
                metaItem = new MetaItem(metaItemName);
            }

            metaItem.setValue(Attribute.AttributeDeserializer.deserialiseValue(valueDescriptor, jp, ctxt));

            list.add(metaItem);
        }

        return list;
    }

    public MetaMap() {
    }

    public MetaMap(Collection<? extends MetaItem<?>> c) {
        super(c);
    }

    public MetaMap(Map<? extends String, ? extends MetaItem<?>> map) {
        super(map);
    }

    // This works around the crappy type system and avoids the need for a type witness
    public <S> Optional<MetaItem<S>> get(MetaItemDescriptor<S> metaDescriptor) {
        return super.get(metaDescriptor);
    }

    public <U extends MetaItemDescriptor<?>> void remove(U nameHolder) {
        super.remove(nameHolder.getName());
    }

    public <S> MetaItem<S> getOrCreate(MetaItemDescriptor<S> metaDescriptor) {
        MetaItem<S> metaItem = get(metaDescriptor).orElse(new MetaItem<>(metaDescriptor));
        addOrReplace(metaItem);
        return metaItem;
    }
}
