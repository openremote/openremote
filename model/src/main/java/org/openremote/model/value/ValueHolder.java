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
package org.openremote.model.value;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Indicates that the implementing class provides a value of &lt;T&gt; the value should be
 * immutable.
 */
public interface ValueHolder<T> {

  @JsonSerialize(converter = ValueDescriptor.NameHolderToStringConverter.class)
  ValueDescriptor<T> getType();

  Class<?> getTypeClass();

  @JsonProperty
  Optional<T> getValue();

  /** Provides basic type casting/coercion useful for unknown values */
  <U> Optional<U> getValue(Class<U> valueType);
}
