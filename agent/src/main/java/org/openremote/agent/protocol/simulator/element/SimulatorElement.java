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
package org.openremote.agent.protocol.simulator.element;

import org.openremote.model.attribute.AttributeType;
import org.openremote.model.value.Value;

public abstract class SimulatorElement<T> {

    final protected AttributeType expectedType;

    protected Value state = null;

    public SimulatorElement(AttributeType expectedType) {
        this.expectedType = expectedType;
    }

    public Value getState() {
        return state;
    }

    public void setState(Value value) {
        if (value != null) {
            if (!isValid(value)) {
                throw new IllegalArgumentException(
                    "Invalid state, expected JSON type '" + expectedType + "' but got '" + value.getType() + "' on: " + this
                );
            }
            this.state = value;
        } else {
            this.state = null;
        }
    }

    protected boolean isValid(Value value) {
        return value == null || value.getType() == expectedType.getValueType();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "state=" + (getState() != null ? getState().toJson() : "null") +
            "}";
    }
}
