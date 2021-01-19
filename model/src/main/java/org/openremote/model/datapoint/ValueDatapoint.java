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
package org.openremote.model.datapoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueDatapoint<T> {

    protected long timestamp;
    protected T value;

    protected ValueDatapoint() {
    }

    @JsonCreator
    public ValueDatapoint(@JsonProperty("x") long timestamp,
                          @JsonProperty("y") T value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    @JsonProperty("x")
    public long getTimestamp() {
        return timestamp;
    }

    @JsonProperty("y")
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp='" + timestamp + '\'' +
            ", value=" + value +
            '}';
    }
}
