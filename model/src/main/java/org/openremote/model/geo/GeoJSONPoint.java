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
import org.geotools.referencing.GeodeticCalculator;
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

    /**
     * Parses a raw location string formatted as "lon,lat" into a {@link GeoJSONPoint}.
     *
     * @param rawLocation the raw location string
     * @return the parsed point, or null if the input is invalid
     */
    @JsonIgnore
    public static GeoJSONPoint parseRawLocation(String rawLocation) {
        if (rawLocation == null) {
            return null;
        }
        String[] parts = rawLocation.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double lon = Double.parseDouble(parts[0].trim());
            double lat = Double.parseDouble(parts[1].trim());
            return new GeoJSONPoint(lon, lat);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Returns a new {@link GeoJSONPoint} offset from this point by the provided east/north distances in meters.
     * Uses GeoTools {@link GeodeticCalculator} for geodetic accuracy.
     *
     * @param eastMeters  distance to move east in meters
     * @param northMeters distance to move north in meters
     * @return a new point offset from this point
     */
    public GeoJSONPoint offsetByMeters(double eastMeters, double northMeters) {
        double distance = Math.hypot(eastMeters, northMeters);
        if (distance == 0d) {
            return new GeoJSONPoint(getX(), getY());
        }
        double azimuth = Math.toDegrees(Math.atan2(eastMeters, northMeters));
        if (azimuth < 0d) {
            azimuth += 360d;
        }
        GeodeticCalculator calculator = new GeodeticCalculator();
        calculator.setStartingGeographicPoint(getX(), getY());
        calculator.setDirection(azimuth, distance);
        java.awt.geom.Point2D destination = calculator.getDestinationGeographicPoint();
        return new GeoJSONPoint(destination.getX(), destination.getY());
    }

    @Override
    public String toString() {
        return "GeoJSONPoint{" +
            "coordinates=" + (coordinates != null ? (coordinates.x + ", " + coordinates.y + ", " + coordinates.getZ()) : "null") +
            '}';
    }
}
