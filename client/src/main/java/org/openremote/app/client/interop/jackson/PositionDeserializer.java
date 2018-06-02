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
package org.openremote.app.client.interop.jackson;

import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonDeserializer;
import com.github.nmorel.gwtjackson.client.JsonDeserializerParameters;
import com.github.nmorel.gwtjackson.client.stream.JsonReader;
import com.github.nmorel.gwtjackson.client.stream.JsonToken;
import org.openremote.model.geo.Position;

public class PositionDeserializer extends JsonDeserializer<Position> {

    @Override
    protected Position doDeserialize(JsonReader jsonReader, JsonDeserializationContext jsonDeserializationContext, JsonDeserializerParameters jsonDeserializerParameters) {

        if ( JsonToken.BEGIN_ARRAY == jsonReader.peek() ) {
            jsonReader.beginArray();
            Double[] values = new Double[3];
            int index = 0;
            while ( JsonToken.END_ARRAY != jsonReader.peek() && index < 3 ) {
                values[index++] = jsonReader.nextDouble();
            }
            jsonReader.endArray();

            return new Position(values[0], values[1], values[2]);
        }

        return null;
    }
}
