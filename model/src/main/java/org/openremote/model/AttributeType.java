/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model;

import elemental.json.JsonType;

public enum AttributeType {
    STRING("String", JsonType.STRING),
    OBJECT("Object", JsonType.OBJECT),
    INTEGER("Integer", JsonType.NUMBER),
    DECIMAL("Decimal", JsonType.NUMBER),
    BOOLEAN("Boolean", JsonType.BOOLEAN),
    OBJECT_ARRAY("Object[]", JsonType.ARRAY),
    STRING_ARRAY("String[]", JsonType.ARRAY),
    INTEGER_ARRAY("Integer[]", JsonType.ARRAY),
    DECIMAL_ARRAY("Decimal[]", JsonType.ARRAY),
    BOOLEAN_ARRAY("Boolean[]", JsonType.ARRAY);

    private String value;
    private JsonType jsonType;

    AttributeType(String value, JsonType jsonType) {
        this.value = value;
        this.jsonType = jsonType;
    }

    public String getValue() {
        return value;
    }

    public JsonType getJsonType() {
        return jsonType;
    }

    public static AttributeType fromValue(String value) {
        if (value != null && !"".equals(value)) {
            AttributeType[] values = AttributeType.values();
            for (AttributeType v : values) {
                if (v.getValue().equalsIgnoreCase(value))
                    return v;
            }
        }
        return STRING;
    }
}
