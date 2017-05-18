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
package org.openremote.model.rules.template;

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;

public class AttributeValueConstraint implements Constraint {

    final protected String attributeName;
    final protected ValueComparator valueComparator;
    final protected Value value;
    final protected boolean caseSensitive;

    public AttributeValueConstraint(String attributeName, ValueComparator valueComparator, Value value, boolean caseSensitive) throws IllegalStateException {
        Objects.requireNonNull(attributeName);
        Objects.requireNonNull(valueComparator);
        Objects.requireNonNull(value);
        this.attributeName = attributeName;
        this.valueComparator = valueComparator;
        this.value = value;
        this.caseSensitive = caseSensitive;
        if (!valueComparator.isValid(value)) {
            throw new IllegalArgumentException("Invalid value for comparator " + valueComparator + ": " + value);
        }
    }

    @Override
    public ObjectValue toObjectValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("type", "attribute-value");
        objectValue.put("attributeName", attributeName);
        objectValue.put("valueComparator", valueComparator.name());
        objectValue.put("caseSensitive", caseSensitive);
        objectValue.put("value", value);
        return objectValue;
    }

    public static Optional<AttributeValueConstraint> fromValue(Value value) {
        return Values.getObject(value)
            .filter(v -> v.getString("type").filter(s -> s.equals("attribute-value")).map(s -> true).orElse(false))
            .flatMap(objectValue -> {
                String attributeName =
                    objectValue.getString("attributeName").orElse(null);
                ValueComparator valueComparator =
                    objectValue.getString("valueComparator").map(ValueComparator::fromString).orElse(null);
                boolean caseSensitive = objectValue.getBoolean("caseSensitive").orElse(false);
                Value v = objectValue.get("value").orElse(null);
                return attributeName != null && valueComparator != null && v != null
                    ? Optional.of(new AttributeValueConstraint(attributeName, valueComparator, v, caseSensitive))
                    : Optional.empty();
            });
    }

    @Override
    public String render() {
        return "attributeName == \"" + attributeName + "\"" +
            ", " + valueComparator.render(value, caseSensitive);
    }
}
