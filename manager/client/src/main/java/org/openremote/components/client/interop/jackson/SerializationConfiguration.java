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
package org.openremote.components.client.interop.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.github.nmorel.gwtjackson.client.AbstractConfiguration;
import org.openremote.model.ValidationFailure;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.value.*;

/**
 * Restart super-dev server after modifying this class!
 */
public class SerializationConfiguration extends AbstractConfiguration {

    @Override
    protected void configure() {

        fieldVisibility(JsonAutoDetect.Visibility.ANY);
        getterVisibility(JsonAutoDetect.Visibility.NONE);
        setterVisibility(JsonAutoDetect.Visibility.NONE);
        isGetterVisibility(JsonAutoDetect.Visibility.NONE);
        creatorVisibility(JsonAutoDetect.Visibility.ANY);
        type(ValidationFailure.Reason.class).deserializer(ValidationFailureReasonDeserializer.class);
        type(MetaItemDescriptor.class).deserializer(MetaItemDescriptorDeserializer.class);
        type(Value.class).serializer(ModelValueSerializer.class).deserializer(ModelValueDeserializer.class);
        type(ObjectValue.class).serializer(ModelValueSerializer.class).deserializer(ModelValueDeserializer.class);
        type(ArrayValue.class).serializer(ModelValueSerializer.class).deserializer(ModelValueDeserializer.class);
        type(NumberValue.class).serializer(ModelValueSerializer.class).deserializer(ModelValueDeserializer.class);
        type(StringValue.class).serializer(ModelValueSerializer.class).deserializer(ModelValueDeserializer.class);
        type(BooleanValue.class).serializer(ModelValueSerializer.class).deserializer(ModelValueDeserializer.class);
    }
}
