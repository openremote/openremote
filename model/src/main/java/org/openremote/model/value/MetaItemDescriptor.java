/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.value;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.ValueUtil;

/**
 * Describes a {@link MetaItem} that can be added to an {@link Attribute}; the {@link #getName()} must match the {@link
 * MetaItem#getName()}, it also indicates what the {@link ValueDescriptor} is for the {@link MetaItem}. The {@link
 * MetaItemDescriptor} applies to the {@link Asset} type it is associated with and all subtypes of this type (i.e. a
 * {@link MetaItemDescriptor} associated with the base {@link Asset} type will be available to all {@link Asset} types
 * (e.g. {@link MetaItemType#READ_ONLY} can be applied to any {@link Asset}'s {@link Attribute}).
 * <p>
 * {@link MetaItemDescriptor#getName} must be globally unique within the context of the manager it is registered with.
 */
public class MetaItemDescriptor<T> extends AbstractNameValueDescriptorHolder<T> {

    /**
     * This class handles serialising {@link MetaItemDescriptor}s as strings
     */
    public static class MetaItemDescriptorStringConverter extends StdConverter<MetaItemDescriptor<?>, String> {

        @Override
        public String convert(MetaItemDescriptor<?> value) {
            return value.getName();
        }
    }

    /**
     * This class handles deserialising meta item descriptor names to {@link MetaItemDescriptor}s
     */
    public static class StringMetaItemDescriptorConverter extends StdConverter<String, MetaItemDescriptor<?>> {

        @Override
        public MetaItemDescriptor<?> convert(String value) {
            return ValueUtil.getMetaItemDescriptor(value).orElse(MetaItemDescriptor.UNKNOWN);
        }
    }

    public static final MetaItemDescriptor<Object> UNKNOWN = new MetaItemDescriptor<>("unkown", ValueDescriptor.UNKNOWN);

    MetaItemDescriptor() {}

    public MetaItemDescriptor(String name, ValueDescriptor<T> valueDescriptor, ValueConstraint...constraints) {
        super(name, valueDescriptor, constraints);
    }

    public MetaItemDescriptor<T> setFormat(ValueFormat format) {
        this.format = format;
        return this;
    }

    public MetaItemDescriptor<T> setConstraints(ValueConstraint...constraints) {
        this.constraints = constraints;
        return this;
    }

    public MetaItemDescriptor<T> setUnits(String...units) {
        this.units = units;
        return this;
    }

    @Override
    public String toString() {
        return MetaItemDescriptor.class.getSimpleName() + "{" +
            super.toString() +
            '}';
    }
}
