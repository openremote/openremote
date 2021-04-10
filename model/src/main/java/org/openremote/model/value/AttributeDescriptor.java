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

import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;

import java.util.Arrays;
import java.util.Collection;

/**
 * Describes an {@link Attribute} that can be added to an {@link Asset}; the {@link #getName()} must match the {@link
 * Attribute#getName()}, it also indicates what the {@link ValueDescriptor} is for the {@link Attribute} and optionally
 * provides default {@link MetaItem}s that should be added to new instances of the {@link Attribute}. The {@link
 * AttributeDescriptor} applies to the {@link Asset} type it is associated with and all subtypes of this type (i.e. an
 * {@link AttributeDescriptor} associated with the base {@link Asset} type will apply to all {@link Asset} types (e.g.
 * {@link Asset#LOCATION})
 * <p>
 * {@link AttributeDescriptor#getName} must be unique within the {@link Asset} type hierarchy to which they are
 * associated e.g. an {@link AttributeDescriptor} with the name 'location' cannot be added to any {@link Asset} type as
 * it is already associated with the {@link Asset} class itself see {@link Asset#LOCATION})
 */
public class AttributeDescriptor<T> extends AbstractNameValueDescriptorHolder<T> implements MetaHolder {

    protected MetaMap meta;
    protected boolean optional;

    AttributeDescriptor() {}

    public AttributeDescriptor(String name, ValueDescriptor<T> valueDescriptor) {
        this(name, valueDescriptor, (MetaMap) null);
    }

    public AttributeDescriptor(String name, ValueDescriptor<T> valueDescriptor, MetaItem<?>... meta) {
        this(name, valueDescriptor, Arrays.asList(meta));
    }

    public AttributeDescriptor(String name, ValueDescriptor<T> valueDescriptor, Collection<MetaItem<?>> meta) {
        this(name, valueDescriptor, meta instanceof MetaMap ? (MetaMap) meta : new MetaMap(meta));
    }

    public AttributeDescriptor(String name, ValueDescriptor<T> valueDescriptor, MetaMap meta) {
        super(name, valueDescriptor);
        this.meta = meta;
    }

    @Override
    public MetaMap getMeta() {
        return meta;
    }

    public AttributeDescriptor<T> withMeta(MetaMap meta) {
        AttributeDescriptor<T> newDescriptor = new AttributeDescriptor<T>(this.name, this.type, meta);
        newDescriptor.format = format;
        newDescriptor.constraints = constraints;
        newDescriptor.units = units;
        newDescriptor.optional = optional;
        return newDescriptor;
    }

    public boolean isOptional() {
        return optional;
    }

    public AttributeDescriptor<T> withOptional(boolean optional) {
        AttributeDescriptor<T> newDescriptor = new AttributeDescriptor<T>(name, type, meta);
        newDescriptor.format = format;
        newDescriptor.constraints = constraints;
        newDescriptor.units = units;
        newDescriptor.optional = optional;
        return newDescriptor;
    }

    public AttributeDescriptor<T> withFormat(ValueFormat format) {
        AttributeDescriptor<T> newDescriptor = new AttributeDescriptor<T>(name, type, meta);
        newDescriptor.format = format;
        newDescriptor.constraints = constraints;
        newDescriptor.units = units;
        newDescriptor.optional = optional;
        return newDescriptor;
    }

    public AttributeDescriptor<T> withConstraints(ValueConstraint...constraints) {
        AttributeDescriptor<T> newDescriptor = new AttributeDescriptor<T>(name, type, meta);
        newDescriptor.format = format;
        newDescriptor.constraints = constraints;
        newDescriptor.units = units;
        newDescriptor.optional = optional;
        return newDescriptor;
    }

    public AttributeDescriptor<T> withUnits(String...units) {
        AttributeDescriptor<T> newDescriptor = new AttributeDescriptor<T>(name, type, meta);
        newDescriptor.format = format;
        newDescriptor.constraints = constraints;
        newDescriptor.units = units;
        newDescriptor.optional = optional;
        return newDescriptor;
    }

    public void updateConstraints(ValueConstraint...valueConstraints) {
        constraints = valueConstraints;
    }

    @Override
    public String toString() {
        return AttributeDescriptor.class.getSimpleName() + "{" +
            super.toString() +
            ", optional=" + optional +
            "}";
    }
}
