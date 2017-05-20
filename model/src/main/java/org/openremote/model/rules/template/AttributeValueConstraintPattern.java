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

import org.openremote.model.attribute.AttributeValueConstraint;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;

public class AttributeValueConstraintPattern implements TemplatePattern {

    public static final String TYPE = "attribute-value-constraint";

    final protected String attributeName;
    final protected AttributeValueConstraint restriction;

    public AttributeValueConstraintPattern(String attributeName, AttributeValueConstraint restriction) {
        Objects.requireNonNull(attributeName);
        Objects.requireNonNull(restriction);
        this.attributeName = attributeName;
        this.restriction = restriction;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public AttributeValueConstraint getRestriction() {
        return restriction;
    }

    @Override
    public String render() {
        return "attributeName == \"" + getAttributeName() + "\"" +
            ", " + getRestriction().renderRulesTemplatePattern();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeName='" + attributeName + '\'' +
            ", restriction=" + restriction +
            '}';
    }

    @Override
    public ObjectValue toModelValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("type", TYPE);
        objectValue.put("attributeName", attributeName);
        objectValue.put("restriction", restriction.toModelValue());
        return objectValue;
    }

    public static Optional<AttributeValueConstraintPattern> fromModelValue(Value value) {
        return Values.getObject(value)
            .filter(v -> v.getString("type").filter(s -> s.equals(TYPE)).map(s -> true).orElse(false))
            .flatMap(objectValue -> objectValue
                .getString("attributeName")
                .map(attributeName -> new Pair<>(
                    attributeName, objectValue.getObject("restriction").flatMap(AttributeValueConstraint::fromModelValue))
                )
                .filter(pair -> pair.value.isPresent())
                .map(pair -> new AttributeValueConstraintPattern(pair.key, pair.value.get()))
            );
    }
}
