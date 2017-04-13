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

import org.openremote.model.event.shared.SharedEvent;

import java.util.Arrays;

/**
 * A client sends this event to the server to refresh its attribute state, expecting
 * the server to answer "soon" with {@link AttributeEvent}s. If the server
 * decides that the client doesn't have the right permissions, or if anything
 * else is not in order (e.g. the entity doesn't exist), the server might not react
 * at all.
 * <p>
 * If no attribute names and only an entity identifier are provided, all attributes
 * of the entity, accessible by the client, will be read/returned.
 */
public class ReadAttributesEvent extends SharedEvent {

    protected String entityId;

    protected String[] attributeNames;

    protected ReadAttributesEvent() {
    }

    public ReadAttributesEvent(String entityId, String... attributeNames) {
        this.entityId = entityId;
        this.attributeNames = attributeNames;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String[] getAttributeNames() {
        return attributeNames;
    }

    public void setAttributeNames(String[] attributeNames) {
        this.attributeNames = attributeNames;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "entityId='" + entityId + '\'' +
            ", attributeNames=" + Arrays.toString(attributeNames) +
            '}';
    }
}
