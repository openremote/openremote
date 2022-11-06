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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.*;

import java.io.IOException;
import java.util.*;

@JsonDeserialize(using = MetaMap.MetaObjectDeserializer.class)
public class MetaMap extends NamedMap<MetaItem<?>> {
    /**
     * Deserialise a {@link MetaMap} that is represented as a JSON object where each key is the name of a
     * {@link MetaItemDescriptor}
     */
    public static class MetaObjectDeserializer extends StdDeserializer<MetaMap> {

        public MetaObjectDeserializer() {
            super(MetaMap.class);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public MetaMap deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (!jp.isExpectedStartObjectToken()) {
                throw new InvalidFormatException(jp, "Expected an object but got something else", jp.nextValue(), MetaMap.class);
            }

            List<MetaItem<?>> list = new ArrayList<>();

            while(jp.nextToken() != JsonToken.END_OBJECT) {
                if(jp.currentToken() == JsonToken.FIELD_NAME) {
                    String metaItemName = jp.getCurrentName();
                    jp.nextToken();

                    MetaItem metaItem = new MetaItem<>();
                    metaItem.setNameInternal(metaItemName);

                    // Find the meta descriptor for this meta item as this will give us value type also; fallback to
                    // OBJECT type meta item to allow deserialization of meta that doesn't exist in the current asset model
                    Optional<ValueDescriptor<?>> valueDescriptor = ValueUtil.getMetaItemDescriptor(metaItemName)
                        .map(MetaItemDescriptor::getType);

                    Class valueType = valueDescriptor.map(ValueDescriptor::getType).orElseGet(() -> (Class) JsonNode.class);
                    metaItem.setValue(jp.readValueAs(valueType));

                    // Get the value descriptor from the value if it isn't known
                    metaItem.setTypeInternal(valueDescriptor.orElseGet(() -> {
                        if (!metaItem.getValue().isPresent()) {
                            return ValueDescriptor.UNKNOWN;
                        }
                        Object value = metaItem.getValue().orElse(null);
                        return ValueUtil.getValueDescriptorForValue(value);
                    }));

                    list.add(metaItem);
                }
            }

            MetaMap metaMap = new MetaMap();
            metaMap.putAllSilent(list);
            return metaMap;
        }
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

    /**
     * Need to declare equals here as {@link com.vladmihalcea.hibernate.type.json.internal.JsonTypeDescriptor} uses
     * {@link Class#getDeclaredMethod} to find it...
     */
    @Override
    public boolean equals(@Nullable Object object) {
        return super.equals(object);
    }
}
