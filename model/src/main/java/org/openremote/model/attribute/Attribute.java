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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.regexp.shared.RegExp;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.HasUniqueResourceName;
import org.openremote.model.ValidationFailure;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.value.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.openremote.model.attribute.Attribute.AttributeFailureReason.*;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

/**
 * Convenience wrapper API for {@link ObjectValue}.
 */
public abstract class Attribute extends AbstractValueTimestampHolder {

    public enum AttributeFailureReason implements ValidationFailure.Reason {
        ATTRIBUTE_NAME_INVALID,
        ATTRIBUTE_TYPE_MISSING,
        ATTRIBUTE_VALUE_TIMESTAMP_MISSING
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

    public String name;

    @JsonIgnore
    protected Meta meta;

    protected Attribute(ObjectValue objectValue) {
        super(objectValue);
    }

    protected Attribute(String name) {
        super(Values.createObject());
        setName(name);
    }

    protected Attribute(String name, AttributeValueType type) {
        this(name);
        setName(name);
        setType(type);
    }

    protected Attribute(String name, AttributeValueType type, Value value, long timestamp) {
        this(name, type);
        setValue(value);
        setValueTimestamp(timestamp);
    }

    protected Attribute(String name, AttributeValueType type, Value value) {
        this(name, type);
        setValue(value);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public String getNameOrThrow() {
        return getName().orElseThrow(() -> new IllegalStateException("Attribute doesn't have a name"));
    }

    public void setName(String name) {
        requireNonNullAndNonEmpty(name);
        this.name = name;
    }

    public void clearName() {
        this.name = null;
    }

    public Optional<AttributeValueType> getType() {
        return getObjectValue().getString(TYPE_FIELD_NAME).flatMap(name -> EnumUtil.enumFromString(AttributeValueType.class, name));
    }

    /**
     * The below is used in a sufficient number of places to provide it here as a utility method
     * with a standardised exception message.
     */
    public AttributeValueType getTypeOrThrow() {
        return getType().orElseThrow(() -> new IllegalStateException("Attribute doesn't have a type"));
    }

    public void setType(AttributeValueType type) {
        Objects.requireNonNull(type);
        getObjectValue().put(TYPE_FIELD_NAME, Values.create(type.name()));
    }

    public void clearType() {
        getObjectValue().remove(TYPE_FIELD_NAME);
    }

    public boolean hasMetaItems() {
        return getObjectValue().getArray(META_FIELD_NAME)
            .filter(arrayValue -> arrayValue.length() > 0)
            .isPresent();
    }

    public Meta getMeta() {
        if (meta == null) {
            return new Meta(getObjectValue()
                .getArray(META_FIELD_NAME)
                .orElseGet(() -> {
                    ArrayValue arr = Values.createArray();
                    getObjectValue().put(META_FIELD_NAME, arr);
                    return arr;
                })
            );
        }
        return meta;
    }

    public Stream<MetaItem> getMetaStream() {
        return getMeta().stream();
    }

    public boolean hasMetaItem(String metaName) {
        return getMetaStream().anyMatch(isMetaNameEqualTo(metaName));
    }

    public boolean hasMetaItem(HasUniqueResourceName metaName) {
        return getMetaStream().anyMatch(isMetaNameEqualTo(metaName));
    }

    public Optional<MetaItem> getMetaItem(String metaName) {
        return getMetaStream()
            .filter(metaItem -> metaItem.getName().filter(s -> s.equals(metaName)).isPresent())
            .findFirst();
    }

    public MetaItem[] getMetaItems(String metaName) {
        return getMetaStream()
            .filter(metaItem -> metaItem.getName().filter(s -> s.equals(metaName)).isPresent())
            .toArray(MetaItem[]::new);
    }

    public Optional<MetaItem> getMetaItem(HasUniqueResourceName hasUniqueResourceName) {
        return getMetaItem(hasUniqueResourceName.getUrn());
    }

    public void setMeta(List<MetaItem> metaItems) {
        Meta meta;
        if (metaItems == null) {
            // Don't allow nulling meta
            meta = new Meta();
        } else if (metaItems instanceof Meta) {
            meta = (Meta) metaItems;
        } else {
            meta = new Meta();
            meta.addAll(metaItems);
        }

        this.meta = meta;
        getObjectValue().put(META_FIELD_NAME, meta.getArrayValue());
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
    public List<ValidationFailure> getValidationFailures() {
        return getValidationFailures(true);
    }

    public List<ValidationFailure> getValidationFailures(boolean includeMeta) {
        List<ValidationFailure> failures = super.getValidationFailures();

        if (!getName().isPresent() || !ATTRIBUTE_NAME_VALIDATOR.test(getName().get()))
            failures.add(new ValidationFailure(ATTRIBUTE_NAME_INVALID));

        if (!getType().isPresent())
            failures.add(new ValidationFailure(ATTRIBUTE_TYPE_MISSING));

        if (!getValueTimestamp().isPresent())
            failures.add(new ValidationFailure(ATTRIBUTE_VALUE_TIMESTAMP_MISSING));

        // Value can be empty, if it's not it must validate with the type
        getValue().flatMap(value ->
            getType().flatMap(attributeType -> attributeType.isValidValue(value))
        ).ifPresent(failures::add);

        if (includeMeta) {
            failures.addAll(getMetaItemsValidationFailures());
        }

        return failures;
    }

    public List<ValidationFailure> getMetaItemsValidationFailures() {
        List<ValidationFailure> failures = new ArrayList<>();
        if (hasMetaItems()) {
            for (MetaItem metaItem : getMeta()) {
                failures.addAll(getMetaItemValidationFailures(metaItem, Optional.empty()));
            }
        }
        return failures;
    }

    public List<ValidationFailure> getMetaItemValidationFailures(MetaItem item, Optional<MetaItemDescriptor> metaItemDescriptor) {
        return item.getValidationFailures(metaItemDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectValue, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Attribute))
            return false;
        Attribute that = (Attribute) obj;
        return Objects.equals(name, that.name) && Objects.equals(objectValue, that.objectValue);
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

    public static <A extends Attribute> boolean isAttributeTypeEqualTo(A attribute, AttributeValueType type) {
        if (attribute == null)
            return false;
        return attribute
            .getType()
            .map(attributeType -> attributeType == type)
            .orElse(type == null);
    }

    public static <A extends Attribute> Predicate<A> isAttributeTypeEqualTo(AttributeValueType type) {
        return attribute -> isAttributeTypeEqualTo(attribute, type);
    }

    public static <A extends Attribute> boolean isAttributeTypeEqualTo(A attribute, ValueType valueType) {
        return isAttributeTypeEqualTo(valueType).test(attribute);
    }

    public static <A extends Attribute> Predicate<A> isAttributeTypeEqualTo(ValueType valueType) {
        return attribute -> {
            if (attribute == null)
                return false;
            return attribute
                .getType()
                .map(attributeType -> attributeType.getValueType() == valueType)
                .orElse(valueType == null);
        };
    }
}
