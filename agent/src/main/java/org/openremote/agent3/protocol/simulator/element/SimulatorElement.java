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

import java.util.Optional;

import static org.openremote.model.util.JsonUtil.replaceJsonNull;

public abstract class SimulatorElement<T> {

    final protected AttributeType expectedType;

    protected JsonValue state = null;

    public SimulatorElement(AttributeType expectedType) {
        this.expectedType = expectedType;
    }

    public JsonValue getState() {
        return state;
    }

    public void setState(Optional<JsonValue> state) {
        // This is only used server side so JsonValue optional issue isn't a problem
        state
            .map(st -> {
                st = replaceJsonNull(st);
                if (!isValid(st)) {
                    throw new IllegalArgumentException(
                        "Invalid state, expected JSON type '" + expectedType + "' but got '" + st.getType() + "' on: " + this
                    );
                }
                this.state = st;
                return st;
            })
            .orElseGet(() -> {
                this.state = null;
                return null;
            });
    }

    protected boolean isValid(JsonValue value) {
        return value == null || value.getType() == expectedType.getJsonType();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "state=" + (getState() != null ? getState().toJson() : "null") +
            "}";
    }
}
