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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import org.openremote.model.util.ValueUtil;

import java.util.Optional;

@JsonSchemaTitle("Substring")
@JsonTypeName(SubStringValueFilter.NAME)
@JsonClassDescription("Returns the substring beginning at the specified index (inclusive) and ending at the optional endIndex (exclusive); if endIndex is not supplied then" +
    " the remainder of the string is returned; negative values can be used to indicate a backwards count from the length of the string e.g. -1 means length-1")
public class SubStringValueFilter extends ValueFilter {

    public static final String NAME = "substring";

    public int beginIndex;
    public Integer endIndex;

    @JsonCreator
    public SubStringValueFilter(@JsonProperty("beginIndex") int beginIndex, @JsonProperty("endIndex") Integer endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public SubStringValueFilter(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    @Override
    public Object filter(Object value) {
        Optional<String> valueStr = ValueUtil.getValue(value, String.class, true);
        if (!valueStr.isPresent()) {
            return null;
        }

        String result = null;

        if (beginIndex < 0) {
            beginIndex = valueStr.get().length() + 1 + beginIndex;
            beginIndex = Math.max(0, beginIndex);
        }

        try {
            if (endIndex != null) {
                if (endIndex < 0) {
                    endIndex = valueStr.get().length() + endIndex;
                    endIndex = Math.max(beginIndex, endIndex);
                }
                result = valueStr.get().substring(beginIndex, endIndex);
            } else {
                result = valueStr.get().substring(beginIndex);
            }
        } catch (IndexOutOfBoundsException ignored) {}

        return result;
    }
}
