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
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Values;

public class GeoJSONGeometry extends AbstractTypeHolder {

    public GeoJSONGeometry(String type) {
        super(type);
    }

    /**
     * Of type "Point".
     */
    public GeoJSONGeometry(double[] coordinates) {
        this("Point");
        ArrayValue array = Values.createArray();
        for (int i = 0; i < coordinates.length; i++) {
            array.set(i, coordinates[i]);
        }
        objectValue.put("coordinates", array);
    }
}
