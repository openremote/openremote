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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Convenience overlay API for {@link JsonObject}.
 *
 * Modifies the given or an empty object.
 */
public abstract class Attributes<CHILD extends Attributes<CHILD, A>, A extends Attribute> {

    final protected JsonObject jsonObject;

    public Attributes() {
        this(Json.createObject());
    }

    // TODO @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Attributes(JsonObject jsonObject) {
        this.jsonObject = jsonObject != null ? jsonObject : Json.createObject();
    }

    public Attributes(CHILD attributes) {
        this.jsonObject = attributes != null && attributes.getJsonObject() != null ? attributes.getJsonObject() : Json.createObject();
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public boolean hasAttribute(String name) {
        return jsonObject.hasKey(name);
    }

    public List<String> names() {
        return Collections.unmodifiableList(Arrays.asList(jsonObject.keys()));
    }

    public int size() {
        return jsonObject.keys().length;
    }

    public List<A> get() {
        List<A> attributes = new ArrayList<>();
        String[] keys = jsonObject.keys();
        for (String key : keys) {
            attributes.add(createAttribute(key, jsonObject.getObject(key)));
        }
        return Collections.unmodifiableList(attributes);
    }

    public A get(String name) {
        return hasAttribute(name) ? createAttribute(name, jsonObject.getObject(name)) : null;
    }

    @SuppressWarnings("unchecked")
    public CHILD put(A... attributes) {
        if (attributes != null)
            for (A attribute : attributes) {
                jsonObject.put(attribute.getName(), attribute.getJsonObject());
            }
        return (CHILD) this;
    }

    @SuppressWarnings("unchecked")
    public CHILD remove(String name) {
        if (hasAttribute(name)) {
            jsonObject.remove(name);
        }
        return (CHILD) this;
    }

    @SuppressWarnings("unchecked")
    public CHILD clear(String... excludeAttributes) {
        List<String> excluded =
            excludeAttributes != null ? Arrays.asList(excludeAttributes) : new ArrayList<>();
        for (A attribute : get()) {
            if (!excluded.contains(attribute.getName())) {
                remove(attribute.getName());
            }
        }
        return (CHILD) this;
    }

    abstract public CHILD copy();

    abstract protected A createAttribute(String name, JsonObject jsonObject);

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
