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
package org.openremote.manager.shared.attribute;

import elemental.json.JsonType;

public enum AttributeType {

    OBJECT("Object", JsonType.OBJECT),
    STRING("String", JsonType.STRING),
    INTEGER("Integer", JsonType.NUMBER),
    FLOAT("Float", JsonType.NUMBER),
    BOOLEAN("Boolean", JsonType.BOOLEAN),
    OBJECT_ARRAY("Object[]", JsonType.ARRAY),
    STRING_ARRAY("String[]", JsonType.ARRAY),
    INTEGER_ARRAY("Integer[]", JsonType.ARRAY),
    FLOAT_ARRAY("Float[]", JsonType.ARRAY),
    BOOLEAN_ARRAY("Boolean[]", JsonType.ARRAY),
    DATETIME("DateTime", JsonType.STRING);

    private String name;
    private JsonType jsonType;

    AttributeType(String name, JsonType jsonType) {
        this.name = name;
        this.jsonType = jsonType;
    }

    public String getName() {
        return name;
    }

    public JsonType getJsonType() {
        return jsonType;
    }

    public static AttributeType fromName(String name) {
        AttributeType[] values = AttributeType.values();
        for (AttributeType value : values) {
            if (value.getName().equalsIgnoreCase(name))
                return value;
        }
        return null;
    }
}
