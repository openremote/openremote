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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.openremote.model.attribute.MetaItem;

import java.io.IOException;

public class MetaItemSerializerJackson2 extends StdSerializer<MetaItem<?>> {

    public MetaItemSerializerJackson2() {
        super((Class<MetaItem<?>>) (Class<?>) MetaItem.class);
    }

    @Override
    public void serialize(MetaItem<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Object rawValue = value.getValue().orElse(null);
        if (rawValue == null) {
            gen.writeNull();
        } else {
            gen.writeObject(rawValue);
        }
    }
}
