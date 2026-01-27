/*
 * Copyright 2016, OpenRemote Inc.
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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The desired or current or past state of an {@link AttributeRef}.
 *
 * <p><code>null</code> is a valid {@link #value}.
 */
public class AttributeState implements Serializable {

  @JsonProperty protected AttributeRef ref;
  protected Object value;

  AttributeState() {}

  public AttributeState(String assetId, Attribute<?> attribute) {
    this(assetId, attribute.getName(), attribute.getValue().orElse(null));
  }

  public AttributeState(String assetId, String attributeName, Object value) {
    this(new AttributeRef(assetId, attributeName), value);
  }

  public AttributeState(AttributeRef ref, Object value) {
    this.ref = Objects.requireNonNull(ref);
    this.value = value;
  }

  /** Sets the {@link #value} to <code>null</code>. */
  public AttributeState(AttributeRef ref) {
    this(ref, null);
  }

  public AttributeRef getRef() {
    return ref;
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> getValue() {
    return Optional.ofNullable(value).map(v -> (T) v);
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public String toString() {
    String valueStr = Objects.toString(value);
    return getClass().getSimpleName()
        + "{"
        + "ref="
        + ref
        + ", value="
        + (valueStr.length() > 100 ? valueStr.substring(0, 100) + "..." : valueStr)
        + '}';
  }
}
