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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class Attribute extends AbstractAttribute {

    @JsonIgnore
    final protected JsonObject jsonObject;

    public Attribute(String name, JsonObject jsonObject) {
        super(name);
        this.jsonObject = jsonObject;
    }

    public Attribute(@JsonProperty("name") String name, @JsonProperty("type") AttributeType type, @JsonProperty("value") JsonValue value) {
        super(name);
        jsonObject = elemental.json.Json.createObject();
        setType(type);
        setValue(value);
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    @JsonProperty("value")
    @Override
    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    public JsonObject getValueAsObject() {
        return jsonObject.hasKey("value") ? jsonObject.getObject("value") : null;
    }

    @Override
    public Attribute setValue(JsonValue value) {
        jsonObject.put("value", value);
        return this;
    }

    @JsonProperty("type")
    public AttributeType getType() {
        String keyValue = jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
        return keyValue != null ? AttributeType.fromName(keyValue) : null;
    }

    public Attribute setType(AttributeType type) {
        jsonObject.put("type", type.getName());
        return this;
    }

    public Metadata getMetadata() {
        return jsonObject.hasKey("metadata") ? new Metadata(jsonObject.getObject("metadata")) : null;
    }

    public Attribute setMetadata(Metadata metadata) {
        jsonObject.put("metadata", metadata.getJsonObject());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        return jsonObject.equals(attribute.jsonObject);
    }

    @Override
    public int hashCode() {
        return jsonObject.hashCode();
    }

    @Override
    public String toString() {
        return getName() + " => " + jsonObject.toJson();
    }
}
