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

public class GeoJSON extends AbstractTypeHolder {

    public static final GeoJSON EMPTY_FEATURE_COLLECTION = new GeoJSON("FeatureCollection").setEmptyFeatures();;

    public GeoJSON(String type) {
        super(type);
    }

    public GeoJSON setFeatures(GeoJSONFeature... features) {
        if (features != null) {
            for (int i = 0; i < features.length; i++) {
                GeoJSONFeature feature = features[i];
                ArrayValue array = Values.createArray();
                array.set(i, feature.getObjectValue());
                objectValue.put("features", array);
            }
        } else {
            return setEmptyFeatures();
        }
        return this;
    }

    public GeoJSON setEmptyFeatures() {
        objectValue.put("features", Values.createArray());
        return this;
    }
}
