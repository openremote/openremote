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
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;

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

    final protected Attribute attribute;

    final protected Date assetCreatedOn;

    final protected String assetId;

    final protected String assetName;

    final protected String assetType;

    final protected String[] assetPath;

    final protected String assetParentId;

    final protected String assetParentName;

    final protected String assetParentType;

    final protected String assetRealmId;

    final protected String assetTenantRealm;

    final protected double[] coordinates;

    final protected JsonValue oldValue;

    final protected long oldValueTimestamp;

    protected Status status = Status.CONTINUE;

    protected Throwable error;

    final protected Class<?> sender;

    public AssetUpdate(Asset asset, Attribute attribute) {
        this(asset, attribute, null, 0, null);
    }

    public AssetUpdate(Asset asset, Attribute attribute, JsonValue oldValue, long oldValueTimestamp, Class<?> sender) {
        this.attribute = attribute;
        this.assetId = asset.getId();
        this.assetName = asset.getName();
        this.assetPath = asset.getPath();
        this.assetType = asset.getType();
        this.assetCreatedOn = asset.getCreatedOn();
        this.assetParentId = asset.getParentId();
        this.assetParentName = asset.getParentName();
        this.assetParentType = asset.getParentType();
        this.assetRealmId = asset.getRealmId();
        this.assetTenantRealm = asset.getTenantRealm();
        this.coordinates = asset.getCoordinates();
        this.oldValue = oldValue;
        this.oldValueTimestamp = oldValueTimestamp;
        this.sender = sender;
    }

    public Date getAssetCreatedOn() {
        return assetCreatedOn;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public AssetType getAssetType() {
        return AssetType.getByValue(assetType);
    }

    public String getAssetTypeString() {
        return assetType;
    }

    public String[] getAssetPath() {
        return assetPath;
    }

    public String getAssetParentId() {
        return assetParentId;
    }

    public String getAssetParentName() {
        return assetParentName;
    }

    public AssetType getAssetParentType() {
        return AssetType.getByValue(assetParentType);
    }

    public String getAssetParentTypeString() {
        return assetParentType;
    }

    public String getAssetRealmId() {
        return assetRealmId;
    }

    public String getAssetTenantRealm() {
        return assetTenantRealm;
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

    public Class<?> getSender() {
        return sender;
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
        return !attribute.getValue().jsEquals(oldValue);
    }

    /////////////////////////////////////////////////////////////////
    // GETTERS AND SETTERS BELOW CAN ONLY BE USED WHEN STATUS IS NOT COMPLETED
    /////////////////////////////////////////////////////////////////

    public Attribute getAttribute() {
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

        return assetId.equals(that.assetId) &&
                getAttributeName().equalsIgnoreCase(that.getAttributeName()) &&
                getValue().jsEquals(that.getValue()) &&
                getValueTimestamp() == that.getValueTimestamp() &&
                ((oldValue == null && that.oldValue == null) || (oldValue != null && oldValue.jsEquals(that.oldValue))) &&
                oldValueTimestamp == that.oldValueTimestamp;
    }

    @Override
    public int hashCode() {
        return assetId.hashCode() + getAttributeName().hashCode() + getValue().hashCode()
            + Long.hashCode(getValueTimestamp())
            + (oldValue != null ? oldValue.hashCode() : 0) + Long.hashCode(oldValueTimestamp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            " assetId=" + getAssetName() +
            " attributeName=" + getAttributeName() +
            " value=" + getValue().toJson() +
            " valueTimestamp=" + getValueTimestamp() +
            " oldValue=" + getOldValue().toJson() +
            " oldValueTimestamp=" + getOldValueTimestamp() +
            '}';
    }
}
