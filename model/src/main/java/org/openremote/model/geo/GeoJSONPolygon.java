/*
 * Copyright 2025, OpenRemote Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.openremote.model.geo.GeoJSONPolygon.TYPE;

/**
 * Represents a GeoJSON Polygon geometry.
 * <p>
 * A Polygon is defined by one or more linear rings. The first ring is the exterior ring (outer boundary),
 * and any subsequent rings are interior rings (holes).
 * <p>
 * Each linear ring is a closed coordinate array where the first and last coordinates must be identical.
 */
@JsonTypeName(TYPE)
public class GeoJSONPolygon extends GeoJSONGeometry {

    public static class CoordinateArrayListConverter extends StdConverter<List<Coordinate[]>, double[][][]> {

        @Override
        public double[][][] convert(List<Coordinate[]> rings) {
            if (rings == null || rings.isEmpty()) {
                return new double[0][][];
            }

            double[][][] result = new double[rings.size()][][];
            for (int i = 0; i < rings.size(); i++) {
                Coordinate[] ring = rings.get(i);
                result[i] = new double[ring.length][];
                for (int j = 0; j < ring.length; j++) {
                    Coordinate coord = ring[j];
                    if (Double.isNaN(coord.getZ())) {
                        result[i][j] = new double[]{coord.x, coord.y};
                    } else {
                        result[i][j] = new double[]{coord.x, coord.y, coord.getZ()};
                    }
                }
            }
            return result;
        }
    }

    public static final String TYPE = "Polygon";

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonSerialize(converter = CoordinateArrayListConverter.class)
    protected List<Coordinate[]> coordinates;

    @JsonCreator
    public GeoJSONPolygon(@JsonProperty("coordinates") double[][][] coordinates) {
        super(TYPE);
        Objects.requireNonNull(coordinates);

        if (coordinates.length == 0) {
            throw new IllegalArgumentException("Polygon must have at least one ring (exterior ring)");
        }

        this.coordinates = new ArrayList<>();
        for (double[][] ring : coordinates) {
            if (ring.length < 4) {
                throw new IllegalArgumentException("Each ring must have at least 4 coordinates (forming a closed ring)");
            }

            Coordinate[] coordRing = new Coordinate[ring.length];
            for (int i = 0; i < ring.length; i++) {
                double[] coord = ring[i];
                if (coord.length < 2) {
                    throw new IllegalArgumentException("Each coordinate must have at least 2 values (longitude, latitude)");
                }

                double x = Math.min(180d, Math.max(-180d, coord[0]));
                double y = Math.min(90d, Math.max(-90d, coord[1]));

                if (coord.length >= 3) {
                    coordRing[i] = new Coordinate(x, y, coord[2]);
                } else {
                    coordRing[i] = new Coordinate(x, y);
                }
            }
            this.coordinates.add(coordRing);
        }
    }

    /**
     * Create a simple polygon with only an exterior ring (no holes)
     */
    public GeoJSONPolygon(Coordinate[] exteriorRing) {
        this(new double[][][]{coordinateArrayToDoubleArray(exteriorRing)});
    }

    /**
     * Create a polygon with an exterior ring and optional holes
     */
    public GeoJSONPolygon(Coordinate[] exteriorRing, Coordinate[]... holes) {
        super(TYPE);
        Objects.requireNonNull(exteriorRing);

        if (exteriorRing.length < 4) {
            throw new IllegalArgumentException("Exterior ring must have at least 4 coordinates");
        }

        this.coordinates = new ArrayList<>();
        this.coordinates.add(clampCoordinates(exteriorRing));

        if (holes != null) {
            for (Coordinate[] hole : holes) {
                if (hole.length < 4) {
                    throw new IllegalArgumentException("Each hole must have at least 4 coordinates");
                }
                this.coordinates.add(clampCoordinates(hole));
            }
        }
    }

    private static Coordinate[] clampCoordinates(Coordinate[] coords) {
        Coordinate[] result = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            Coordinate coord = coords[i];
            double x = Math.min(180d, Math.max(-180d, coord.x));
            double y = Math.min(90d, Math.max(-90d, coord.y));
            result[i] = Double.isNaN(coord.getZ()) ? new Coordinate(x, y) : new Coordinate(x, y, coord.getZ());
        }
        return result;
    }

    private static double[][] coordinateArrayToDoubleArray(Coordinate[] coords) {
        double[][] result = new double[coords.length][];
        for (int i = 0; i < coords.length; i++) {
            Coordinate coord = coords[i];
            if (Double.isNaN(coord.getZ())) {
                result[i] = new double[]{coord.x, coord.y};
            } else {
                result[i] = new double[]{coord.x, coord.y, coord.getZ()};
            }
        }
        return result;
    }

    public List<Coordinate[]> getCoordinates() {
        return coordinates;
    }

    @JsonIgnore
    public Coordinate[] getExteriorRing() {
        return coordinates.isEmpty() ? new Coordinate[0] : coordinates.getFirst();
    }

    @JsonIgnore
    public List<Coordinate[]> getHoles() {
        return coordinates.size() > 1 ? coordinates.subList(1, coordinates.size()) : new ArrayList<>();
    }

    @JsonIgnore
    public boolean hasHoles() {
        return coordinates.size() > 1;
    }

    @Override
    public String toString() {
        return "GeoJSONPolygon{" +
            "rings=" + coordinates.size() +
            ", exteriorRingPoints=" + (coordinates.isEmpty() ? 0 : coordinates.getFirst().length) +
            ", holes=" + (coordinates.size() - 1) +
            '}';
    }
}
