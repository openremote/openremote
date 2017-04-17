/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model;

import com.google.gwt.regexp.shared.RegExp;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import org.openremote.model.util.JsonUtil;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Convenience overlay API for {@link JsonObject}.
 */
public abstract class Attribute extends AbstractValueTimestampHolder {

    public static final class Functions {

        private Functions() {}

        public static <A extends Attribute> List<A> removeAttribute(List<A> attributes, String name) {
            attributes.removeIf(attribute -> attribute.getName().equals(name));
            return attributes;
        }

        public static <A extends Attribute> Optional<A> getAttribute(List<A> attributes, String name) {
            return attributes
                .stream()
                .filter(attribute -> attribute.getName().equals(name))
                .findFirst();
        }

        public static Function<Stream<? extends Attribute>, Optional<? extends Attribute>> getAttribute(String name) {
            return attributeStream -> attributeStream
                .filter(attribute -> attribute.getName().equals(name))
                .findFirst();
        }

        public static <A extends Attribute> Function<A, Optional<AttributeRef>> getAttributeLink(String metaName) {
            return attribute -> attribute.getMetaItem(metaName)
                .filter(item -> item.getValue().getType() == JsonType.ARRAY)
                .map(AbstractValueHolder::getValueAsArray)
                .map(AttributeRef::new);
        }

        @SuppressWarnings("unchecked")
        public static <A extends Attribute> UnaryOperator<A> setAttributeLink(String metaName, AttributeRef attributeRef) {
            return attribute -> {
                if (attributeRef == null) {
                    throw new IllegalArgumentException(
                        "Attribute reference cannot be null for '" + metaName + "' on: " + attribute
                    );
                }
                return (A)attribute.setMeta(
                    attribute.getMeta().replace(metaName, new MetaItem(metaName, attributeRef.asJsonValue()))
                );
            };
        }

        @SuppressWarnings("unchecked")
        public static <A extends Attribute> UnaryOperator<A> removeAttributeLink(String metaName) {
            return attribute -> (A)attribute.setMeta(attribute.getMeta().removeAll(metaName));
        }

        public static <A extends Attribute> Predicate<A> isAttributeLink(String metaName) {
            return attribute -> getAttributeLink(metaName)
                .apply(attribute)
                .isPresent();
        }

        public static <A extends Attribute> Predicate<A> isValid() {
            return A::isValid;
        }

        public static <A extends Attribute> Predicate<A> notValid() {
            return attribute -> !attribute.isValid();
        }

        public static <A extends Attribute> Predicate<A> isOfType(AttributeType attributeType) {
            return attribute -> attribute.getType().equals(attributeType);
        }

        public static <A extends Attribute> Predicate<A> isValueEqualTo(JsonValue value) {
            return attribute -> JsonUtil.equals(attribute.getValue(), value);
        }

        public static <A extends Attribute> Predicate<A> hasMetaItem(String metaName) {
            return attribute -> attribute.hasMetaItem(metaName);
        }

        public static <A extends Attribute> Predicate<A> hasMetaItem(String metaName, JsonValue value) {
            return attribute -> attribute.hasMetaItem(metaName, value);
        }

        public static <A extends Attribute, T extends JsonValue> Predicate<A> hasMetaItem(String metaName, Class<T> clazz) {
            return attribute -> attribute.hasMetaItem(metaName, clazz);
        }

        public static <A extends Attribute> Function<A, Optional<MetaItem>> getMetaItem(String metaName) {
            return attribute -> attribute.getMetaItem(metaName);
        }

        public static <A extends Attribute> Function<A, Optional<MetaItem>> getMetaItem(String metaName, JsonValue value) {
            return attribute -> attribute.getMetaItem(metaName, value);
        }

        public static <A extends Attribute, T extends JsonValue> Function<A, Optional<MetaItem>> getMetaItem(String metaName, Class<T> clazz) {
            return attribute -> attribute.getMetaItem(metaName, clazz);
        }

        public static <A extends Attribute> Function<A, List<MetaItem>> getMetaItems(String metaName) {
            return attribute -> attribute.getMetaItems(metaName);
        }

        public static <A extends Attribute> Function<A, List<MetaItem>> getMetaItems(String metaName, JsonValue value) {
            return attribute -> attribute.getMetaItems(metaName, value);
        }

        public static <A extends Attribute, T extends JsonValue> Function<A, List<MetaItem>> getMetaItems(String metaName, Class<T> clazz) {
            return attribute -> attribute.getMetaItems(metaName, clazz);
        }
    }

    /**
     * Attribute names should be very simple, as we use them in SQL path
     * expressions, etc. and must manually escape.
     */
    public static final String ATTRIBUTE_NAME_PATTERN = "^\\w+$";
    public static final RegExp ATTRIBUTE_NAME_REGEXP = RegExp.compile(ATTRIBUTE_NAME_PATTERN);
    public static final Predicate<String> ATTRIBUTE_NAME_VALIDATOR =
        name -> name != null && name.length() > 0 && ATTRIBUTE_NAME_REGEXP.test(name);

    public static final String TYPE_FIELD_NAME = "type";
    public static final String META_FIELD_NAME = "meta";

    protected String name;

    protected Attribute(String name) {
        this(name, Json.createObject());
    }

    protected Attribute(String name, AttributeType type) {
        super(Json.createObject());
        setName(name);
        setType(type);
    }

    protected Attribute(String name, JsonObject jsonObject) {
        super(jsonObject);
        setName(name);
    }

    protected Attribute(String name, AttributeType type, JsonValue value) {
        this(name, type);
        setValue(value);
    }

    protected Attribute(Attribute attribute) {
        this(attribute.getName(), attribute.getType(), attribute.getJsonObject());
    }

    @Override
    protected boolean isValidValue(JsonValue value) {
        return getType().isValid(value);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public AttributeType getType() {
        String typeName = jsonObject.hasKey(TYPE_FIELD_NAME) ? jsonObject.get(TYPE_FIELD_NAME).asString() : null;
        return typeName != null ? AttributeType.fromValue(typeName) : AttributeType.NULL;
    }

    public void setType(AttributeType type) {
        if (type == null) {
            type = AttributeType.NULL;
        }

        if (getType() == type) {
            return;
        }

        jsonObject.put(TYPE_FIELD_NAME, Json.create(type.getValue()));

        // Remove the old value and timestamp
        jsonObject.remove(VALUE_TIMESTAMP_FIELD_NAME);
        setValueUnchecked(Json.createNull());
    }

    public boolean hasMeta() {
        return jsonObject.hasKey(META_FIELD_NAME);
    }

    public Meta getMeta() {
        return hasMeta() ? new Meta(jsonObject.getArray(META_FIELD_NAME)) : new Meta();
    }

    @SuppressWarnings("unchecked")
    public Attribute setMeta(Meta meta) {
        if (meta != null) {
            jsonObject.put(META_FIELD_NAME, meta.getJsonArray());
        } else if (jsonObject.hasKey(META_FIELD_NAME)) {
            jsonObject.remove(META_FIELD_NAME);
        }
        return this;
    }

    public Stream<MetaItem> getMetaItemStream() {
        return getMeta().stream();
    }

    public List<MetaItem> getMetaItems(String name) {
        return hasMeta() ? getMeta().getAll(name) : Collections.emptyList();
    }

    public List<MetaItem> getMetaItems(String name, JsonValue value) {
        return hasMeta() ? getMeta().getAll(name, value) : Collections.emptyList();
    }

    public <T extends JsonValue> List<MetaItem> getMetaItems(String name, Class<T> clazz) {
        return hasMeta() ? getMeta().getAll(name, clazz) : Collections.emptyList();
    }

    public List<MetaItem> getMetaItems(Predicate<MetaItem> filter) {
        return hasMeta() ? getMeta().getAll(filter) : Collections.emptyList();
    }

    public Optional<MetaItem> getMetaItem(int index) {
        return hasMeta() ? getMeta().get(index) : Optional.empty();
    }

    public Optional<MetaItem> getMetaItem(String name) {
        return hasMeta() ? getMeta().get(name) : Optional.empty();
    }

    public Optional<MetaItem> getMetaItem(String name, int startIndex) {
        return hasMeta() ? getMeta().get(name, startIndex) : Optional.empty();
    }

    public Optional<MetaItem> getMetaItem(String name, JsonValue value) {
        return hasMeta() ? getMeta().get(name, value) : Optional.empty();
    }

    public Optional<MetaItem> getMetaItem(String name, JsonValue value, int startIndex) {
        return hasMeta() ? getMeta().get(name, value, startIndex) : Optional.empty();
    }

    public <T extends JsonValue> Optional<MetaItem> getMetaItem(String name, Class<T> clazz) {
        return hasMeta() ? getMeta().get(name, clazz) : Optional.empty();
    }

    public <T extends JsonValue> Optional<MetaItem> getMetaItem(String name, Class<T> clazz, int startIndex) {
        return hasMeta() ? getMeta().get(name, clazz, startIndex) : Optional.empty();
    }

    public MetaItem getMetaItemOrThrow(int index) throws NoSuchElementException {
        MetaItem item = getMetaItem(index).orElse(null);

        if (item == null) {
            throw new NoSuchElementException("Missing item: " + index);
        }

        return item;
    }

    public MetaItem getMetaItemOrThrow(MetaItem item) throws NoSuchElementException {
        return getMetaItemOrThrow(item.getName(), item.getValue(), 0);
    }

    public MetaItem getMetaItemOrThrow(MetaItem item, int startIndex) throws NoSuchElementException {
        return getMetaItemOrThrow(item.getName(), item.getValue(), startIndex);
    }

    public MetaItem getMetaItemOrThrow(String name) throws NoSuchElementException {
        return getMetaItemOrThrow(name, 0);
    }

    public MetaItem getMetaItemOrThrow(String name, int startIndex) throws NoSuchElementException {
        MetaItem item = getMetaItem(name, startIndex).orElse(null);

        if (item == null) {
            throw new NoSuchElementException("Missing item: " + name);
        }

        return item;
    }

    public MetaItem getMetaItemOrThrow(String name, JsonValue value) throws NoSuchElementException {
        return getMetaItemOrThrow(name, value, 0);
    }

    public MetaItem getMetaItemOrThrow(String name, JsonValue value, int startIndex) throws NoSuchElementException {
        MetaItem item = getMetaItem(name, value, startIndex).orElse(null);

        if (item == null) {
            throw new NoSuchElementException("Missing item: " + name);
        }

        return item;
    }

    public <T extends JsonValue> MetaItem getMetaItemOrThrow(String name, Class<T> clazz) throws NoSuchElementException {
        return getMetaItemOrThrow(name, clazz, 0);
    }

    public <T extends JsonValue> MetaItem getMetaItemOrThrow(String name, Class<T> clazz, int startIndex) throws NoSuchElementException {
        MetaItem item = getMetaItem(name, clazz, startIndex).orElse(null);

        if (item == null) {
            throw new NoSuchElementException("Missing item: " + name);
        }

        return item;
    }

    public boolean hasMetaItem(MetaItem item) {
        return hasMeta() && getMeta().contains(item);
    }

    public boolean hasMetaItem(String name) {
        return hasMeta() && getMeta().contains(name);
    }

    public boolean hasMetaItem(String name, JsonValue value) {
        return hasMeta() && getMeta().contains(name, value);
    }

    public <T extends JsonValue> boolean hasMetaItem(String name, Class<T> clazz) {
        return hasMeta() && getMeta().contains(name, clazz);
    }

    public int indexOfMetaItem(MetaItem item) {
        return indexOfMetaItem(item, 0);
    }

    public int indexOfMetaItem(MetaItem item, int startIndex) {
        return hasMeta() ? getMeta().indexOf(item) : -1;
    }

    public int indexOfMetaItem(String name) {
        return indexOfMetaItem(name, 0);
    }

    public int indexOfMetaItem(String name, int startIndex) {
        return hasMeta() ? getMeta().indexOf(name) : -1;
    }

    public int indexOfMetaItem(String name, JsonValue value) {
        return indexOfMetaItem(name, value, 0);
    }

    public int indexOfMetaItem(String name, JsonValue value, int startIndex) {
        return hasMeta() ? getMeta().indexOf(name, value, startIndex) : -1;
    }

    public <T extends JsonValue> int indexOfMetaItem(String name, Class<T> clazz) {
        return indexOfMetaItem(name, clazz, 0);
    }

    public <T extends JsonValue> int indexOfMetaItem(String name, Class<T> clazz, int startIndex) {
        return hasMeta() ? getMeta().indexOf(name, clazz, startIndex) : -1;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && ATTRIBUTE_NAME_VALIDATOR.test(name) && getValue() != null && (getValue().getType() == JsonType.NULL || getValue().getType() == getType().getJsonType());
    }
}
