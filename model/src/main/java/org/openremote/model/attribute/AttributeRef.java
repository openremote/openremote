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

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;

import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

/**
 * A reference to an entity and an {@link Attribute}.
 * <p>
 * The {@link #entityId} and {@link #attributeName} are required to identify
 * an entity's attribute.
 * <p>
 * Two attribute references are {@link #equals} if they reference the same entity
 * and attribute.
 */
public class AttributeRef {

    protected String entityId;
    protected String attributeName;

    protected AttributeRef() {
    }

    public AttributeRef(String entityId, String attributeName) {
        requireNonNullAndNonEmpty(entityId);
        requireNonNullAndNonEmpty(attributeName);
        this.entityId = entityId;
        this.attributeName = attributeName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeRef that = (AttributeRef) o;
        return entityId.equals(that.entityId) && attributeName.equals(that.attributeName);
    }

    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + attributeName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "entityId='" + entityId + '\'' +
            ", attributeName='" + attributeName + '\'' +
            '}';
    }

    public ArrayValue toArrayValue() {
        ArrayValue arrayValue = Values.createArray();
        arrayValue.set(0, Values.create(getEntityId()));
        arrayValue.set(1, Values.create(getAttributeName()));
        return arrayValue;
    }

    public static boolean isAttributeRef(AbstractValueHolder valueHolder) {
        return valueHolder != null
            && valueHolder.getValueAsArray().filter(AttributeRef::isAttributeRef).isPresent();
    }

    public static boolean isAttributeRef(Value value) {
        boolean result = Values.getArray(value)
            .map(arrayValue ->
                arrayValue.length() == 2
                    && arrayValue.getString(0).filter(s -> !s.isEmpty()).isPresent()
                    && arrayValue.getString(1).filter(s -> !s.isEmpty()).isPresent()
            )
            .orElse(
                Values.getObject(value)
                    .map(objectValue ->
                        objectValue.getString("entityId").isPresent() && objectValue.getString("attributeName").isPresent()
                    ).orElse(false)
            );

        return result;
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<AttributeRef> fromValue(Value value) {
        return Values.getArray(value)
            .filter(AttributeRef::isAttributeRef)
            .map(arrayValue ->
                new AttributeRef(arrayValue.getString(0).get(), arrayValue.getString(1).get())
            );
    }
}