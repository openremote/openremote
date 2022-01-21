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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Optional;

/**
 * Indicates that the implementing class provides a value of &lt;T&gt; the value should be immutable.
 */
public interface ValueHolder<T> {

    @JsonProperty
    @JsonSerialize(converter = ValueDescriptor.ValueDescriptorStringConverter.class)
    @JsonDeserialize(converter = ValueDescriptor.StringValueDescriptorConverter.class)
    ValueDescriptor<T> getType();

    @JsonProperty
    Optional<T> getValue();

    /**
     * Provides basic type casting/coercion useful for unknown values
     */
    <U> Optional<U> getValueAs(Class<U> valueType);
}
