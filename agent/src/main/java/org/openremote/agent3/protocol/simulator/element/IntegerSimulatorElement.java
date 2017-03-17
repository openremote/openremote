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
package org.openremote.agent3.protocol.simulator.element;

import elemental.json.JsonValue;
import org.openremote.model.AttributeType;

public class IntegerSimulatorElement extends SimulatorElement {

    public static final String ELEMENT_NAME_INTEGER = "integer";
    public static final String ELEMENT_NAME_RANGE = "range";

    final protected Double min;
    final protected Double max;

    public IntegerSimulatorElement(boolean reflectActuatorWrites) {
        this(reflectActuatorWrites, null, null);
    }

    public IntegerSimulatorElement(boolean reflectActuatorWrites, Double min, Double max) {
        super(AttributeType.INTEGER, reflectActuatorWrites);
        this.min = min;
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    protected boolean isValid(JsonValue value) {
        boolean valid = super.isValid(value);
        return valid && (
            value == null || (
                (getMin() == null || value.asNumber() >= getMin())
                    && (getMax() == null || value.asNumber() <= getMax())
            )
        );
    }

}
