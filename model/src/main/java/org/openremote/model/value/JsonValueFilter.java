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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.util.TextUtil;

import java.util.Arrays;
import java.util.List;

import static org.openremote.model.value.RegexValueFilter.NAME;

@JsonTypeName(NAME)
public class JsonValueFilter extends ValueFilter<ObjectValue> {

    public static final String NAME = "json";

    @JsonProperty
    protected List<String> path;

    @JsonCreator
    public JsonValueFilter(@JsonProperty("path") List<String> path) {
        this.path = path;
    }

    public JsonValueFilter(String... path) {
        this(Arrays.asList(path));
    }

    @Override
    public Class<ObjectValue> getMessageType() {
        return ObjectValue.class;
    }

    @Override
    public Value process(ObjectValue value) {
        if (value == null || path == null || path.isEmpty()) {
            return null;
        }

        Value currentValue = value;
        for (int i = 0; i < path.size(); i++) {
            String pathSegment = path.get(i);
            if (currentValue == null || TextUtil.isNullOrEmpty(pathSegment)) {
                return null;
            }

            if (TextUtil.INTEGER_POSITIVE_VALIDATOR.test(pathSegment)) {
                // Check value is an array
                if (currentValue.getType() != ValueType.ARRAY) {
                    // Integer key requires an array value
                    return null;
                }
                currentValue = ((ArrayValue)currentValue).get(Integer.parseInt(pathSegment)).orElse(null);
            } else {
                if (currentValue.getType() != ValueType.OBJECT) {
                    return null;
                }
                currentValue = ((ObjectValue)currentValue).get(pathSegment).orElse(null);
            }
        }

        return currentValue;
    }
}
