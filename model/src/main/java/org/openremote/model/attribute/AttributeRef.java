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
package org.openremote.model.attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

/**
 * A reference to an entity and an {@link Attribute}.
 * <p>
 * The {@link #id} and {@link #name} are required to identify an asset's attribute.
 * <p>
 * Two attribute references are {@link #equals} if they reference the same asset
 * and attribute.
 */
public class AttributeRef implements Serializable {

    protected String id;
    protected String name;

    @JsonCreator
    public AttributeRef(@JsonProperty("id") String id, @JsonProperty("name") String name) {
        requireNonNullAndNonEmpty(id);
        requireNonNullAndNonEmpty(name);
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeRef that = (AttributeRef) o;
        return id.equals(that.id) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            '}';
    }
}
