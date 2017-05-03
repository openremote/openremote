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
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.util.JsonUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.openremote.model.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.util.JsonUtil.asJsonArray;
import static org.openremote.model.util.JsonUtil.getJsonValueType;
import static org.openremote.model.util.JsonUtil.sanitizeJsonValue;
import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

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

    protected Attribute(JsonObject jsonObject) {
        super(jsonObject);
    }

    protected Attribute(String name) {
        this(Json.createObject());
        setName(name);
    }

    protected Attribute(String name, AttributeType type) {
        this(name);
        setName(name);
        setType(type);
    }

    protected Attribute(String name, AttributeType type, JsonValue value) {
        this(name, type);
        setValue(value);
    }

    public boolean hasName() {
        return getName().isPresent();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        requireNonNullAndNonEmpty(name);
        this.name = name;
    }

    public boolean hasType() {
        return jsonObject.hasKey(TYPE_FIELD_NAME) && JsonUtil.isOfTypeString(jsonObject.get(TYPE_FIELD_NAME));
    }

    public Optional<AttributeType> getType() {
        return Optional.ofNullable(hasType() ? AttributeType.fromValue(jsonObject.getString(TYPE_FIELD_NAME)) : null);
    }

    public void setType(AttributeType type) {
        Objects.requireNonNull(type);

        if (getType().orElse(null) == type) {
            return;
        }

        jsonObject.put(TYPE_FIELD_NAME, Json.create(type.getValue()));

        // Remove the old value and timestamp
        jsonObject.remove(VALUE_TIMESTAMP_FIELD_NAME);
        clearValue();
    }

    public boolean hasMeta() {
        return asJsonArray(jsonObject.getArray(META_FIELD_NAME))
            .map(jsonArray -> jsonArray.length() > 0)
            .orElse(false);
    }

    public Meta getMeta() {
        if (!hasMeta()) {
            // Create array object so don't have to call setMeta
            // can just update the collection like normal POJO behaviour
            JsonArray metaArray = Json.createArray();
            jsonObject.put(META_FIELD_NAME, metaArray);
        }

        return new Meta(jsonObject.getArray(META_FIELD_NAME));
    }

    public Stream<MetaItem> getMetaStream() {
        return getMeta().stream();
    }

    public boolean hasMetaItem(String metaName) {
        return getMetaItem(metaName).isPresent();
    }

    public boolean hasMetaItem(HasMetaName hasMetaName) {
        return getMetaItem(hasMetaName).isPresent();
    }

    public Optional<MetaItem> getMetaItem(String metaName) {
        return getMetaStream()
            .filter(isMetaNameEqualTo(metaName))
            .findFirst();
    }

    public Optional<MetaItem> getMetaItem(HasMetaName hasMetaName) {
        return getMetaItem(hasMetaName.getUrn());
    }

    // The below methods cause weird behaviour where things will compile
    // but will fail at runtime - some weird generics issue
//    public <A extends Attribute> A setMeta(List<MetaItem> meta) {
//        if (meta != null) {
//            Meta metaObj;
//            if (meta instanceof Meta) {
//                metaObj = (Meta)meta;
//            } else {
//                metaObj = new Meta();
//                metaObj.addAll(meta);
//            }
//            jsonObject.put(META_FIELD_NAME, metaObj.getJsonArray());
//        } else {
//            jsonObject.remove(META_FIELD_NAME);
//        }
//
//        return (A)this;
//    }
//
//    @SuppressWarnings("unchecked")
//    public <A extends Attribute> A setMeta(Meta meta) {
//        setMeta((List<MetaItem>)meta);
//        return (A)this;
//    }
//
//    @SuppressWarnings("unchecked")
//    public <A extends Attribute> A setMeta(MetaItem... meta) {
//        setMeta(Arrays.asList(meta));
//        return (A)this;
//    }

    @Override
    public boolean isValid() {
        // TODO: Should value validity be part of the isValid check?
        return super.isValid()
            && hasName() && ATTRIBUTE_NAME_VALIDATOR.test(name)
            && getType()
            .isPresent();
    }

    @Override
    public boolean isValidValue(JsonValue value) {
        JsonValue sanitizedValue = sanitizeJsonValue(value);

        return super.isValidValue(sanitizedValue)
            && (sanitizedValue == null ||
             getType()
                .map(type -> type.getJsonType() == getJsonValueType(sanitizedValue))
                .orElse(false));
    }

    //    ---------------------------------------------------
//    FUNCTIONAL METHODS BELOW
//    ---------------------------------------------------

    public static <A extends Attribute> boolean equals(A attribute1, A attribute2) {
        if (attribute1 == null && attribute2 == null)
            return true;

        if (attribute1 == null || attribute2 == null)
            return false;

        Optional<String> name = attribute2.getName();
        return isAttributeNameEqualTo(attribute1, name.orElse(null))
            && JsonUtil.equals(attribute1.getValue().orElse(null), attribute2.getValue().orElse(null));
    }

    public static <A extends Attribute> boolean isAttributeNameEqualTo(A attribute, String name) {
        if (attribute == null)
            return false;

        return attribute
            .getName()
            .map(attributeName -> attributeName.equals(name))
            .orElse(name == null);
    }

    public static <A extends Attribute> Predicate<A> isAttributeNameEqualTo(String name) {
        return attribute -> isAttributeNameEqualTo(attribute, name);
    }

    public static <A extends Attribute> boolean isAttributeTypeEqualTo(A attribute, AttributeType type) {
        if (attribute == null)
            return false;

        return attribute
            .getType()
            .map(attributeType -> attributeType == type)
            .orElse(type == null);
    }

    public static <A extends Attribute> Predicate<A> isAttributeTypeEqualTo(AttributeType type) {
        return attribute -> isAttributeTypeEqualTo(attribute, type);
    }
}
