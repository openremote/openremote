/*
 * Copyright 2020, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.attribute;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckForNull;

import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

public class AttributeMap extends NamedMap<Attribute<?>> {

  public AttributeMap() {}

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
    return get(attributeDescriptor)
        .orElseGet(
            () -> {
              Attribute<S> attr = new Attribute<>(attributeDescriptor);
              put(attr);
              return attr;
            });
  }

  @SuppressWarnings("unchecked")
  public <S> Attribute<S> getOrCreate(String attributeName, ValueDescriptor<S> valueDescriptor) {
    return (Attribute<S>)
        get(attributeName)
            .orElseGet(
                () -> {
                  Attribute<S> attr = new Attribute<>(attributeName, valueDescriptor);
                  put(attr);
                  return attr;
                });
  }

  public <T> void setValue(AttributeDescriptor<T> descriptor, T value) {
    getOrCreate(descriptor).setValue(value);
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    return super.equals(object);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
