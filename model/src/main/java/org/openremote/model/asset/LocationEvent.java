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
package org.openremote.model.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.geo.GeoJSONPoint;

import java.util.Arrays;

/**
 * A location event.
 */
public class LocationEvent extends SharedEvent {

    protected String assetId;
    protected GeoJSONPoint coordinates;

    @JsonCreator
    public LocationEvent(
        @JsonProperty("assetId") String assetId,
        @JsonProperty("coordinates") GeoJSONPoint coordinates,
        @JsonProperty("timestamp") long timestamp
    ) {
        super(timestamp);
        this.assetId = assetId;
        this.coordinates = coordinates;
    }

    public String getAssetId() {
        return assetId;
    }

    public GeoJSONPoint getCoordinates() {
        return coordinates;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp=" + timestamp +
            ", assetId=" + assetId +
            ", coordinates=" + (coordinates == null ? "null" : coordinates.toValue()) +
            "}";
    }
}
