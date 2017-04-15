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
import org.openremote.model.asset.AssetMeta;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Convenience overlay API for {@link JsonObject}.
 */
public abstract class Attribute extends AbstractValueTimestampHolder {

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
        this(attribute.getName(), attribute.getType().orElse(null), attribute.getJsonObject());
    }

    @Override
    protected boolean isValidValue(JsonValue value) {
        return getType().isPresent()
            ? getType().get().isValid(value)
            : super.isValidValue(value);
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

    public Optional<AttributeType> getType() {
        String typeName = jsonObject.hasKey(TYPE_FIELD_NAME) ? jsonObject.get(TYPE_FIELD_NAME).asString() : null;
        return Optional.ofNullable(typeName != null ? AttributeType.fromValue(typeName) : null);
    }

    @SuppressWarnings("unchecked")
    public void setType(AttributeType type) {
        if (type != null) {
            jsonObject.put(TYPE_FIELD_NAME, Json.create(type.getValue()));
        }
    }

    public boolean hasMeta() {
        return jsonObject.hasKey(META_FIELD_NAME);
    }

    public Meta getMeta() {
        return hasMeta() ? new Meta(jsonObject.getArray(META_FIELD_NAME)) : new Meta();
    }

    public Stream<MetaItem> getMetaItemStream() {
        return getMeta().stream();
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

    public Optional<MetaItem> firstMetaItem(String name) {
        return getMeta().first(name);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && ATTRIBUTE_NAME_VALIDATOR.test(name) && getType().isPresent();
    }

    public static Predicate<Attribute> isAttributeValid() {
        return Attribute::isValid;
    }

    public static Predicate<Attribute> isAttributeType(AttributeType attributeType) {
        return attribute -> attribute.getType().map(at -> at.equals(attributeType)).orElse(false);
    }

    public  static Predicate<Attribute> hasMetaItem(String name) {
        return attribute -> attribute.firstMetaItem(name).isPresent();
    }

    public static Predicate<MetaItem> isMetaItem(String name) {
        return metaItem -> metaItem.getName().equals(name);
    }

    public static Function<Stream<? extends Attribute>, Optional<? extends Attribute>> findAttribute(String name) {
        return attributeStream -> attributeStream
            .filter(attribute -> attribute.getName().equals(name))
            .findFirst();
    }

    public static Predicate<Stream<? extends Attribute>> containsAttributeNamed(String name) {
        return attributeStream -> findAttribute(name).apply(attributeStream).isPresent();
    }

    public static Function<Attribute, Optional<AttributeRef>> getAttributeLink(String metaName) {
        return attribute -> attribute.firstMetaItem(metaName)
            .filter(item -> item.getValue().getType() == JsonType.ARRAY)
            .map(AbstractValueHolder::getValueAsArray)
            .map(AttributeRef::new);
    }

    public static void removeAttribute(List<? extends Attribute> attributes, String name) {
        attributes.removeIf(attribute -> attribute.getName().equals(name));
    }

    public static <A extends Attribute> Optional<A> findAttribute(List<A> attributes, String name) {
        for (A attribute : attributes) {
            if (attribute.getName().equals(name))
                return Optional.of(attribute);
        }
        return Optional.empty();
    }

    public static UnaryOperator<Attribute> setAttributeLink(String metaName, AttributeRef attributeRef) {
        return attribute -> {
            if (attributeRef == null) {
                throw new IllegalArgumentException(
                    "Attribute reference cannot be null for '" + metaName + "' on: " + attribute
                );
            }
            return attribute.setMeta(
                attribute.getMeta().replace(metaName, new MetaItem(metaName, attributeRef.asJsonValue()))
            );
        };
    }

    public static UnaryOperator<Attribute> removeAttributeLink(String metaName) {
        return attribute -> attribute.setMeta(attribute.getMeta().removeAll(metaName));
    }
}
