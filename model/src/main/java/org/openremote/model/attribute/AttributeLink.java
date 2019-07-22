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
import org.openremote.model.value.*;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A link from one attribute to another with a definition of how to map the value from the source attribute
 * to the linked attribute.
 */
public class AttributeLink {

    public enum ConverterType {

        /**
         * Ignore this value (do not send to the linked attribute)
         */
        IGNORE("@IGNORE"),

        /**
         * Send a null vale to the linked attribute
         */
        NULL("@NULL"),

        /**
         * Toggle the value of the linked attribute; the linked attribute's type must be
         * {@link AttributeValueType#BOOLEAN}
         */
        TOGGLE("@TOGGLE"),

        /**
         * Increment the value of the linked attribute; the linked attribute's type must be
         * {@link AttributeValueType#NUMBER}
         */
        INCREMENT("@INCREMENT"),

        /**
         * Decrement the value of the linked attribute; the linked attribute's type must be
         * {@link AttributeValueType#NUMBER}
         */
        DECREMENT("@DECREMENT");

        private String value;
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

    protected AttributeRef attributeRef;
    protected ObjectValue converter;
    protected ValueFilter[] filters;

    @JsonCreator
    public AttributeLink(@JsonProperty("attributeRef") AttributeRef attributeRef,
                         @JsonProperty("converter") ObjectValue converter,
                         @JsonProperty("filters") ValueFilter[] filters) {
        this.attributeRef = requireNonNull(attributeRef);
        this.converter = converter;
        this.filters = filters;
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public Optional<ObjectValue> getConverter() {
        return Optional.ofNullable(converter);
    }

    public void setConverter(ObjectValue converter) {
        this.converter = converter;
    }

    public void setAttributeRef(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
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
            "attributeRef=" + attributeRef +
            ", converter=" + converter +
            '}';
    }

    public static boolean isAttributeLink(Value value) {
        return Values.getObject(value)
            .map(objectValue -> {
                return AttributeRef.isAttributeRef(objectValue.get("attributeRef").orElse(null)) &&
                    objectValue.get("converter").map(v -> v.getType() == ValueType.OBJECT).orElse(true);
            })
            .orElse(false);
    }
}