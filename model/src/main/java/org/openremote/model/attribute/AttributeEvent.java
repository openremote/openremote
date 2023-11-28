/*
 * Copyright 2023, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nonnull;
import org.openremote.model.event.shared.AssetInfo;
import org.openremote.model.event.shared.AttributeInfo;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an {@link Attribute} value at a point in time.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AttributeEvent extends SharedEvent implements Comparable<AttributeEvent>, AttributeInfo {

    protected AttributeRef ref;
    protected Object value;
    protected long timestamp;
    protected boolean deleted;
    @JsonIgnore
    protected Object source;
    @JsonIgnore
    protected String realm;
    @JsonIgnore
    protected String parentId;
    @JsonIgnore
    protected ValueDescriptor<?> valueType;
    @JsonIgnore
    protected Object oldValue;
    @JsonIgnore
    protected long oldValueTimestamp;
    @JsonIgnore
    protected String[] path;
    @JsonIgnore
    protected String assetName;
    @JsonIgnore
    protected String assetType;
    @JsonIgnore
    protected Date createdOn;
    @JsonIgnore
    protected MetaMap meta;

    public <T> AttributeEvent(String assetId, AttributeDescriptor<T> attributeDescriptor, T value) {
        this(assetId, attributeDescriptor.getName(), value);
    }

    public AttributeEvent(String assetId, String attributeName, Object value) {
        this(new AttributeRef(assetId, attributeName), value);
    }

    public AttributeEvent(String assetId, String attributeName, Object value, long timestamp) {
        this(new AttributeRef(assetId, attributeName), value, timestamp);
    }

    public AttributeEvent(AttributeState attributeState) {
        this(attributeState.getRef(), attributeState.getValue());
    }

    public AttributeEvent(AttributeState attributeState, long timestamp) {
        this(attributeState.getRef(), attributeState.getValue(), timestamp);
    }

    public AttributeEvent(AttributeRef attributeRef, Object value) {
        this(attributeRef, value, 0L);
    }

    @JsonCreator
    public AttributeEvent(AttributeRef ref, Object value, long timestamp) {
        super(timestamp);
        Objects.requireNonNull(ref);
        this.ref = ref;
        this.value = value;
    }

    @Override
    public AttributeState getAttributeState() {
        return new AttributeState(ref, value);
    }

    public Object getOldValue() {
        return oldValue;
    }

    public long getOldValueTimestamp() {
        return oldValueTimestamp;
    }

    @Override
    public AttributeRef getAttributeRef() {
        return ref;
    }

    @Override
    public String getId() {
        return getAttributeRef().getId();
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public String[] getPath() {
        return path;
    }

    @Override
    public String[] getAttributeNames() {
        return new String[]{getAttributeRef().getName()};
    }

    @Override
    public String getAssetName() {
        return assetName;
    }

    @Override
    public String getAssetType() {
        return assetType;
    }

    @Override
    public Date getCreatedOn() {
        return createdOn;
    }

    @Override
    public ValueDescriptor getType() {
        return valueType;
    }

    @Override
    public Class<?> getTypeClass() {
        return getType() != null ? getType().getType() : Object.class;
    }

    @Override
    public Optional<Object> getValue() {
        return getAttributeState().getValue();
    }

    @Override
    public Optional getValue(@Nonnull Class valueType) {
        return ValueUtil.getValueCoerced(value, valueType);
    }

    @Override
    public MetaMap getMeta() {
        return meta;
    }

    @Override
    public String getName() {
        return ref.getName();
    }

    public <U> Optional<U> getMetaValue(MetaItemDescriptor<U> metaItemDescriptor) {
        return Optional.ofNullable(getMeta()).flatMap(metaMap -> metaMap.getValue(metaItemDescriptor));
    }

    public boolean hasMeta(MetaItemDescriptor<?> metaItemDescriptor) {
        return Optional.ofNullable(getMeta()).map(metaMap -> metaMap.has(metaItemDescriptor)).orElse(false);
    }

    /**
     * Compares entity identifier, attribute name, value, and timestamp.
     */
    public boolean matches(AttributeEvent event) {
        return matches(event, false);
    }

    /**
     * Compares entity identifier, attribute name, value, source, and optional timestamp.
     */
    public boolean matches(AttributeEvent event, boolean ignoreTimestamp) {
        return getId().equals(event.getId())
            && getName().equals(event.getName())
            && getValue().equals(event.getValue())
            && (ignoreTimestamp || getTimestamp() == event.getTimestamp());
    }

    @Override
    public int compareTo(AttributeEvent that) {
        int result = getId().compareTo(that.getId());
        if (result == 0)
            result = getName().compareTo(that.getName());
        if (result == 0)
            result = Long.compare(getTimestamp(), that.getTimestamp());
        return result;
    }

    /**
     * Simple equality comparing {@link #getId()} and {@link #getName()}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeEvent that = (AttributeEvent) o;
        return Objects.equals(getName(), that.getName())
            && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getId());
    }

    @Override
    public String toString() {
        String valueStr = Objects.toString(value);
        return getClass().getSimpleName() + "{" +
            "timestamp=" + Instant.ofEpochMilli(timestamp) +
            ", ref=" + ref +
            ", value=" + (valueStr.length() > 100 ? valueStr.substring(0, 100) + "..." : valueStr) +
            "}";
    }

    public static AttributeEvent deleted(AttributeRef ref) {
        AttributeEvent event = new AttributeEvent(ref, null);
        event.deleted = true;
        return event;
    }

    public AttributeEvent withRealm(String realm) {
        AttributeEvent event = new AttributeEvent(getId(), getName(), getValue(), getTimestamp());
        event.realm = realm;
        return event;
    }

    public static AttributeEvent enriched(AssetInfo asset, Attribute<?> attribute, Object source, Object value, long valueTimestamp, Object oldValue, long oldValueTimestamp) {
        AttributeEvent enriched = new AttributeEvent(
            new AttributeRef(asset.getId(), attribute.getName()),
            value,
            valueTimestamp
        );

        enriched.oldValue = oldValue;
        enriched.oldValueTimestamp = oldValueTimestamp;

        enriched.source = source;
        enriched.valueType = attribute.getType();
        enriched.meta = attribute.getMeta();

        enriched.path = asset.getPath();
        enriched.createdOn = asset.getCreatedOn();
        enriched.assetName = asset.getAssetName();
        enriched.assetType = asset.getAssetType();
        enriched.parentId = asset.getParentId();
        enriched.realm = asset.getRealm();
        return enriched;
    }
}
