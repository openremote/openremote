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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.openremote.model.geo.GeoJSONMultiPolygon.TYPE;

/**
 * Represents a GeoJSON MultiPolygon geometry.
 * <p>
 * A MultiPolygon is an array of Polygon coordinate arrays. Each polygon can have holes.
 */
@JsonTypeName(TYPE)
public class GeoJSONMultiPolygon extends GeoJSONGeometry {

    public static class CoordinateListConverter extends StdConverter<List<List<Coordinate[]>>, double[][][][]> {

        @Override
        public double[][][][] convert(List<List<Coordinate[]>> polygons) {
            if (polygons == null || polygons.isEmpty()) {
                return new double[0][][][];
            }

            double[][][][] result = new double[polygons.size()][][][];
            for (int i = 0; i < polygons.size(); i++) {
                List<Coordinate[]> rings = polygons.get(i);
                result[i] = new double[rings.size()][][];
                for (int j = 0; j < rings.size(); j++) {
                    Coordinate[] ring = rings.get(j);
                    result[i][j] = new double[ring.length][];
                    for (int k = 0; k < ring.length; k++) {
                        Coordinate coord = ring[k];
                        if (Double.isNaN(coord.getZ())) {
                            result[i][j][k] = new double[]{coord.x, coord.y};
                        } else {
                            result[i][j][k] = new double[]{coord.x, coord.y, coord.getZ()};
                        }
                    }
                }
            }
            return result;
        }
    }

    public static final String TYPE = "MultiPolygon";

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonSerialize(converter = CoordinateListConverter.class)
    protected List<List<Coordinate[]>> coordinates;

    @JsonCreator
    public GeoJSONMultiPolygon(@JsonProperty("coordinates") double[][][][] coordinates) {
        super(TYPE);
        Objects.requireNonNull(coordinates);
        
        if (coordinates.length == 0) {
            throw new IllegalArgumentException("MultiPolygon must have at least one polygon");
        }

        this.coordinates = new ArrayList<>();
        for (double[][][] polygon : coordinates) {
            if (polygon.length == 0) {
                throw new IllegalArgumentException("Each polygon must have at least one ring (exterior ring)");
            }

            List<Coordinate[]> rings = new ArrayList<>();
            for (double[][] ring : polygon) {
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
                rings.add(coordRing);
            }
            this.coordinates.add(rings);
        }
    }

    /**
     * Create a MultiPolygon from multiple GeoJSONPolygon objects
     */
    public GeoJSONMultiPolygon(GeoJSONPolygon... polygons) {
        super(TYPE);
        Objects.requireNonNull(polygons);
        
        if (polygons.length == 0) {
            throw new IllegalArgumentException("MultiPolygon must have at least one polygon");
        }

        this.coordinates = new ArrayList<>();
        for (GeoJSONPolygon polygon : polygons) {
            this.coordinates.add(polygon.getCoordinates());
        }
    }

    /**
     * Create a MultiPolygon from coordinate arrays (each representing a simple polygon without holes)
     */
    public GeoJSONMultiPolygon(List<Coordinate[]> polygonRings) {
        super(TYPE);
        Objects.requireNonNull(polygonRings);
        
        if (polygonRings.isEmpty()) {
            throw new IllegalArgumentException("MultiPolygon must have at least one polygon");
        }

        this.coordinates = new ArrayList<>();
        for (Coordinate[] ring : polygonRings) {
            if (ring.length < 4) {
                throw new IllegalArgumentException("Each ring must have at least 4 coordinates");
            }
            List<Coordinate[]> polygon = new ArrayList<>();
            polygon.add(clampCoordinates(ring));
            this.coordinates.add(polygon);
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

    public List<List<Coordinate[]>> getCoordinates() {
        return coordinates;
    }

    @JsonIgnore
    public int getPolygonCount() {
        return coordinates.size();
    }

    @JsonIgnore
    public List<Coordinate[]> getPolygon(int index) {
        if (index < 0 || index >= coordinates.size()) {
            throw new IndexOutOfBoundsException("Polygon index out of bounds: " + index);
        }
        return coordinates.get(index);
    }

    @JsonIgnore
    public List<GeoJSONPolygon> getPolygons() {
        List<GeoJSONPolygon> polygons = new ArrayList<>();
        for (List<Coordinate[]> rings : coordinates) {
            if (rings.isEmpty()) continue;
            
            Coordinate[] exteriorRing = rings.get(0);
            if (rings.size() == 1) {
                polygons.add(new GeoJSONPolygon(exteriorRing));
            } else {
                Coordinate[][] holes = rings.subList(1, rings.size()).toArray(new Coordinate[0][]);
                polygons.add(new GeoJSONPolygon(exteriorRing, holes));
            }
        }
        return polygons;
    }

    @Override
    public String toString() {
        int totalRings = coordinates.stream().mapToInt(List::size).sum();
        return "GeoJSONMultiPolygon{" +
            "polygons=" + coordinates.size() +
            ", totalRings=" + totalRings +
            '}';
    }
}

