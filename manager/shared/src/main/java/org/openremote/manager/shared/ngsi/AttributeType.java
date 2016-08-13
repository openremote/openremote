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
package org.openremote.manager.shared.ngsi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("type")
    private String name;
    private JsonType jsonType;

    private AttributeType(@JsonProperty("name") String name, @JsonProperty("jsonType") JsonType jsonType) {
        this.name = name;
        this.jsonType = jsonType;
    }

    public String getName() {
        return name;
    }

    public JsonType getJsonType() {
        return jsonType;
    }

    @JsonCreator
    public static AttributeType fromString(@JsonProperty("type") String str) {
        return fromName(str);
    }

    public static AttributeType fromName(String name) {
        AttributeType[] vals = AttributeType.values();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].getName().equalsIgnoreCase(name))
                return vals[i];
        }
        return null;
    }
}
