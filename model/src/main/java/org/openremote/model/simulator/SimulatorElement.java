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
package org.openremote.model.simulator;

import org.openremote.model.ValidationFailure;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.value.Value;

import java.util.ArrayList;
import java.util.List;

public abstract class SimulatorElement {

    final protected AttributeRef attributeRef;
    final protected AttributeType expectedType;
    protected Value value = null;

    public SimulatorElement(AttributeRef attributeRef, AttributeType expectedType) {
        this.attributeRef = attributeRef;
        this.expectedType = expectedType;
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public AttributeType getExpectedType() {
        return expectedType;
    }

    public Value getValue() {
        return value;
    }

    public List<ValidationFailure> setValue(Value value) {
        List<ValidationFailure> failures = new ArrayList<>();
        if (value != null) {
            failures.addAll(getValidationFailures(value));
            if (failures.isEmpty()) {
                this.value = value;
            }
        } else {
            this.value = null;
        }
        return failures;
    }

    protected List<ValidationFailure> getValidationFailures(Value value) {
        List<ValidationFailure> failures = new ArrayList<>();
        expectedType.isValidValue(value).ifPresent(failures::add);
        return failures;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "value=" + (getValue() != null ? getValue().toJson() : "null") +
            "}";
    }
}
