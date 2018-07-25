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

import java.util.Objects;

public class RectangularLocationPredicate extends LocationPredicate {

    public static final String name = "rect";
    public boolean negated;
    public double latMin;
    public double lngMin;
    public double latMax;
    public double lngMax;

    @JsonCreator
    public RectangularLocationPredicate(@JsonProperty("latMin") double latMin,
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

    public RectangularLocationPredicate(@JsonProperty("latMin") double latMin,
                                        @JsonProperty("lngMin") double lngMin,
                                        @JsonProperty("latMax") double latMax,
                                        @JsonProperty("lngMax") double lngMax) {
        this(latMin, lngMin, latMax, lngMax, false);
    }

    public RectangularLocationPredicate negate() {
        negated = true;
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
        RectangularLocationPredicate that = (RectangularLocationPredicate) o;
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
}
