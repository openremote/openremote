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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.util.ValueUtil;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Predicate for GEO JSON point values; will return true if the point is within the specified rectangle specified as
 * latitude and longitude values of two corners unless negated.
 */
@JsonSchemaTitle("Rectangular geofence")
@JsonSchemaDescription("Predicate for GEO JSON point values; will return true if the point is within the specified rectangle specified as latitude and longitude values of two corners unless negated.")
public class RectangularGeofencePredicate extends GeofencePredicate {

    public static final String name = "rect";
    public double latMin;
    public double lngMin;
    public double latMax;
    public double lngMax;

    public RectangularGeofencePredicate() {
    }

    @JsonCreator
    public RectangularGeofencePredicate(@JsonProperty("latMin") double latMin,
                                        @JsonProperty("lngMin") double lngMin,
                                        @JsonProperty("latMax") double latMax,
                                        @JsonProperty("lngMax") double lngMax,
                                        @JsonProperty("negated") boolean negated) {
        this.latMin = latMin;
        this.lngMin = lngMin;
        this.latMax = latMax;
        this.lngMax = lngMax;
        this.negated = negated;
    }

    public RectangularGeofencePredicate(@JsonProperty("latMin") double latMin,
                                        @JsonProperty("lngMin") double lngMin,
                                        @JsonProperty("latMax") double latMax,
                                        @JsonProperty("lngMax") double lngMax) {
        this(latMin, lngMin, latMax, lngMax, false);
    }

    @Override
    public RectangularGeofencePredicate negate() {
        negated = !negated;
        return this;
    }

    public double getLatMin() {
        return latMin;
    }

    public double getLngMin() {
        return lngMin;
    }

    public double getLatMax() {
        return latMax;
    }

    public double getLngMax() {
        return lngMax;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RectangularGeofencePredicate that = (RectangularGeofencePredicate) o;
        return negated == that.negated &&
            Double.compare(that.latMin, latMin) == 0 &&
            Double.compare(that.lngMin, lngMin) == 0 &&
            Double.compare(that.latMax, latMax) == 0 &&
            Double.compare(that.lngMax, lngMax) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, negated, latMin, lngMin, latMax, lngMax);
    }

    @Override
    public double[] getCentrePoint() {
        double x = (lngMin + lngMax) / 2;
        double y = (latMin + latMax) / 2;
        return new double[]{x, y};
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

            Envelope envelope = new Envelope(lngMin,
                lngMax,
                latMin,
                latMax);

            if (negated) {
                return !envelope.contains(coordinate);
            }

            return envelope.contains(coordinate);
        };
    }
}
