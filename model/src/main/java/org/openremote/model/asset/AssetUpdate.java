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
import org.openremote.model.AttributeType;
import org.openremote.model.util.JsonUtil;

import java.util.Date;

/**
 * An asset attribute value change that can be handled by a sequence of processors.
 */
public class AssetUpdate {

    /**
     * Processors of updates can change the status to direct further processing.
     */
    public enum Status {
        /**
         * Processor is happy for update to continue through the system.
         */
        CONTINUE,

        /**
         * Processor has finally handled the update, cancel further processing.
         */
        HANDLED,

        /**
         * Don't process event in any more rule engines but continue through rest of processing chain.
         */
        RULES_HANDLED,

        /**
         * Processor encountered an error trying to process the update, cancel further processing and escalate.
         */
        ERROR,

        /**
         * Indicates that this update has been through the entire processing chain; the object can no longer be
         * mutated at this stage.
         */
        COMPLETED
    }

    final protected AbstractAssetAttribute attribute;

    final protected Date createdOn;

    final protected String id;

    final protected String name;

    final protected String typeString;

    final protected AssetType type;

    final protected String[] path;

    final protected String parentId;

    final protected String parentName;

    final protected String parentTypeString;

    final protected AssetType parentType;

    final protected String realmId;

    final protected String tenantRealm;

    final protected double[] coordinates;

    final protected JsonValue oldValue;

    final protected long oldValueTimestamp;

    protected Status status = Status.CONTINUE;

    protected Throwable error;

    // True if the update was initiated by a protocol and is being processed northbound
    protected boolean northbound;

    public AssetUpdate(Asset asset, AbstractAssetAttribute attribute) {
        this(asset, attribute, null, 0, false);
    }

    public AssetUpdate(Asset asset, AbstractAssetAttribute attribute, JsonValue oldValue, long oldValueTimestamp, boolean northbound) {
        this.attribute = attribute;
        this.id = asset.getId();
        this.name = asset.getName();
        if (asset.getPath() == null) {
            throw new IllegalArgumentException("Asset not loaded completely, empty path: " + asset);
        }
        this.path = asset.getPath();
        this.typeString = asset.getType();
        this.type = asset.getWellKnownType();
        this.createdOn = asset.getCreatedOn();
        this.parentId = asset.getParentId();
        this.parentName = asset.getParentName();
        this.parentTypeString = asset.getParentType();
        this.parentType = asset.getParentType() != null ? AssetType.getByValue(asset.getParentType()) : null;
        this.realmId = asset.getRealmId();
        this.tenantRealm = asset.getTenantRealm();
        this.coordinates = asset.getCoordinates();
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

    public JsonValue getOldValue() {
        return oldValue;
    }

    public long getOldValueTimestamp() {
        return oldValueTimestamp;
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
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

    public boolean isCompleted() {
        return getStatus() == Status.COMPLETED;
    }

    public boolean isValueChanged() {
        return !JsonUtil.equals(attribute.getValue(), oldValue);
    }

    /////////////////////////////////////////////////////////////////
    // GETTERS AND SETTERS BELOW CAN ONLY BE USED WHEN STATUS IS NOT COMPLETED
    /////////////////////////////////////////////////////////////////

    public AbstractAssetAttribute getAttribute() {
        if (!isCompleted()) {
            return attribute;
        }
        throw new IllegalStateException("Instance is immutable, status '" + getStatus() + "': " + this);
    }

    public void setValue(JsonValue value) {
        if (!isCompleted()) {
            attribute.setValue(value);
        } else {
            throw new IllegalStateException("Instance is immutable, status '" + getStatus() + "': " + this);
        }
    }

    public void setValueUnchecked(JsonValue value) {
        if (!isCompleted()) {
            attribute.setValueUnchecked(value);
        } else {
            throw new IllegalStateException("Instance is immutable, status '" + getStatus() + "': " + this);
        }
    }

    public void setStatus(Status status) {
        if (!isCompleted()) {
            this.status = status;
        } else {
            throw new IllegalStateException("Instance is immutable, status '" + getStatus() + "': " + this);
        }
    }

    public void setError(Throwable error) {
        if (!isCompleted()) {
            this.error = error;
        } else {
            throw new IllegalStateException("Instance is immutable, status '" + getStatus() + "': " + this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssetUpdate that = (AssetUpdate) o;

        // TODO Don't use jsEquals(), write own comparison by value

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
            "assetId=" + getName() +
            ", attributeName=" + getAttributeName() +
            ", value=" + getValue().toJson() +
            ", valueTimestamp=" + getValueTimestamp() +
            ", oldValue=" + (oldValue != null ? oldValue.toJson() : "null") +
            ", oldValueTimestamp=" + getOldValueTimestamp() +
            '}';
    }
}
