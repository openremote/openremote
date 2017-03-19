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
import com.github.nmorel.gwtjackson.client.stream.JsonToken;
import com.google.gwt.core.client.JsArrayInteger;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * This can only (safely) deserialize JSON Objects and Arrays, not primitives.
 */
public class ElementalJsonDeserializer extends JsonDeserializer<JsonValue> {

    @Override
    protected JsonValue doDeserialize(com.github.nmorel.gwtjackson.client.stream.JsonReader reader, JsonDeserializationContext ctx, JsonDeserializerParameters params) {
        if (reader.hasNext()) {
            String nextValue = reader.nextValue();
            JsonValue value = Json.instance().parse(nextValue);
            return value;
        }
        return null;
    }
}
