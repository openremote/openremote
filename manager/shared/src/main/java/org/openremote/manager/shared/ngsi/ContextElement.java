/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.ngsi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ContextElement extends ContextEntity {
    @JsonInclude
    protected List<Attribute> attributes;

    public ContextElement(@JsonProperty("type") String type, @JsonProperty("id") String id, @JsonProperty("pattern") boolean pattern, @JsonProperty("attributes") List<Attribute> attributes) {
        super(type, id, pattern);
        this.attributes = attributes;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }
}
