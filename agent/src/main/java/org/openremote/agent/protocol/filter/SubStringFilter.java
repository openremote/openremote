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
package org.openremote.agent.protocol.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import static org.openremote.agent.protocol.filter.SubStringFilter.NAME;

@JsonTypeName(NAME)
public class SubStringFilter extends MessageFilter<StringValue> {

    public static final String NAME = "substring";

    @JsonProperty
    protected int beginIndex;
    @JsonProperty
    protected Integer endIndex;

    @JsonCreator
    public SubStringFilter(@JsonProperty("beginIndex") int beginIndex, @JsonProperty("endIndex") Integer endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public SubStringFilter(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    @Override
    public Class<StringValue> getMessageType() {
        return StringValue.class;
    }

    @Override
    public Value process(StringValue value) {
        if (value == null) {
            return null;
        }

        String result = null;

        try {
            if (endIndex != null) {
                result = value.getString().substring(beginIndex, endIndex);
            } else {
                result = value.getString().substring(beginIndex);
            }
        } catch (IndexOutOfBoundsException ignored) {}

        return result == null ? null : Values.create(result);
    }
}
