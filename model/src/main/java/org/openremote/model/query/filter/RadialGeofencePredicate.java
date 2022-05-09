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
package org.openremote.model.query.filter;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.referencing.GeodeticCalculator;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.util.ValueUtil;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Predicate for GEO JSON point values; will return true if the point is within the specified radius of the specified
 * latitude and longitude unless negated.
 */
@JsonSchemaTitle("Radial geofence")
@JsonSchemaDescription("Predicate for GEO JSON point values; will return true if the point is within the specified radius of the specified latitude and longitude unless negated.")
public class RadialGeofencePredicate extends GeofencePredicate {

    public static final String name = "radial";
    public int radius;
    public double lat;
    public double lng;

    public RadialGeofencePredicate() {
    }

    @JsonCreator
    public RadialGeofencePredicate(@JsonProperty("radius") int radius,
                                   @JsonProperty("lat") double lat,
                                   @JsonProperty("lng") double lng,
                                   @JsonProperty("negated") boolean negated) {
        this.radius = radius;
        this.lat = lat;
        this.lng = lng;
        this.negated = negated;
    }

    public RadialGeofencePredicate(@JsonProperty("radius") int radius,
                                   @JsonProperty("lat") double lat,
                                   @JsonProperty("lng") double lng) {
        this(radius, lat, lng, false);
    }

    @Override
    public RadialGeofencePredicate negate() {
        negated = !negated;
        return this;
    }

    public RadialGeofencePredicate radius(int radius) {
        this.radius = radius;
        return this;
    }

    public RadialGeofencePredicate lat(double lat) {
        this.lat = lat;
        return this;
    }

    public RadialGeofencePredicate lng(double lng) {
        this.lng = lng;
        return this;
    }

    public int getRadius() {
        return radius;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadialGeofencePredicate that = (RadialGeofencePredicate) o;
        return negated == that.negated &&
            radius == that.radius &&
            Double.compare(that.lat, lat) == 0 &&
            Double.compare(that.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, negated, radius, lat, lng);
    }

    @Override
    public double[] getCentrePoint() {
        return new double[]{lng, lat};
    }

    @Override
    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {

        return obj -> {
            if (obj == null) return false;

            Coordinate coordinate;

            if (obj instanceof Coordinate) {
                coordinate = (Coordinate) obj;
            } else {
                coordinate = ValueUtil.getValue(obj, GeoJSONPoint.class).map(GeoJSONPoint::getCoordinates).orElse(null);
            }

            if (coordinate == null) {
                return false;
            }

            coordinate.x = Math.min(180d, Math.max(-180d, coordinate.x));
            coordinate.y = Math.min(90d, Math.max(-90d, coordinate.y));

            GeodeticCalculator calculator = new GeodeticCalculator();
            calculator.setStartingGeographicPoint(lng, lat);
            calculator.setDestinationGeographicPoint(coordinate.x, coordinate.y);
            if (negated) {
                return calculator.getOrthodromicDistance() > radius;
            }
            return calculator.getOrthodromicDistance() <= radius;
        };
    }
}
