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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.SharedEvent;

/**
 * A client sends this event to the server to refresh the value of the specified attribute, expecting the server to
 * answer "soon" with an {@link AttributeEvent}. If the server decides that the client doesn't have the right
 * permissions, or if anything else is not in order (e.g. the asset doesn't exist), the server might not react at all.
 */
public class ReadAttributeEvent extends SharedEvent {

    protected AttributeRef ref;

    @JsonCreator
    public ReadAttributeEvent(@JsonProperty("ref") AttributeRef attributeRef) {
        this.ref = attributeRef;
    }

    public ReadAttributeEvent(String assetId, String attributeName) {
        this.ref = new AttributeRef(assetId, attributeName);
    }

    public AttributeRef getAttributeRef() {
        return ref;
    }

    public void setAttributeRef(AttributeRef ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "ref='" + ref + '\'' +
            '}';
    }
}
