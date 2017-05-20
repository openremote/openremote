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
package org.openremote.model.attribute;

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueComparator;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Simple binary value comparison operation.
 */
public class AttributeValueConstraint {

    final protected ValueComparator valueComparator;
    final protected Value value;

    public AttributeValueConstraint(ValueComparator valueComparator, Value value) throws IllegalArgumentException {
        Objects.requireNonNull(valueComparator);
        Objects.requireNonNull(value);
        this.valueComparator = valueComparator;
        this.value = value;
        if (!valueComparator.isApplicable(value.getType())) {
            throw new IllegalArgumentException("Comparator " + valueComparator + " can not be applied on: " + value);
        }
    }

    public ValueComparator getValueComparator() {
        return valueComparator;
    }

    public Value getValue() {
        return value;
    }

    public boolean apply(Attribute attribute, Supplier<Boolean> emptyDefault) {
        return attribute.getValue()
            .map(v -> getValueComparator().apply(v, getValue()))
            .orElseGet(emptyDefault);
    }

    public boolean apply(Value v) {
        return getValueComparator().apply(v, getValue());
    }

    public String renderRulesTemplatePattern() {
        return getValueComparator().renderRulesTemplatePattern(getValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "valueComparator=" + valueComparator +
            ", value=" + value +
            '}';
    }

    public ObjectValue toModelValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("valueComparator", valueComparator.name());
        objectValue.put("value", value);
        return objectValue;
    }

    public static Optional<AttributeValueConstraint> fromModelValue(Value value) {
        return Values.getObject(value)
            .filter(v -> v.getString("valueComparator").isPresent())
            .filter(v -> v.getString("value").isPresent())
            .flatMap(objectValue -> objectValue
                .getString("valueComparator")
                .flatMap(ValueComparator::fromString)
                .map(comparator -> new AttributeValueConstraint(comparator, objectValue.get("value").get()))
            );
    }
}
