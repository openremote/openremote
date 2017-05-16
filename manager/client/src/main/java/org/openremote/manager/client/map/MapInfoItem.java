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
package org.openremote.manager.client.map;

import org.openremote.model.value.Value;

import java.util.Optional;

public class MapInfoItem {

    final protected String label;
    final protected String format;
    final protected Value value;

    public MapInfoItem(String label, String format, Value value) {
        this.label = label;
        this.format = format;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public Optional<String> getFormat() {
        return Optional.ofNullable(format);
    }

    public Optional<Value> getValue() {
        return Optional.ofNullable(value);
    }
}
