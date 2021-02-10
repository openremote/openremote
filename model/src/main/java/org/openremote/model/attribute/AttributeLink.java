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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.value.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A link from one attribute to another with a definition of how to map the value from the source attribute
 * to the linked attribute.
 */
// TODO: Somehow combine this with flow rules
public class AttributeLink implements Serializable {

    public enum ConverterType {

        /**
         * Toggle the value of the linked attribute; the linked attribute's type must be
         * {@link Boolean}
         */
        TOGGLE("@TOGGLE"),

        /**
         * Increment the value of the linked attribute; the linked attribute's type must be
         * {@link Number}
         */
        INCREMENT("@INCREMENT"),

        /**
         * Decrement the value of the linked attribute; the linked attribute's type must be
         * {@link Number}
         */
        DECREMENT("@DECREMENT"),

        /**
         * Negates the value; the linked attribute's type must be
         * {@link Number} or {@link Boolean}
         */
        NEGATE("@NEGATE");

        private final String value;
        // Prevents cloning of values each time fromString is called
        private static final ConverterType[] copyOfValues = values();

        ConverterType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Optional<ConverterType> fromValue(String value) {
            for (ConverterType converter : copyOfValues) {
                if (converter.getValue().equalsIgnoreCase(value))
                    return Optional.of(converter);
            }
            return Optional.empty();
        }
    }

    protected AttributeRef ref;
    protected ObjectNode converter;
    protected ValueFilter[] filters;

    @JsonCreator
    public AttributeLink(@JsonProperty("ref") AttributeRef ref,
                         @JsonProperty("converter") ObjectNode converter,
                         @JsonProperty("filters") ValueFilter[] filters) {
        this.ref = requireNonNull(ref);
        this.converter = converter;
        this.filters = filters;
    }

    public AttributeRef getAttributeRef() {
        return ref;
    }

    public Optional<ObjectNode> getConverter() {
        return Optional.ofNullable(converter);
    }

    public void setConverter(ObjectNode converter) {
        this.converter = converter;
    }

    public void setAttributeRef(AttributeRef ref) {
        this.ref = ref;
    }

    public ValueFilter[] getFilters() {
        return filters;
    }

    public void setFilters(ValueFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "ref=" + ref +
            ", converter=" + converter +
            ", filters=" + (filters != null ? Arrays.toString(filters) : "null") +
            '}';
    }
}
