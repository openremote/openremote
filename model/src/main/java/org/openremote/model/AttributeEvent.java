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

import elemental.json.JsonValue;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;

/**
 * A timestamped {@link AttributeState}.
 */
public class AttributeEvent extends SharedEvent {

    /**
     * Processing of the attribute event depends on where the event originated from.
     *
     * TODO Use this as header in Camel messages for conditional routing
     */
    public enum Source {

        /**
         * The event was created by a client, it's a write request/actuator message.
         */
        SOUTHBOUND,

        /**
         * The event was created by internal processing, for example, as a rule consequence.
         */
        PROCESSING,

        /**
         * The event was created by a protocol, it's a value change on a sensor.
         */
        NORTHBOUND
    }

    static public class EntityIdFilter extends EventFilter<AttributeEvent> {

        public static final String FILTER_TYPE = "attribute-entity-id";

        protected String entityId;

        protected EntityIdFilter() {
        }

        public EntityIdFilter(String entityId) {
            this.entityId = entityId;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        @Override
        public String getFilterType() {
            return FILTER_TYPE;
        }

        @Override
        public boolean apply(AttributeEvent event) {
            return event.getEntityId().equals(entityId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "assetId='" + entityId + '\'' +
                '}';
        }
    }

    protected AttributeState attributeState;

    protected AttributeEvent() {
    }

    public AttributeEvent(String entityId, String attributeName, JsonValue value) {
        this(new AttributeState(new AttributeRef(entityId, attributeName), value));
    }

    public AttributeEvent(AttributeRef attributeRef, JsonValue value) {
        this(new AttributeState(attributeRef, value));
    }

    public AttributeEvent(AttributeState attributeState) {
        this.attributeState = attributeState;
    }

    public AttributeEvent(AttributeState attributeState, long timestamp) {
        super(timestamp);
        this.attributeState = attributeState;
    }

    public AttributeState getAttributeState() {
        return attributeState;
    }

    public AttributeRef getAttributeRef() {
        return getAttributeState().getAttributeRef();
    }

    public String getEntityId() {
        return getAttributeRef().getEntityId();
    }

    public String getAttributeName() {
        return getAttributeRef().getAttributeName();
    }

    public JsonValue getValue() {
        return getAttributeState().getValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp=" + timestamp +
            ", attributeState=" + attributeState +
            "}";
    }
}
