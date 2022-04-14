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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.*;

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
public class AssetState<T> implements Comparable<AssetState<?>>, NameValueHolder<T>, MetaHolder {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    final protected String attributeName;

    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    @JsonDeserialize(converter = ValueDescriptor.StringValueDescriptorConverter.class)
    final protected ValueDescriptor<T> attributeValueType;

    final protected T value;

    final protected long timestamp;

    final protected AttributeEvent.Source source;

    final protected T oldValue;

    final protected long oldValueTimestamp;

    final protected String id;

    final protected String assetName;

    final protected String assetType;

    final protected Date createdOn;

    final protected String[] path;

    final protected String parentId;

    final protected String realm;

    final protected MetaMap meta;

    public AssetState(Asset<?> asset, Attribute<T> attribute, AttributeEvent.Source source) {
        this.attributeName = attribute.getName();
        this.attributeValueType = attribute.getType();
        this.value = attribute.getValue().orElse(null);
        this.timestamp = attribute.getTimestamp().orElse(-1L);
        this.source = source;
        this.oldValue = asset.getAttribute(attributeName, attribute.getType().getType()).flatMap(Attribute::getValue).orElse(null);
        this.oldValueTimestamp = asset.getAttributes().get(attributeName).flatMap(Attribute::getTimestamp).orElse(-1L);
        this.id = asset.getId();
        this.assetName = asset.getName();
        this.assetType = asset.getType();
        this.createdOn = asset.getCreatedOn();
        this.path = asset.getPath();
        this.parentId = asset.getParentId();
        this.realm = asset.getRealm();
        this.meta = attribute.getMeta();
    }

    @Override
    public String getName() {
        return attributeName;
    }

    @JsonProperty
    @JsonSerialize(converter = ValueDescriptor.ValueDescriptorStringConverter.class)
    @Override
    public ValueDescriptor<T> getType() {
        return attributeValueType;
    }

    @JsonProperty
    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public <U> Optional<U> getValueAs(Class<U> valueType) {
        return ValueUtil.getValueCoerced(value, valueType);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AttributeEvent.Source getSource() {
        return source;
    }

    public Optional<Object> getOldValue() {
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

    public String getAssetName() {
        return assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public String[] getPath() {
        return path;
    }

    public String getParentId() {
        return parentId;
    }

    public String getRealm() {
        return realm;
    }

    public MetaMap getMeta() {
        return meta;
    }

    public <U> Optional<U> getMetaValue(MetaItemDescriptor<U> metaItemDescriptor) {
        return getMeta().getValue(metaItemDescriptor);
    }

    public boolean hasMeta(MetaItemDescriptor<?> metaItemDescriptor) {
        return getMeta().has(metaItemDescriptor);
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
        return getId().equals(event.getAssetId())
            && getName().equals(event.getAttributeName())
            && getValue().equals(event.getValue())
            && (ignoreTimestamp || getTimestamp() == event.getTimestamp())
            && (source == null || getSource() == source);
    }

    @Override
    public int compareTo(AssetState<?> that) {
        int result = getId().compareTo(that.getId());
        if (result == 0)
            result = getName().compareTo(that.getName());
        if (result == 0)
            result = Long.compare(getTimestamp(), that.getTimestamp());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetState<?> that = (AssetState<?>) o;
        return Objects.equals(attributeName, that.attributeName)
            && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + getId() + '\'' +
            ", name='" + getAssetName() + '\'' +
            ", type='" + getAssetType() + '\'' +
            ", attributeName='" + getName() + '\'' +
            ", attributeValueDescriptor=" + getType() +
            ", value=" + getValue() +
            ", timestamp=" + getTimestamp() +
            ", oldValue=" + getOldValue() +
            ", oldValueTimestamp=" + getOldValueTimestamp() +
            ", source=" + getSource() +
            '}';
    }
}
