/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.model.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.openremote.model.value.impl.ColourRGB;

import java.io.IOException;

public class ColourRGBDeserializerJackson2 extends StdDeserializer<ColourRGB> {

    public ColourRGBDeserializerJackson2() {
        super(ColourRGB.class);
    }

    @Override
    public ColourRGB deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode value = parser.getCodec().readTree(parser);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return ColourRGB.fromHexString(value.asText());
        }
        if (value.isObject()) {
            return new ColourRGB(
                value.path("r").asInt(),
                value.path("g").asInt(),
                value.path("b").asInt()
            );
        }
        if (value.isArray() && value.size() == 3) {
            return new ColourRGB(
                value.get(0).asInt(),
                value.get(1).asInt(),
                value.get(2).asInt()
            );
        }
        throw JsonMappingException.from(parser, "ColourRGB must be a hex string, object, or RGB array");
    }
}
