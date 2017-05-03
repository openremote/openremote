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
package org.openremote.model.geo;

import org.openremote.model.AbstractTypeHolder;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

public class GeoJSONFeature extends AbstractTypeHolder {

    public GeoJSONFeature(String type) {
        super(type);
    }

    public GeoJSONFeature setProperty(String name, String value) {
        return setProperty(name, Values.create(value));
    }

    protected GeoJSONFeature setProperty(String name, Value value) {
        ObjectValue properties = objectValue.getObject("properties").orElse(Values.createObject());
        properties.put(name, value);
        if (!objectValue.hasKey("properties"))
            objectValue.put("properties", properties);
        return this;
    }

    public GeoJSONFeature setGeometry(GeoJSONGeometry geometry) {
        if (geometry != null) {
            objectValue.put("geometry", geometry.getObjectValue());
        } else {
            objectValue.remove("geometry");
        }
        return this;
    }
}
