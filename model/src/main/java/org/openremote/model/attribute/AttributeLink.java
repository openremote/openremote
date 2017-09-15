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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

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
         * {@link AttributeType#BOOLEAN}
         */
        TOGGLE("@TOGGLE"),

        /**
         * Increment the value of the linked attribute; the linked attribute's type must be
         * {@link AttributeType#NUMBER}
         */
        INCREMENT("@INCREMENT"),

        /**
         * Decrement the value of the linked attribute; the linked attribute's type must be
         * {@link AttributeType#NUMBER}
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

    public AttributeLink(String entityId, String attributeName, ObjectValue converter) {
        this(new AttributeRef(entityId, attributeName), converter);
    }

    /**
     * @param converter can be <code>null</code> if no conversions are defined (i.e. value is direct write through).
     */
    public AttributeLink(AttributeRef attributeRef, ObjectValue converter) {
        this.attributeRef = requireNonNull(attributeRef);
        this.converter = converter;
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    @JsonIgnore
    public Optional<ObjectValue> getConverter() {
        return Optional.ofNullable(converter);
    }

    public void setConverter(ObjectValue converter) {
        this.converter = converter;
    }

    // For GWT jackson only
    @JsonProperty("converter")
    private ObjectValue getConverterPrivate() {
        return converter;
    }

    public ObjectValue toObjectValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("attributeRef", getAttributeRef().toArrayValue());
        objectValue.put("converter", converter);
        return objectValue;
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

    @SuppressWarnings("ConstantConditions")
    public static Optional<AttributeLink> fromValue(Value value) {
        return Values.getObject(value)
            .filter(AttributeLink::isAttributeLink)
            .flatMap(objectValue ->
                AttributeRef.fromValue(objectValue.get("attributeRef").get())
                    .map(attrRef -> new AttributeLink(attrRef, objectValue.getObject("converter").orElse(null)))
            );
    }
}