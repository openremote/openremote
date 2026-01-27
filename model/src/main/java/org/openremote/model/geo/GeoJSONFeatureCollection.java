/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.geo;

import static org.openremote.model.geo.GeoJSONFeatureCollection.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(TYPE)
public class GeoJSONFeatureCollection extends GeoJSON {

  public static final String TYPE = "FeatureCollection";
  public static final GeoJSONFeatureCollection EMPTY =
      new GeoJSONFeatureCollection((List<GeoJSONFeature>) null);

  @JsonProperty protected List<GeoJSONFeature> features;

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
