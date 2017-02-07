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

import elemental.json.*;

import java.util.NoSuchElementException;

public class Attribute extends AbstractValueHolder<Attribute> {

    protected String name;

    public Attribute() {
        super(Json.createObject());
    }

    public Attribute(String name) {
        this(name, Json.createObject());
    }

    public Attribute(String name, AttributeType type) {
        this(name);
        setType(type);
    }

    public Attribute(String name, JsonObject jsonObject) {
        super(jsonObject);
        this.name = name;
    }

    public Attribute(String name, AttributeType type, JsonValue value) {
        this(name, Json.createObject());
        setType(type);
        setValue(value);
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean hasMetadata() {
        return jsonObject.hasKey("metadata");
    }

    public Metadata getMetadata() {
        return hasMetadata() ? new Metadata(jsonObject.getArray("metadata")) : null;
    }

    public Attribute setMetadata(Metadata metadata) {
        if (metadata != null) {
            jsonObject.put("metadata", metadata.getJsonArray());
        } else if (jsonObject.hasKey("metadata")) {
            jsonObject.remove("metadata");
        }
        return this;
    }

    public boolean hasMetaItem(String name) {
        return hasMetadata() && getMetadata().contains(name);
    }

    public MetadataItem firstMetaItem(String name) {
        return hasMetaItem(name) ? getMetadata().first(name) : null;
    }

    public MetadataItem firstMetaItemOrThrow(String name) throws NoSuchElementException {
        if (!hasMetaItem(name))
            throw new NoSuchElementException("Missing item: " + name);
        return firstMetaItem(name);
    }

    public Attribute copy() {
        return new Attribute(getName(), Json.parse(getJsonObject().toJson()));
    }

    public boolean isValid() {
        return getName() != null && getName().length() > 0 && getType() != null;
    }
}
