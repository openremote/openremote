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
package org.openremote.app.client.interop.jackson;

import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonDeserializer;
import com.github.nmorel.gwtjackson.client.JsonDeserializerParameters;
import com.github.nmorel.gwtjackson.client.stream.JsonReader;
import com.google.gwt.core.client.GWT;
import org.openremote.app.client.assets.attributes.AttributeDescriptorMapper;
import org.openremote.app.client.assets.attributes.AttributeValueDescriptorMapper;
import org.openremote.model.attribute.AttributeDescriptor;
import org.openremote.model.attribute.AttributeValueDescriptor;

public class AttributeDescriptorDeserializer extends JsonDeserializer<AttributeDescriptor> {

    private static final AttributeDescriptorDeserializer INSTANCE = new AttributeDescriptorDeserializer();
    private static final AttributeDescriptorMapper MAPPER = GWT.create(AttributeDescriptorMapper.class);

    public static AttributeDescriptorDeserializer getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeDescriptor doDeserialize(JsonReader reader, JsonDeserializationContext ctx, JsonDeserializerParameters params) {
        // Read entire object in
        return MAPPER.read(reader.nextValue());
    }
}
