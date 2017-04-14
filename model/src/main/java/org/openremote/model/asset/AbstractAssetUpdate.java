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

import elemental.json.JsonValue;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;
import org.openremote.model.AttributeType;
import org.openremote.model.util.JsonUtil;

import java.util.Date;

/**
 * An asset attribute value update, capturing that asset state at a point in time.
 * <p>
 * This class layout is convenient for writing expressions that match getters, while
 * carrying as much asset details as needed.
 */
public abstract class AbstractAssetUpdate {

    final protected AssetAttribute attribute;

    final protected Date createdOn;

    final protected String id;

    final protected String name;

    final protected String typeString;

    final protected AssetType type;

    final protected String[] pathFromRoot;

    final protected String parentId;

    final protected String parentName;

    final protected String parentTypeString;

    final protected AssetType parentType;

    final protected String realmId;

    final protected String tenantRealm;

    final protected double[] coordinates;

    final protected JsonValue oldValue;

    final protected long oldValueTimestamp;

    // True if the update was initiated by a protocol and is being processed northbound
    protected boolean northbound;

    public AbstractAssetUpdate(Asset asset, AssetAttribute attribute) {
        this(asset, attribute, null, 0, false);
    }

    public AbstractAssetUpdate(Asset asset, AssetAttribute attribute, JsonValue oldValue, long oldValueTimestamp, boolean northbound) {
        this(
            attribute,
            asset.getCreatedOn(),
            asset.getId(),
            asset.getName(),
            asset.getType(),
            asset.getWellKnownType(),
            asset.getReversePath(),
            asset.getParentId(),
            asset.getParentName(),
            asset.getParentType(),
            asset.getParentType() != null ? AssetType.getByValue(asset.getParentType()) : null,
            asset.getRealmId(),
            asset.getTenantRealm(),
            asset.getCoordinates(),
            oldValue,
            oldValueTimestamp,
            northbound
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
            that.pathFromRoot,
            that.parentId,
            that.parentName,
            that.parentTypeString,
            that.parentType,
            that.realmId,
            that.tenantRealm,
            that.coordinates,
            that.oldValue,
            that.oldValueTimestamp,
            that.northbound
        );
    }

    protected AbstractAssetUpdate(AssetAttribute attribute,
                                  Date createdOn, String id, String name, String typeString, AssetType type,
                                  String[] pathFromRoot, String parentId, String parentName, String parentTypeString, AssetType parentType,
                                  String realmId, String tenantRealm,
                                  double[] coordinates,
                                  JsonValue oldValue,
                                  long oldValueTimestamp,
                                  boolean northbound) {
        this.attribute = attribute;
        this.createdOn = createdOn;
        this.id = id;
        this.name = name;
        this.typeString = typeString;
        this.type = type;
        if (pathFromRoot == null) {
            throw new IllegalArgumentException("Empty path (asset not completely loaded?): " + id);
        }
        this.pathFromRoot = pathFromRoot;
        this.parentId = parentId;
        this.parentName = parentName;
        this.parentTypeString = parentTypeString;
        this.parentType = parentType;
        this.realmId = realmId;
        this.tenantRealm = tenantRealm;
        this.coordinates = coordinates;
        this.oldValue = oldValue;
        this.oldValueTimestamp = oldValueTimestamp;
        this.northbound = northbound;
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

    public String[] getPathFromRoot() {
        return pathFromRoot;
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

    public JsonValue getOldValue() {
        return oldValue;
    }

    public long getOldValueTimestamp() {
        return oldValueTimestamp;
    }

    public boolean isNorthbound() {
        return northbound;
    }

    public JsonValue getValue() {
        return attribute.getValue();
    }

    public String getAttributeName() {
        return attribute.getName();
    }

    public long getValueTimestamp() {
        return attribute.getValueTimestamp();
    }

    public AttributeType getAttributeType() {
        return attribute.getType();
    }

    public AttributeEvent getStateEvent() {
        return new AttributeEvent(new AttributeState(getAttributeRef(), attribute.getValue()), attribute.getValueTimestamp());
    }

    public boolean isValueChanged() {
        return !JsonUtil.equals(attribute.getValue(), oldValue);
    }

    public AttributeRef getAttributeRef() {
        return attribute.getReference();
    }

    /**
     * This is here because {@link #attribute} is not always publicly accessible
     */
    public boolean attributeRefsEqual(AbstractAssetUpdate assetUpdate) {
        return assetUpdate != null && assetUpdate.getAttributeRef().equals(getAttributeRef());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractAssetUpdate that = (AbstractAssetUpdate) o;

        return id.equals(that.id) &&
            getAttributeName().equalsIgnoreCase(that.getAttributeName()) &&
            JsonUtil.equals(getValue(), that.getValue()) &&
            getValueTimestamp() == that.getValueTimestamp() &&
            ((oldValue == null && that.oldValue == null) || (oldValue != null && JsonUtil.equals(oldValue, that.oldValue))) &&
            oldValueTimestamp == that.oldValueTimestamp;
    }

    @Override
    public int hashCode() {
        return id.hashCode() + getAttributeName().hashCode() + JsonUtil.hashCode(getValue())
            + Long.hashCode(getValueTimestamp())
            + (oldValue != null ? JsonUtil.hashCode(oldValue) : 0) + Long.hashCode(oldValueTimestamp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + getId() + '\'' +
            ", name='" + getName() + '\'' +
            ", typeString='" + getType() + '\'' +
            ", attributeName='" + getAttributeName() + '\'' +
            ", attributeType=" + getAttributeType() +
            ", value='" + getValue().toJson() + '\'' +
            ", valueTimestamp=" + getValueTimestamp() +
            ", oldValue='" + (getOldValue() != null ? getOldValue().toJson() : "null") + '\'' +
            ", oldValueTimestamp=" + getOldValueTimestamp() +
            '}';
    }
}
