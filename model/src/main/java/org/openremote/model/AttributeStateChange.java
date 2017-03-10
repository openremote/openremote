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
package org.openremote.model;

public class AttributeStateChange {

    /**
     * Consumers of state changes can change the status to direct further processing.
     */
    public enum Status {
        /**
         * Consumer is happy for attribute state to continue through the system.
         */
        CONTINUE,

        /**
         * Consumer has finally handled the change, cancel further processing.
         */
        HANDLED
    }

    final protected Attribute attribute;
    final protected AttributeState originalState;
    final protected AttributeState newState;
    protected Status processingStatus = Status.CONTINUE;

    public AttributeStateChange(Attribute attribute, AttributeState originalState) {
        this.attribute = attribute;
        this.newState = new AttributeState(originalState.getAttributeRef(), attribute.getValue());
        this.originalState = originalState;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public AttributeState getNewState() {
        return newState;
    }

    public AttributeState getOriginalState() {
        return originalState;
    }

    public Status getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(Status processingStatus) {
        this.processingStatus = processingStatus;
    }

    /**
     * Two attribute state changes are considered equal if they reference
     * the same attribute and if the original and desired new states are the equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }
        AttributeStateChange that = (AttributeStateChange) o;
        return this.getOriginalState().equals(that.getOriginalState()) &&
            this.getNewState().equals(that.getNewState());
    }

    @Override
    public int hashCode() {
        int result = getNewState().hashCode();
        result = 31 * result + getOriginalState().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "originalState=" + originalState +
            ", newState=" + newState +
            ", processingStatus=" + processingStatus +
            '}';
    }
}
