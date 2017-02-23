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

import elemental.json.Json;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import org.openremote.model.AttributeType;

public abstract class SimulatorElement<T> {

    final protected AttributeType expectedType;
    protected JsonValue state = Json.createNull();

    public SimulatorElement(AttributeType expectedType) {
        this.expectedType = expectedType;
    }

    public JsonValue getState() {
        return state;
    }

    public void setState(JsonValue state) {
        if (state == null) {
            this.state = Json.createNull();
            return;
        }
        if (isValid(state)) {
            this.state = state;
        } else {
            throw new IllegalArgumentException("Invalid state, expected '" + expectedType + "': " + state);
        }
    }

    protected boolean isValid(JsonValue value) {
        return value == null || value.getType() == JsonType.NULL || value.getType() == expectedType.getJsonType();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "state=" + getState().asString() +
            "}";
    }
}
