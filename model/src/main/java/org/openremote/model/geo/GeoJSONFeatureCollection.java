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
package org.openremote.model.geo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openremote.model.geo.GeoJSONFeatureCollection.TYPE;

@JsonTypeName(TYPE)
public class GeoJSONFeatureCollection extends GeoJSON {

    public static final String TYPE = "FeatureCollection";
    public static final GeoJSONFeatureCollection EMPTY = new GeoJSONFeatureCollection((List<GeoJSONFeature>)null);

    @JsonProperty
    protected List<GeoJSONFeature> features;

    @JsonCreator
    public GeoJSONFeatureCollection(@JsonProperty("features") GeoJSONFeature... features) {
        this(Arrays.asList(features));
    }

    public GeoJSONFeatureCollection(List<GeoJSONFeature> features) {
        super(TYPE);
        this.features = features != null ? features : new ArrayList<>(0);
    }

    public GeoJSONFeature[] getFeatures() {
        return features.toArray(new GeoJSONFeature[features.size()]);
    }
}
