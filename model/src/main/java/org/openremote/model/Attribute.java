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
import org.openremote.model.util.AttributeUtil;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Convenience overlay API for {@link JsonObject}.
 */
public abstract class Attribute<CHILD extends Attribute<CHILD>> extends AbstractValueTimestampHolder<CHILD> {

    public static final String TYPE_FIELD_NAME = "type";
    public static final String META_FIELD_NAME = "meta";
    protected String name;

    protected Attribute(String name) {
        this(name, Json.createObject());
    }

    protected Attribute(String name, AttributeType type) {
        super(Json.createObject());
        setName(name);
        setType(type);
    }

    protected Attribute(String name, JsonObject jsonObject) {
        super(jsonObject);
        setName(name);

        // Verify jsonObject is valid
        if (!isValid()) {
            throw new IllegalArgumentException("Supplied JSON object is not valid for this type of attribute: " + jsonObject.toJson());
        }
    }

    protected Attribute(String name, AttributeType type, JsonValue value) {
        this(name, type);
        setValue(value);
    }

    protected Attribute(CHILD attribute) {
        this(attribute.getName(), attribute.getType(), attribute.getJsonObject());
    }

    @Override
    protected boolean isValidValue(JsonValue value) {
        return getType() != null
            ? getType().isValid(value)
            : super.isValidValue(value);
    }

    public void setName(String name) {
        if (!AttributeUtil.nameIsValid(name)) {
            throw new IllegalArgumentException(
                    "Invalid attribute name (must match '" + AttributeUtil.ATTRIBUTE_NAME_PATTERN + "'): " + name
            );
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public AttributeType getType() {
        String typeName = jsonObject.hasKey(TYPE_FIELD_NAME) ? jsonObject.get(TYPE_FIELD_NAME).asString() : null;
        return typeName != null ? AttributeType.fromValue(typeName) : null;
    }

    @SuppressWarnings("unchecked")
    public CHILD setType(AttributeType type) {
        if (type != null) {
            jsonObject.put(TYPE_FIELD_NAME, Json.create(type.getValue()));
        }/* else if (jsonObject.hasKey(TYPE_FIELD_NAME)) {
            jsonObject.remove(TYPE_FIELD_NAME);
        }*/
        return (CHILD)this;
    }

    public boolean hasMeta() {
        return jsonObject.hasKey(META_FIELD_NAME);
    }

    public Meta getMeta() {
        return hasMeta() ? new Meta(jsonObject.getArray(META_FIELD_NAME)) : new Meta();
    }

    @SuppressWarnings("unchecked")
    public CHILD setMeta(Meta meta) {
        if (meta != null) {
            jsonObject.put(META_FIELD_NAME, meta.getJsonArray());
        } else if (jsonObject.hasKey(META_FIELD_NAME)) {
            jsonObject.remove(META_FIELD_NAME);
        }
        return (CHILD) this;
    }

    public boolean hasMetaItem(String name) {
        return hasMeta() && getMeta().contains(name);
    }

    public boolean hasMetaItem(String name, JsonValue value) {
        return hasMeta() && getMeta().contains(name, value);
    }

    public MetaItem firstMetaItem(String name) {
        return hasMetaItem(name) ? getMeta().first(name) : null;
    }

    public MetaItem firstMetaItem(String name, JsonValue value) {
        return hasMeta() ? getMeta().first(name, value) : null;
    }

    public MetaItem firstMetaItemOrThrow(String name) throws NoSuchElementException {
        if (!hasMetaItem(name))
            throw new NoSuchElementException("Missing item: " + name);
        return firstMetaItem(name);
    }

    public MetaItem firstMetaItemOrThrow(String name, JsonValue value) throws NoSuchElementException {
        if (!hasMeta())
            throw new NoSuchElementException("Missing item: " + name);
        MetaItem metaItem = getMeta().first(name, value);
        if (metaItem == null)
            throw new NoSuchElementException("Missing item: " + name);

        return metaItem;
    }

    public List<MetaItem> getMetaItems(String name) {
        if (!hasMeta()) {
            return Collections.emptyList();
        }

        return getMeta().get(name);
    }

    public List<MetaItem> getMetaItems(String name, JsonValue value) {
        if (!hasMeta()) {
            return Collections.emptyList();
        }

        return getMeta().get(name, value);
    }

    public List<MetaItem> getMetaItems(Predicate<MetaItem> filter) {
        if (!hasMeta()) {
            return Collections.emptyList();
        }

        return getMeta().get(filter);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && AttributeUtil.nameIsValid(name) && getType() != null;
    }
}
