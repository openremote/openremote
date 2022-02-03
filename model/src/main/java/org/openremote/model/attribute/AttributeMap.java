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
package org.openremote.model.attribute;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class AttributeMap extends NamedMap<Attribute<?>> {

    public AttributeMap() {
    }

    public AttributeMap(Collection<? extends Attribute<?>> c) {
        super(c);
    }

    public AttributeMap(Map<? extends String, ? extends Attribute<?>> map) {
        super(map);
    }

    // This works around the crappy type system and avoids the need for a type witness
    public <S> Optional<Attribute<S>> get(AttributeDescriptor<S> attributeDescriptor) {
        return super.get(attributeDescriptor);
    }

    public <U extends AttributeDescriptor<?>> void remove(U nameHolder) {
        super.remove(nameHolder);
    }

    public <S> Attribute<S> getOrCreate(AttributeDescriptor<S> attributeDescriptor) {
        return get(attributeDescriptor).orElseGet(() -> {
            Attribute<S> attr = new Attribute<>(attributeDescriptor);
            putSilent(attr);
            return attr;
        });
    }

    @SuppressWarnings("unchecked")
    public <S> Attribute<S> getOrCreate(String attributeName, ValueDescriptor<S> valueDescriptor) {
        return (Attribute<S>) get(attributeName).orElseGet(() -> {
            Attribute<S> attr = new Attribute<>(attributeName, valueDescriptor);
            putSilent(attr);
            return attr;
        });
    }

    public <T> void setValue(AttributeDescriptor<T> descriptor, T value) {
        getOrCreate(descriptor).setValue(value);
    }

    /**
     * Need to declare equals here as {@link com.vladmihalcea.hibernate.type.json.internal.JsonTypeDescriptor} uses
     * {@link Class#getDeclaredMethod} to find it...
     */
    @Override
    public boolean equals(@Nullable Object object) {
        return super.equals(object);
    }
}
