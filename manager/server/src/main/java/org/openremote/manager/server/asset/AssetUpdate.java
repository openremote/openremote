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
package org.openremote.manager.server.asset;

import org.openremote.model.Attribute;
import org.openremote.model.AttributeEvent;

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
         * Don't process event in any more rules but continue through rest of processing chain
         */
        RULES_HANDLED,

        /**
         * Processor encountered an error trying to process the update, cancel further processing and escalate.
         */
        ERROR
    }

    final protected ServerAsset asset;
    final protected Attribute attribute;
    final protected AttributeEvent newState;
    final protected AttributeEvent oldState;

    protected Status status = Status.CONTINUE;
    protected Throwable error;

    /**
     * @throws IllegalArgumentException If the asset, attribute, newState, and oldState don't reference the same asset ID and attribute name.
     */
    public AssetUpdate(ServerAsset asset, Attribute attribute, AttributeEvent newState, AttributeEvent oldState) throws IllegalArgumentException {
        this.asset = asset;
        this.attribute = attribute;
        this.newState = newState;
        this.oldState = oldState;

        // Resolve attribute references after some sanity checks
        if (!newState.getAttributeRef().getEntityId().equals(asset.getId())) {
            throw new IllegalArgumentException("New state entity ID must match: " + asset);
        }
        if (!newState.getAttributeRef().getAttributeName().equals(attribute.getName())) {
            throw new IllegalArgumentException("New state attribute name must match: " + attribute);
        }
        if (!oldState.getAttributeRef().getEntityId().equals(asset.getId())) {
            throw new IllegalArgumentException("Old state entity ID must match: " + asset);
        }
        if (!oldState.getAttributeRef().getAttributeName().equals(attribute.getName())) {
            throw new IllegalArgumentException("Old state attribute name must match: " + attribute);
        }
        newState.getAttributeRef().setEntityName(asset.getName());
        oldState.getAttributeRef().setEntityName(asset.getName());
    }

    public ServerAsset getAsset() {
        return asset;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public AttributeEvent getNewState() {
        return newState;
    }

    public Class<?> getSender() {
        return newState.getSender();
    }

    public AttributeEvent getOldState() {
        return oldState;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isValueChanged() {
        return !newState.getAttributeState().getValue().jsEquals(oldState.getAttributeState().getValue());
    }

    public String getEntityName() {
        return newState.getEntityName();
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssetUpdate that = (AssetUpdate) o;

        return oldState.equals(that.oldState) && newState.equals(that.newState);
    }

    @Override
    public int hashCode() {
        return oldState.hashCode() + newState.hashCode();
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "newState=" + newState +
            ", status=" + status +
            '}';
    }
}
