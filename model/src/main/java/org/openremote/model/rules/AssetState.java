/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.rules;

import javaemul.internal.annotations.GwtIncompatible;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeValueDescriptor;
import org.openremote.model.attribute.Meta;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * An asset attribute value update, capturing asset state at a point in time.
 * <p>
 * This class layout is convenient for writing rules. Two asset states
 * are equal if they have the same asset ID and attribute name (the same attribute
 * reference).
 */
@GwtIncompatible
public class AssetState implements Comparable<AssetState> {

    final protected String attributeName;

    final protected AttributeValueDescriptor attributeValueType;

    final protected Value value;

    final protected long timestamp;

    final protected AttributeEvent.Source source;

    final protected Value oldValue;

    final protected long oldValueTimestamp;

    final protected String id;

    final protected String name;

    final protected String typeString;

    final protected AssetType type;

    final protected Date createdOn;

    final protected String[] path;

    final protected String parentId;

    final protected String parentName;

    final protected String parentTypeString;

    final protected AssetType parentType;

    final protected String realm;

    final protected Meta meta;

    public AssetState(AssetState that) {
        this.attributeName = that.attributeName;
        this.attributeValueType = that.attributeValueType;
        this.value = that.value;
        this.timestamp = that.timestamp;
        this.source = that.source;
        this.oldValue = that.oldValue;
        this.oldValueTimestamp = that.oldValueTimestamp;
        this.id = that.id;
        this.name = that.name;
        this.typeString = that.typeString;
        this.type = that.type;
        this.createdOn = that.createdOn;
        this.path = that.path;
        this.parentId = that.parentId;
        this.parentName = that.parentName;
        this.parentTypeString = that.parentTypeString;
        this.parentType = that.parentType;
        this.realm = that.realm;
        this.meta = that.meta;
    }

    public AssetState(Asset asset, AssetAttribute attribute, AttributeEvent.Source source) {
        this.attributeName = attribute.getNameOrThrow();
        this.attributeValueType = attribute.getTypeOrThrow();
        this.value = attribute.getValue().orElse(null);
        this.timestamp = attribute.getValueTimestamp().orElse(-1L);
        this.source = source;
        this.oldValue = asset.getAttribute(attributeName).flatMap(AbstractValueHolder::getValue).orElse(null);
        this.oldValueTimestamp = asset.getAttribute(attributeName).flatMap(AbstractValueTimestampHolder::getValueTimestamp).orElse(-1L);
        this.id = asset.getId();
        this.name = asset.getName();
        this.typeString = asset.getType();
        this.type = asset.getWellKnownType();
        this.createdOn = asset.getCreatedOn();
        this.path = asset.getPath();
        this.parentId = asset.getParentId();
        this.parentName = asset.getParentName();
        this.parentTypeString = asset.getParentType();
        this.parentType = asset.getParentWellKnownType();
        this.realm = asset.getRealm();
        this.meta = attribute.getMeta();
    }

    public String getAttributeName() {
        return attributeName;
    }

    public AttributeValueDescriptor getAttributeValueType() {
        return attributeValueType;
    }

    public Optional<Value> getValue() {
        return Optional.ofNullable(value);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AttributeEvent.Source getSource() {
        return source;
    }

    public Optional<Value> getOldValue() {
        return Optional.ofNullable(oldValue);
    }

    public long getOldValueTimestamp() {
        return oldValueTimestamp;
    }

    public String getId() {
        return id;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public String getName() {
        return name;
    }

    public String getTypeString() {
        return typeString;
    }

    public AssetType getType() {
        return type;
    }

    public String[] getPath() {
        return path;
    }

    public String getParentId() {
        return parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public String getParentTypeString() {
        return parentTypeString;
    }

    public AssetType getParentType() {
        return parentType;
    }

    public String getRealm() {
        return realm;
    }

    public Meta getMeta() {
        return meta;
    }

    public boolean isValueChanged() {
        return !getValue().equals(Optional.ofNullable(oldValue));
    }

    public Optional<Boolean> getValueAsBoolean() {
        return getValue().flatMap(Values::getBoolean);
    }

    public Optional<Double> getValueAsNumber() {
        return getValue().flatMap(Values::getNumber);
    }

    public Optional<String> getValueAsString() {
        return getValue().flatMap(Values::getString);
    }

    public Optional<ObjectValue> getValueAsObject() {
        return getValue().flatMap(Values::getObject);
    }

    public Optional<ArrayValue> getValueAsArray() {
        return getValue().flatMap(Values::getArray);
    }

    /**
     * <code>true</code> if this value is not empty and that value is null or this value is greater than that value.
     */
    public boolean isValueGreaterThan(Number that) {
        Optional<Double> number = getValueAsNumber();
        return (number.isPresent() && that == null) || (number.isPresent() && number.get() > that.doubleValue());
    }

    /**
     * <code>true</code> if this value is not empty and the old value is null or this value is greater than old value.
     */
    public boolean isValueGreaterThanOldValue() {
        return isValueGreaterThan(Values.getNumber(getOldValue().orElse(null)).orElse(null));
    }

    /**
     * <code>true</code> if this value is empty and that value is not null or this value is less than that value.
     */
    public boolean isValueLessThan(Number that) {
        Optional<Double> number = getValueAsNumber();
        return (!number.isPresent() && that != null) || (number.isPresent() && number.get() < that.doubleValue());
    }

    /**
     * <code>true</code> if this value is empty and the old value is not null or this value is less than old value.
     */
    public boolean isValueLessThanOldValue() {
        return isValueLessThan(Values.getNumber(getOldValue().orElse(null)).orElse(null));
    }

    /**
     * Value is empty or {@link org.openremote.model.value.BooleanValue} <code>false</code>.
     */
    public boolean isValueFalse() {
        return !getValueAsBoolean().isPresent() || !getValueAsBoolean().get();
    }

    /**
     * Value is not empty and {@link org.openremote.model.value.BooleanValue} <code>true</code>.
     */
    public boolean isValueTrue() {
        return getValueAsBoolean().isPresent() && getValueAsBoolean().get();
    }

    /**
     * Compares entity identifier, attribute name, value, and timestamp.
     */
    public boolean matches(AttributeEvent event) {
        return matches(event, null, false);
    }

    /**
     * Compares entity identifier, attribute name, value, source, and optional timestamp.
     */
    public boolean matches(AttributeEvent event, AttributeEvent.Source source, boolean ignoreTimestamp) {
        return getId().equals(event.getEntityId())
            && getAttributeName().equals(event.getAttributeName())
            && getValue().equals(event.getValue())
            && (ignoreTimestamp || getTimestamp() == event.getTimestamp())
            && (source == null || getSource() == source);
    }

    @Override
    public int compareTo(AssetState that) {
        int result = getId().compareTo(that.getId());
        if (result == 0)
            result = getAttributeName().compareTo(that.getAttributeName());
        if (result == 0)
            result = Long.compare(getTimestamp(), that.getTimestamp());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetState that = (AssetState) o;
        return Objects.equals(attributeName, that.attributeName) &&
            Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + getId() + '\'' +
            ", name='" + getName() + '\'' +
            ", parentName='" + getParentName() + '\'' +
            ", type='" + getTypeString() + '\'' +
            ", attributeName='" + getAttributeName() + '\'' +
            ", attributeValueDescriptor=" + getAttributeValueType() +
            ", value=" + (getValue().isPresent() ? getValue().get().toJson() : "null") + // TODO Performance?
            ", timestamp=" + getTimestamp() +
            ", oldValue=" + (getOldValue().isPresent() ? getOldValue().get().toJson() : "null") +
            ", oldValueTimestamp=" + getOldValueTimestamp() +
            ", source=" + getSource() +
            '}';
    }
}
