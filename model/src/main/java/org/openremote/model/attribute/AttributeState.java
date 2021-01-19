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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * The desired or current or past state of an {@link AttributeRef}.
 * <p>
 * <code>null</code> is a valid {@link #value}.
 * </p>
 */
public class AttributeState implements Serializable {

    @JsonProperty
    protected AttributeRef ref;
    protected Object value;
    protected boolean deleted;

    AttributeState() {}

    public AttributeState(String assetId, Attribute<?> attribute) {
        this(assetId, attribute.getName(), attribute.getValue().orElse(null));
    }

    public AttributeState(String assetId, String attributeName, Object value) {
        this(new AttributeRef(assetId, attributeName), value);
    }

    public AttributeState(AttributeRef ref, Object value) {
        this.ref = Objects.requireNonNull(ref);
        this.value = value;
    }

    /**
     * Sets the {@link #value} to <code>null</code>.
     */
    public AttributeState(AttributeRef ref) {
        this(ref, null);
    }

    public AttributeRef getRef() {
        return ref;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getValue() {
        return Optional.ofNullable(value).map(v -> (T)v);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "ref=" + ref +
            ", value=" + value +
            ", deleted=" + deleted +
            '}';
    }
}
