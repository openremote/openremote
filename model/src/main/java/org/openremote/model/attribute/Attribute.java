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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.openremote.model.asset.Asset;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores a named value with associated {@link MetaItem}s.
 */
@JsonDeserialize(using = Attribute.AttributeDeserializer.class)
public class Attribute<T> extends AbstractNameValueHolder<T> implements MetaHolder {

    public static class AttributeDeserializer extends StdDeserializer<Attribute<?>> {

        protected AttributeDeserializer() {
            super(Attribute.class);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Attribute<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            // Need to find the type field to know how to deserialise the value
            TokenBuffer tokenBuffer = new TokenBuffer(jp);
            tokenBuffer.copyCurrentStructure(jp);
            JsonParser jp2 = tokenBuffer.asParser();
            JsonParser jp3 = tokenBuffer.asParser();
            String attributeValueType = null;
            int level = 1;
            jp2.nextToken();

            while (level > 0) {
                JsonToken nextToken = jp2.nextToken();

                if (nextToken == JsonToken.START_OBJECT) {
                    level++;
                    continue;
                }

                if (nextToken == JsonToken.END_OBJECT) {
                    level--;
                    continue;
                }

                if (level == 1 && jp2.currentName().equals("type")) {
                    jp2.nextToken();
                    attributeValueType = jp2.getValueAsString();
                    break;
                }
            }

            if (attributeValueType == null) {
                throw new JsonParseException(jp, "Failed to extract attribute type information");
            }

            // Get inner attribute type or fallback to primitive/JSON type
            Optional<ValueDescriptor<?>> valueDescriptor = ValueUtil.getValueDescriptor(attributeValueType);
            Attribute attribute = new Attribute<>();

            while (jp3.nextToken() != JsonToken.END_OBJECT) {
                if (jp3.currentToken() == JsonToken.FIELD_NAME) {
                    String propName = jp3.currentName();
                    JsonToken token = jp3.nextToken();
                    if (token == JsonToken.VALUE_NULL) {
                        continue;
                    }
                    switch (propName) {
                        case "meta":
                            attribute.meta = jp3.readValueAs(MetaMap.class);
                            break;
                        case "name":
                            attribute.name = jp3.readValueAs(String.class);
                            break;
                        case "timestamp":
                            attribute.timestamp = jp3.readValueAs(Long.class);
                            break;
                        case "value":
                            @SuppressWarnings("unchecked")
                            Class valueType = valueDescriptor.map(ValueDescriptor::getType)
                                .orElseGet(() -> (Class) Object.class);
                            attribute.value = jp3.readValueAs(valueType);
                            break;
                    }
                }
            }

            // Get the value descriptor from the value if it isn't known
            attribute.type = valueDescriptor.orElseGet(() -> {
                if (attribute.value == null) {
                    return ValueDescriptor.UNKNOWN;
                }
                Object value = attribute.value;
                return ValueUtil.getValueDescriptorForValue(value);
            });

            return (Attribute<?>) attribute;
        }
    }

    @Valid
    protected MetaMap meta;
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    protected long timestamp;

    Attribute() {
    }

    public Attribute(AttributeDescriptor<T> attributeDescriptor) {
        this(attributeDescriptor, null);
    }

    public Attribute(AttributeDescriptor<T> attributeDescriptor, T value) {
        this(attributeDescriptor.getName(), attributeDescriptor.getType(), value);

        // Auto merge meta from attribute descriptor
        if (attributeDescriptor.getMeta() != null) {
            getMeta().addOrReplace(attributeDescriptor.getMeta());
        }
    }

    public Attribute(AttributeDescriptor<T> attributeDescriptor, T value, long timestamp) {
        this(attributeDescriptor, value);
        setTimestamp(timestamp);
    }

    @SuppressWarnings("unchecked")
    public Attribute(String name, ValueDescriptor<?> valueDescriptor) {
        this(name, (ValueDescriptor<T>) valueDescriptor, null);
    }

    public Attribute(String name, ValueDescriptor<T> valueDescriptor, T value) {
        super(name, valueDescriptor, value);
    }

    public Attribute(String name, ValueDescriptor<T> valueDescriptor, T value, long timestamp) {
        this(name, valueDescriptor, value);
        setTimestamp(timestamp);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list.
     */
    public static Stream<Attribute<?>> getAddedOrModifiedAttributes(Collection<Attribute<?>> oldAttributes,
                                                                    Collection<Attribute<?>> newAttributes) {
        return getAddedOrModifiedAttributes(oldAttributes, newAttributes, null);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list.
     */
    public static Stream<Attribute<?>> getAddedOrModifiedAttributes(Collection<Attribute<?>> oldAttributes,
                                                                    Collection<Attribute<?>> newAttributes,
                                                                    Predicate<String> ignoredAttributeNames) {
        return getAddedOrModifiedAttributes(
            oldAttributes,
            newAttributes,
            null,
            ignoredAttributeNames);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list
     */
    public static Stream<Attribute<?>> getAddedOrModifiedAttributes(Collection<Attribute<?>> oldAttributes,
                                                                    Collection<Attribute<?>> newAttributes,
                                                                    Predicate<String> limitToAttributeNames,
                                                                    Predicate<String> ignoredAttributeNames) {
        return newAttributes.stream()
            .filter(newAttribute -> {
                    if (limitToAttributeNames != null && !limitToAttributeNames.test(newAttribute.getName())) {
                        return false;
                    }

                    if (ignoredAttributeNames != null && ignoredAttributeNames.test(newAttribute.getName())) {
                        return false;
                    }

                    return oldAttributes.stream().filter(attribute ->
                        attribute.getName().equals(newAttribute.getName()))
                        .findFirst()
                        .map(attribute -> {
                            // Attribute may have been modified do basic equality check
                            return !attribute.equals(newAttribute);
                        })
                        .orElse(true); // Attribute is new
                }
            );
    }

    // For JPA/Hydrators
    void setNameInternal(String name) {
        this.name = name;
    }

    public MetaMap getMeta() {
        if (meta == null) {
            meta = new MetaMap();
        }

        return meta;
    }

    public Attribute<T> setMeta(MetaMap meta) {
        this.meta = meta;
        return this;
    }

    public Attribute<T> addMeta(@NotNull MetaMap meta) {
        getMeta().addAll(meta);
        return this;
    }

    public Attribute<T> addMeta(@NotNull MetaItem<?>... meta) {
        getMeta().addAll(meta);
        return this;
    }

    public Attribute<T> addMeta(@NotNull Collection<MetaItem<?>> meta) {
        getMeta().addAll(meta);
        return this;
    }

    public Attribute<T> addOrReplaceMeta(@NotNull MetaMap meta) {
        getMeta().addAll(meta);
        return this;
    }

    public Attribute<T> addOrReplaceMeta(@NotNull MetaItem<?>... meta) {
        return addOrReplaceMeta(Arrays.asList(meta));
    }

    public Attribute<T> addOrReplaceMeta(@NotNull Collection<MetaItem<?>> meta) {
        getMeta().addAll(meta);
        return this;
    }

    public <U> Optional<U> getMetaValue(MetaItemDescriptor<U> metaItemDescriptor) {
        return getMeta().getValue(metaItemDescriptor);
    }

    public <U> U getMetaValueOrDefault(MetaItemDescriptor<U> metaItemDescriptor) {
        return getMeta().getValueOrDefault(metaItemDescriptor);
    }

    public boolean hasMeta(MetaItemDescriptor<?> metaItemDescriptor) {
        return getMeta().has(metaItemDescriptor);
    }

    public boolean hasMeta(String metaItemName) {
        return getMeta().has(metaItemName);
    }

    public <U> Optional<MetaItem<U>> getMetaItem(MetaItemDescriptor<U> metaItemDescriptor) {
        return getMeta().get(metaItemDescriptor);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
        timestamp = 0L;
    }

    public void setValue(T value, long timestamp) {
        super.setValue(value);
        this.timestamp = timestamp;
    }

    @JsonIgnore
    public Optional<Long> getTimestamp() {
        return hasExplicitTimestamp() ? Optional.of(Math.abs(timestamp)) : Optional.empty();
    }

    public Attribute<T> setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public boolean hasExplicitTimestamp() {
        return timestamp > 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            ", timestamp='" + getTimestamp().orElse(0L) + '\'' +
            "} ";
    }

    public String toStringAll() {
        return getClass().getSimpleName() + "{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            ", timestamp='" + getTimestamp().orElse(0L) + '\'' +
            ", meta='" + (meta == null ? "" : getMeta().values().stream().map(MetaItem::toString).collect(Collectors.joining(","))) + '\'' +
            "} ";
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(timestamp) + Objects.hash(meta);
    }

    /**
     * Super fast equality checking using the name, type and timestamp; when an {@link Attribute} value is updated then
     * the timestamp must be greater than the existing value timestamp; and also when merging an {@link Asset} any
     * modified {@link Attribute} should have a newer timestamp; the backend should take care to update the {@link
     * Attribute} timestamp when merging an {@link Asset} by using {@link #deepEquals}.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Attribute))
            return false;
        Attribute<?> that = (Attribute<?>) obj;

        return Objects.equals(timestamp, that.timestamp)
            && Objects.equals(name, that.name)
            && Objects.equals(type, that.type);
    }

    /**
     * Deep equality check looking at value and meta; this is needed when merging an {@link Asset} as the {@link
     * Attribute} meta and value can be modified without the timestamp being updated; the backend should take care to
     * update timestamps when meta or value is updated during a merge so that the standard {@link #equals} logic can be
     * used in all other cases. Meta value and value equality checking is done by converting value to {@link JsonNode}
     * as all {@link Attribute} and {@link MetaItem} values must be serializable but this doesn't mean that the value
     * type has an equality override so this is the safest mechanism.
     */
    public boolean deepEquals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Attribute))
            return false;
        Attribute<?> that = (Attribute<?>) obj;

        boolean timestampMatches = Objects.equals(timestamp, that.timestamp);
        boolean metaMatches = (meta == null && that.meta != null && that.meta.isEmpty()) || (that.meta == null && meta != null && meta.isEmpty()) || Objects.equals(meta, that.meta);
        boolean superMatches = super.equals(obj);
        boolean result = timestampMatches && metaMatches && superMatches;
        return result;
    }

    public boolean equals(Object obj, Comparator<Attribute<?>> comparator) {
        if (comparator == null) {
            return equals(obj);
        }
        if (obj == null)
            return false;
        if (!(obj instanceof Attribute))
            return false;
        Attribute<?> that = (Attribute<?>) obj;
        return comparator.compare(this, that) == 0;
    }
}
