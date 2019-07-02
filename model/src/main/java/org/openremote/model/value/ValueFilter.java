/*
 * Copyright 2017, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for a filter that can be applied to messages of type &lt;T&gt; the filter can return a different
 * {@link ValueType} to the supplied message (i.e. value conversion as well as filtering). Filters can be chained and
 * filters should be applied using the following logic:
 * <ul>
 * <li>If message is null then do not pass through filter</li>
 * <li>If message type doesn't match the filter's message type then treat as if filter returned null</li>
 * <li>If filter throws an exception then handle and treat as if filter returned null</li>
 * </ul>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = RegexValueFilter.NAME, value = RegexValueFilter.class),
    @JsonSubTypes.Type(name = SubStringValueFilter.NAME, value = SubStringValueFilter.class),
    @JsonSubTypes.Type(name = JsonValueFilter.NAME, value = JsonValueFilter.class)
})
public abstract class ValueFilter<T extends Value> {

    /**
     * Get the message type
     */
    public abstract Class<T> getMessageType();

    /**
     * Apply this filter to the supplied message
     */
    public abstract Value process(T message);

}
