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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;

import static org.openremote.model.geo.GeoJSONPoint.TYPE;

@JsonTypeName(TYPE)
public class GeoJSONPoint extends GeoJSONGeometry {

    public static final String TYPE = "Point";
    @JsonProperty
    protected Position coordinates;

    @JsonCreator
    public GeoJSONPoint(@JsonProperty("coordinates") Position coordinates) {
        super(TYPE);
        this.coordinates = coordinates;
    }

    public GeoJSONPoint(double x, double y) {
        this(new Position(x, y));
    }

    public GeoJSONPoint(double x, double y, double z) {
        this(new Position(x, y, z));
    }

    public Position getCoordinates() {
        return coordinates;
    }

    public double getX() {
        return coordinates.getX();
    }

    public double getY() {
        return coordinates.getY();
    }

    public Double getZ() {
        return coordinates.getZ();
    }

    public boolean hasZ() {
        return coordinates.hasZ();
    }

    @Override
    public ObjectValue toValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("type", type);
        ArrayValue coords = Values.createArray();
        coords.add(Values.create(getX()));
        coords.add(Values.create(getY()));
        if (hasZ()) {
            coords.add(Values.create(getZ()));
        }
        objectValue.put("coordinates", coords);
        return objectValue;
    }

    public static Optional<GeoJSONPoint> fromValue(Value value) {
        return Values.getObject(value)
            .map(obj -> {
                String type = obj.getString("type").orElse(null);
                if (!TYPE.equalsIgnoreCase(type)) {
                    return null;
                }

                ArrayValue coords = obj.getArray("coordinates").orElse(null);
                if (coords == null || coords.length() < 2 || coords.length() > 3) {
                    return null;
                }

                Double x = coords.getNumber(0).orElse(null);
                Double y = coords.getNumber(1).orElse(null);
                Double z = coords.length() == 3 ? coords.getNumber(3).orElse(null) : null;

                if (x == null || y == null) {
                    return null;
                }
                if (z == null) {
                    return new GeoJSONPoint(new Position(x, y));
                } else {
                    return new GeoJSONPoint(new Position(x, y, z));
                }
            });
    }
}
