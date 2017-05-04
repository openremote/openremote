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
package org.openremote.model.attribute;

import com.google.gwt.regexp.shared.RegExp;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

/**
 * Convenience wrapper API for {@link ObjectValue}.
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

    protected Attribute(ObjectValue objectValue) {
        super(objectValue);
    }

    protected Attribute(String name) {
        super(Values.createObject());
        setName(name);
    }

    protected Attribute(String name, AttributeType type) {
        this(name);
        setName(name);
        setTypeAndClearValue(type);
    }

    protected Attribute(String name, AttributeType type, Value value) {
        this(name, type);
        setValue(value);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        requireNonNullAndNonEmpty(name);
        this.name = name;
    }

    public Optional<AttributeType> getType() {
        return getObjectValue().getString(TYPE_FIELD_NAME).flatMap(AttributeType::optionalValueOf);
    }

    public void setTypeAndClearValue(AttributeType type) {
        Objects.requireNonNull(type);
        getObjectValue().put(TYPE_FIELD_NAME, Values.create(type.name()));

        // Remove existing value and timestamp
        clearValue();
    }

    public boolean hasMetaItems() {
        return getObjectValue().getArray(META_FIELD_NAME)
            .filter(arrayValue -> arrayValue.length() > 0)
            .isPresent();
    }

    public Meta getMeta() {
        if (!getObjectValue().hasKey(META_FIELD_NAME)) {
            // Create array object so don't have to call setMeta
            // can just update the collection like normal POJO behaviour
            getObjectValue().put(META_FIELD_NAME, Values.createArray());
        }
        return new Meta(getObjectValue()
            .getArray(META_FIELD_NAME)
            .orElseThrow(() -> new IllegalStateException("Attribute " + META_FIELD_NAME + "' field is not an array"))
        );
    }

    public Stream<MetaItem> getMetaStream() {
        return getMeta().stream();
    }

    public boolean hasMetaItem(String metaName) {
        return getMetaItem(metaName).isPresent();
    }

    public boolean hasMetaItem(HasMetaName metaName) {
        return getMetaItem(metaName).isPresent();
    }

    public Optional<MetaItem> getMetaItem(String metaName) {
        return getMetaStream()
            .filter(metaItem -> metaItem.getName().filter(s -> s.equals(metaName)).isPresent())
            .findFirst();
    }

    public Optional<MetaItem> getMetaItem(HasMetaName hasMetaName) {
        return getMetaItem(hasMetaName.getUrn());
    }

    public void setMeta(List<MetaItem> metaItems) {
        if (metaItems != null) {
            Meta meta;
            if (metaItems instanceof Meta) {
                meta = (Meta) metaItems;
            } else {
                meta = new Meta();
                meta.addAll(metaItems);
            }
            getObjectValue().put(META_FIELD_NAME, meta.getArrayValue());
        } else {
            getObjectValue().remove(META_FIELD_NAME);
        }
    }

    public Attribute setMeta(Meta meta) {
        setMeta((List<MetaItem>) meta);
        return this;
    }

    public Attribute setMeta(MetaItem... meta) {
        setMeta(Arrays.asList(meta));
        return this;
    }

    @Override
    public boolean isValid() {
        return super.isValid()
            // Must have a valid name
            && getName().isPresent()
            && ATTRIBUTE_NAME_VALIDATOR.test(getName().get()) && (
            // and either have no value
            !getValue().isPresent()
                // or have a timestamp and a type that validates the value
                || (hasValueTimestamp() && isValidValue(getValue().get()))
        );
    }

    public boolean isValidValue(Value value) {
        return getType().filter(type -> type.isValid(value)).isPresent();
    }

    //    ---------------------------------------------------
    //    FUNCTIONAL METHODS BELOW
    //    ---------------------------------------------------

    public static <A extends Attribute> boolean isAttributeNameEqualTo(A attribute, String name) {
        return attribute != null
            && attribute.getName().filter(attributeName -> attributeName.equals(name)).isPresent();
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
