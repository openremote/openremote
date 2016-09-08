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

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class Attribute {

    final protected String name;
    final protected JsonObject jsonObject;

    public Attribute(String name) {
        this(name, Json.createObject());
    }

    public Attribute(String name, AttributeType type) {
        this(name);
        setType(type);
    }

    public Attribute(String name, JsonObject jsonObject) {
        this.name = name;
        this.jsonObject = jsonObject;
    }

    public Attribute(String name, AttributeType type, JsonValue value) {
        this(name, elemental.json.Json.createObject());
        setType(type);
        setValue(value);
    }

    public String getName() {
        return name;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public AttributeType getType() {
        String typeName = jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
        return typeName != null ? AttributeType.fromValue(typeName) : null;
    }

    public Attribute setType(AttributeType type) {
        if (type != null) {
            jsonObject.put("type", Json.create(type.getValue()));
        } else if (jsonObject.hasKey("type")) {
            jsonObject.remove("type");
        }
        return this;
    }

    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    public String getValueAsString() {
        return jsonObject.hasKey("value") ? jsonObject.get("value").asString() : null;
    }

    public JsonObject getValueAsObject() {
        return jsonObject.hasKey("value") ? jsonObject.getObject("value") : null;
    }

    public Attribute setValue(JsonValue value) {
        jsonObject.put("value", value);
        return this;
    }

    public Attribute setValue(String value) {
        return setValue(Json.create(value));
    }

    public Attribute setValue(Integer value) {
        return setValue(Json.create(value));
    }

    public Attribute setValue(double value) {
        return setValue(Json.create(value));
    }

    public Attribute setValue(boolean value) {
        return setValue(Json.create(value));
    }

    public boolean hasMetadata() {
        return jsonObject.hasKey("metadata");
    }

    public boolean hasMetadataElement(String name) {
        return hasMetadata() && getMetadata().hasElement(name);
    }

    public Metadata getMetadata() {
        return hasMetadata() ? new Metadata(jsonObject.getObject("metadata")) : null;
    }

    public Attribute setMetadata(Metadata metadata) {
        if (metadata != null) {
            jsonObject.put("metadata", metadata.getJsonObject());
        } else if (jsonObject.hasKey("metadata")) {
            jsonObject.remove("metadata");
        }
        return this;
    }

    public Attribute copy() {
        return new Attribute(getName(), Json.parse(getJsonObject().toJson()));
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }

}
