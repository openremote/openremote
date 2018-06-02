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
package org.openremote.model.geo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

@JsonFormat
@JsonSerialize(using = Position.PositionSerializer.class)
@JsonDeserialize(using = Position.PositionDeserializer.class)
public class Position {

    public static class PositionSerializer extends JsonSerializer<Position> {

        @Override
        public void serialize(Position value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeArray(value.values, 0, value.values.length);
        }
    }

    public static class PositionDeserializer extends JsonDeserializer<Position> {

        @Override
        public Position deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            double[] values = p.getCodec().readValue(p, double[].class);
            return new Position(values[0], values[1]);
        }
    }

    protected double[] values;

    public Position(double x, double y) {
        values = new double[] {x, y};
    }

    public Position(double x, double y, double z) {
        values = new double[] {x, y, z};
    }

    public double getX() {
        return values[0];
    }

    public double getY() {
        return values[1];
    }

    public Double getZ() {
        return values.length == 3 ? values[2] : null;
    }

    public boolean hasZ() {
        return values.length == 3;
    }
}
