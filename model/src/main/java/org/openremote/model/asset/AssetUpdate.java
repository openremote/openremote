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

/**
 * An asset attribute value change that can be handled by a sequence of processors
 * with status and error handling.
 */
public class AssetUpdate extends AbstractAssetUpdate {

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

    protected Status status = Status.CONTINUE;

    protected Throwable error;

    public AssetUpdate(Asset asset, AbstractAssetAttribute attribute) {
        super(asset, attribute);
    }

    public AssetUpdate(Asset asset, AbstractAssetAttribute attribute, JsonValue oldValue, long oldValueTimestamp, boolean northbound) {
        super(asset, attribute, oldValue, oldValueTimestamp, northbound);
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isCompleted() {
        return getStatus() == Status.COMPLETED;
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
}
