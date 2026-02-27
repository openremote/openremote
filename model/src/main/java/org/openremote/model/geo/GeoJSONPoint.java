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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.locationtech.jts.geom.Coordinate;

import java.util.Objects;

import static org.openremote.model.geo.GeoJSONPoint.TYPE;

@JsonTypeName(TYPE)
public class GeoJSONPoint extends GeoJSONGeometry {

    public static class CoordinateArrayConverter extends StdConverter<Coordinate, double[]> {

        @Override
        public double[] convert(Coordinate value) {
            if (Double.isNaN(value.getZ())) {
                return new double[] {value.x, value.y};
            }
            return new double[] {value.x, value.y, value.getZ()};
        }
    }

    public static final String TYPE = "Point";
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonSerialize(converter = CoordinateArrayConverter.class)
    protected Coordinate coordinates;

    @JsonCreator
    public GeoJSONPoint(@JsonProperty("coordinates") Coordinate coordinates) {
        super(TYPE);
        Objects.requireNonNull(coordinates);
        coordinates.x = Math.min(180d, Math.max(-180d, coordinates.x));
        coordinates.y = Math.min(90d, Math.max(-90d, coordinates.y));
        this.coordinates = coordinates;
    }

    public GeoJSONPoint(double x, double y) {
        this(new Coordinate(x, y));
    }

    public GeoJSONPoint(double x, double y, double z) {
        this(new Coordinate(x, y, z));
    }

    public Coordinate getCoordinates() {
        return coordinates;
    }

    @JsonIgnore
    public double getX() {
        return coordinates.x;
    }

    @JsonIgnore
    public double getY() {
        return coordinates.y;
    }

    @JsonIgnore
    public Double getZ() {
        return coordinates.getZ();
    }

    @JsonIgnore
    public boolean hasZ() {
        return coordinates.getZ() != Coordinate.NULL_ORDINATE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoJSONPoint)) return false;
        if (!super.equals(o)) return false;
        GeoJSONPoint that = (GeoJSONPoint) o;
        return Objects.equals(coordinates, that.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coordinates);
    }

    @Override
    public String toString() {
        return "GeoJSONPoint{" +
            "coordinates=" + (coordinates != null ? (coordinates.x + ", " + coordinates.y + ", " + coordinates.getZ()) : "null") +
            '}';
    }
}
