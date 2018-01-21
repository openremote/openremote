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
package org.openremote.model.asset;


import javaemul.internal.annotations.GwtIncompatible;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import static org.openremote.model.asset.AssetType.CUSTOM;

/**
 * An asset attribute value update, capturing that asset state at a point in time.
 * <p>
 * This class layout is convenient for writing expressions that match getters, while
 * carrying as much asset details as needed.
 */
@GwtIncompatible
public abstract class AbstractAssetUpdate implements Comparable<AbstractAssetUpdate> {

    final public AssetAttribute attribute;

    final public Date createdOn;

    final public String id;

    final protected String name;

    final protected String typeString;

    final protected AssetType type;

    /**
     * The identifiers of all parents representing the path in the tree. The first element
     * is the root asset without a parent, the last is the identifier of this instance.
     */
    final public String[] path;

    final public String parentId;

    final public String parentName;

    final public String parentTypeString;

    final public AssetType parentType;

    final public String realmId;

    final public String tenantRealm;

    final public double[] coordinates;

    final public Value oldValue;

    final protected long oldValueTimestamp;

    final public AttributeEvent.Source source;

    final protected LocalDateTime time;

    public AbstractAssetUpdate(Asset asset, AssetAttribute attribute, AttributeEvent.Source source) {
        this(asset, attribute, null, 0, source);
    }

    public AbstractAssetUpdate(Asset asset, AssetAttribute attribute, Value oldValue, long oldValueTimestamp, AttributeEvent.Source source) {
        this(
            attribute,
            asset.getCreatedOn(),
            asset.getId(),
            asset.getName(),
            asset.getType(),
            asset.getWellKnownType(),
            asset.getPath(),
            asset.getParentId(),
            asset.getParentName(),
            asset.getParentType(),
            AssetType.getByValue(asset.getParentType()).orElse(CUSTOM),
            asset.getRealmId(),
            asset.getTenantRealm(),
            asset.getCoordinates(),
            oldValue,
            oldValueTimestamp,
            source
        );
    }

    public AbstractAssetUpdate(AbstractAssetUpdate that) {
        this(
            that.attribute,
            that.createdOn,
            that.id,
            that.name,
            that.typeString,
            that.type,
            that.path,
            that.parentId,
            that.parentName,
            that.parentTypeString,
            that.parentType,
            that.realmId,
            that.tenantRealm,
            that.coordinates,
            that.oldValue,
            that.oldValueTimestamp,
            that.source
        );
    }

    protected AbstractAssetUpdate(AssetAttribute attribute,
                                  Date createdOn, String id, String name, String typeString, AssetType type,
                                  String[] path, String parentId, String parentName, String parentTypeString, AssetType parentType,
                                  String realmId, String tenantRealm,
                                  double[] coordinates,
                                  Value oldValue,
                                  long oldValueTimestamp,
                                  AttributeEvent.Source source) {
        this.attribute = attribute;
        this.createdOn = createdOn;
        this.id = id;
        this.name = name;
        this.typeString = typeString;
        this.type = type;
        if (path == null) {
            throw new IllegalArgumentException("Empty path (asset not completely loaded?): " + id);
        }
        this.path = path;
        this.parentId = parentId;
        this.parentName = parentName;
        this.parentTypeString = parentTypeString;
        this.parentType = parentType;
        this.realmId = realmId;
        this.tenantRealm = tenantRealm;
        this.coordinates = coordinates;
        this.oldValue = oldValue;
        this.oldValueTimestamp = oldValueTimestamp;
        this.source = source;
        this.time = attribute.getValueTimestamp().map(ts -> LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())).orElse(null);
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public String getId() {
        return id;
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

    public String getRealmId() {
        return realmId;
    }

    public String getTenantRealm() {
        return tenantRealm;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public Value getOldValue() {
        return oldValue;
    }

    public long getOldValueTimestamp() {
        return oldValueTimestamp;
    }

    public AttributeEvent.Source getSource() {
        return source;
    }

    public Optional<Value> getValue() {
        return attribute.getValue();
    }

    public Optional<Boolean> getValueAsBoolean() {
        return attribute.getValueAsBoolean();
    }

    public Optional<Double> getValueAsNumber() {
        return attribute.getValueAsNumber();
    }

    public Optional<String> getValueAsString() {
        return attribute.getValueAsString();
    }

    public Optional<ObjectValue> getValueAsObject() {
        return attribute.getValueAsObject();
    }

    public Optional<ArrayValue> getValueAsArray() {
        return attribute.getValueAsArray();
    }

    public AssetAttribute getAttribute() {
        return attribute;
    }

    public String getAttributeName() {
        return attribute.getName().orElse(null);
    }

    public long getTimestamp() {
        return attribute.getValueTimestamp().orElse(-1L);
    }

    public LocalDateTime getTime() {
        return time;
    }

    public AttributeType getAttributeType() {
        return attribute.getType().orElse(null);
    }

    public boolean isValueChanged() {
        return !attribute.getValue().equals(Optional.ofNullable(oldValue));
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
        return isValueGreaterThan(Values.getNumber(getOldValue()).orElse(null));
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
        return isValueLessThan(Values.getNumber(getOldValue()).orElse(null));
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

    public AttributeRef getAttributeRef() {
        return attribute.getReferenceOrThrow();
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
    public boolean matches(AttributeEvent that, AttributeEvent.Source source, boolean ignoreTimestamp) {
        return getAttributeRef().equals(that.getAttributeRef())
            && getValue().equals(that.getValue())
            && (ignoreTimestamp || getTimestamp() == that.getTimestamp())
            && (source == null || getSource() == source);
    }

    @Override
    public int compareTo(AbstractAssetUpdate that) {
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

        AbstractAssetUpdate that = (AbstractAssetUpdate) o;

        return getId().equals(that.getId()) &&
            getAttribute().getName().equals(that.getAttribute().getName());
    }

    @Override
    public int hashCode() {
        return getId().hashCode()
            + getAttributeName().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + getId() + '\'' +
            ", name='" + getName() + '\'' +
            ", parentName='" + getParentName() + '\'' +
            ", typeString='" + getType() + '\'' +
            ", attributeName='" + getAttributeName() + '\'' +
            ", attributeType=" + getAttributeType() +
            ", value='" + (getValue().isPresent() ? getValue().get().toJson() : "null") + '\'' + // TODO Performance?
            ", timestamp=" + getTimestamp() +
            ", time=" + getTime() +
            ", oldValue='" + (getOldValue() != null ? getOldValue().toJson() : "null") + '\'' +
            ", oldValueTimestamp=" + getOldValueTimestamp() +
            ", source=" + getSource() +
            '}';
    }
}
