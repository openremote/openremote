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
package org.openremote.manager.client.interop.jackson;

import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonDeserializer;
import com.github.nmorel.gwtjackson.client.JsonDeserializerParameters;
import com.github.nmorel.gwtjackson.client.stream.JsonReader;
import elemental.json.Json;
import elemental.json.JsonValue;

public class ElementalJsonDeserializer extends JsonDeserializer<JsonValue> {

    @Override
    protected JsonValue doDeserialize(JsonReader reader, JsonDeserializationContext ctx, JsonDeserializerParameters params) {
        if (reader.hasNext()) {
            String nextValue = reader.nextValue();
            JsonValue value = Json.instance().parse(nextValue);
            value = makeCastableToJavaScriptObject(value);
            return value;
        }
        return null;
    }

    static native JsonValue makeCastableToJavaScriptObject(JsonValue value) /*-{
        // If it's a primitive, wrap in Object
        if (value === null || typeof value == 'object') {
            return value;
        }
        return Object(value);
    }-*/;
}
