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
 * An asset attribute value update, capturing that asset state at a point in time.
 * <p>
 * Instances of this class are processed by the system, with a sequence of processors. Each
 * processor can consume the asset state, and optionally set its status, thus controlling
 * how the asset state update is ultimately handled (e.g. the asset storage processor can
 * stop it from being handled by the asset datapoint processor).
 */
public class AssetState extends AbstractAssetUpdate {

    /**
     * Processors of asset state updates can change the status to direct further processing.
     */
    public enum ProcessingStatus {
        /**
         * Processor is happy for update to continue through the system.
         */
        CONTINUE,

        /**
         * Processor has finally handled the update, cancel further processing.
         */
        HANDLED,

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

    protected ProcessingStatus processingStatus = ProcessingStatus.CONTINUE;

    protected Throwable error;

    public AssetState(Asset asset, AssetAttribute attribute) {
        super(asset, attribute);
    }

    public AssetState(Asset asset, AssetAttribute attribute, JsonValue oldValue, long oldValueTimestamp, boolean northbound) {
        super(asset, attribute, oldValue, oldValueTimestamp, northbound);
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public Throwable getError() {
        return error;
    }

    /**
     * @return <code>true</code> is this state has completed processing, it's not currently being processed.
     */
    public boolean isCompleted() {
        return getProcessingStatus() == ProcessingStatus.COMPLETED;
    }

    /////////////////////////////////////////////////////////////////
    // GETTERS AND SETTERS BELOW CAN ONLY BE USED WHEN STATUS IS NOT COMPLETED
    /////////////////////////////////////////////////////////////////

    public AssetAttribute getAttribute() {
        if (!isCompleted()) {
            return attribute;
        }
        throw new IllegalStateException("Instance is immutable, processing status '" + getProcessingStatus() + "': " + this);
    }

    public void setValue(JsonValue value) {
        if (!isCompleted()) {
            attribute.setValue(value);
        } else {
            throw new IllegalStateException("Instance is immutable, processing status '" + getProcessingStatus() + "': " + this);
        }
    }

    public void setValueUnchecked(JsonValue value) {
        if (!isCompleted()) {
            attribute.setValueUnchecked(value);
        } else {
            throw new IllegalStateException("Instance is immutable, processing status '" + getProcessingStatus() + "': " + this);
        }
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        if (!isCompleted()) {
            this.processingStatus = processingStatus;
        } else {
            throw new IllegalStateException("Instance is immutable, processing status '" + getProcessingStatus() + "': " + this);
        }
    }

    public void setError(Throwable error) {
        if (!isCompleted()) {
            this.error = error;
        } else {
            throw new IllegalStateException("Instance is immutable, processing status '" + getProcessingStatus() + "': " + this);
        }
    }
}
