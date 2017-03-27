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

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

import java.util.NoSuchElementException;

/**
 * Convenience overlay API for {@link JsonObject}.
 * <p>
 * Modifies the given or an empty object.
 */
public abstract class Attribute<CHILD extends Attribute<CHILD>> extends AbstractValueTimestampHolder<CHILD> {

    /**
     * Attribute names should be very simple, as we use them in SQL path expressions, etc. and must manually escape.
     */
    public static final String ATTRIBUTE_NAME_PATTERN = "^\\w+$";

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

    public Attribute(CHILD attribute) {
        this(attribute.getName(), attribute.getJsonObject());
    }

    @Override
    protected boolean isValidValue(JsonValue value) {
        return getType() != null
            ? getType().isValid(value)
            : super.isValidValue(value);
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

    @SuppressWarnings("unchecked")
    public CHILD setType(AttributeType type) {
        if (type != null) {
            jsonObject.put("type", Json.create(type.getValue()));
        } else if (jsonObject.hasKey("type")) {
            jsonObject.remove("type");
        }
        return (CHILD)this;
    }

    public boolean hasMeta() {
        return jsonObject.hasKey("meta");
    }

    public Meta getMeta() {
        return hasMeta() ? new Meta(jsonObject.getArray("meta")) : null;
    }

    @SuppressWarnings("unchecked")
    public CHILD setMeta(Meta meta) {
        if (meta != null) {
            jsonObject.put("meta", meta.getJsonArray());
        } else if (jsonObject.hasKey("meta")) {
            jsonObject.remove("meta");
        }
        return (CHILD) this;
    }

    public boolean hasMetaItem(String name) {
        return hasMeta() && getMeta().contains(name);
    }

    public MetaItem firstMetaItem(String name) {
        return hasMetaItem(name) ? getMeta().first(name) : null;
    }

    public MetaItem firstMetaItemOrThrow(String name) throws NoSuchElementException {
        if (!hasMetaItem(name))
            throw new NoSuchElementException("Missing item: " + name);
        return firstMetaItem(name);
    }

    public boolean isValid() {
        return getName() != null && getName().length() > 0 && getType() != null;
    }

    public abstract CHILD copy();
}
