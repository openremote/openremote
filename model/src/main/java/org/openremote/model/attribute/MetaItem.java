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
package org.openremote.model.attribute;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.openremote.model.value.AbstractNameValueHolder;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;
import java.util.Objects;

/**
 * A named value whose name must match the name of a {@link MetaItemDescriptor} and whose value must match the value
 * type of the {@link MetaItemDescriptor}.
 */
@JsonSerialize(using = MetaItem.MetaItemSerializer.class)
public class MetaItem<T> extends AbstractNameValueHolder<T> {

    /**
     * Serialise the meta item as just the value to make it less verbose
     */
    @SuppressWarnings("rawtypes")
    public static class MetaItemSerializer extends StdSerializer<MetaItem> {

        protected MetaItemSerializer() {
            super(MetaItem.class);
        }

        @Override
        public void serialize(MetaItem value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeObject(value.value);
        }
    }

    MetaItem() {
    }

    @SuppressWarnings("unchecked")
    public MetaItem(MetaItemDescriptor<T> metaDescriptor) {
        // If it's a boolean meta descriptor assume the caller wants it to be true as a default
        this(metaDescriptor, (T) (metaDescriptor.getType().getType() != Boolean.class ? null : true));
    }

    public MetaItem(MetaItemDescriptor<T> metaDescriptor, T value) {
        super(metaDescriptor.getName(), metaDescriptor.getType(), value);
    }

    // For JPA/Hydrators
    protected void setNameInternal(String name) {
        this.name = name;
    }

    protected void setTypeInternal(ValueDescriptor<T> type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
