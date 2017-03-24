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

import com.fasterxml.jackson.annotation.JsonCreator;
import elemental.json.Json;
import elemental.json.JsonObject;

import java.util.*;

/**
 * Convenience overlay API for {@link JsonObject}.
 *
 * Modifies the given or an empty object.
 */
public class Attributes {

    final protected JsonObject jsonObject;

    public Attributes() {
        this(Json.createObject());
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Attributes(JsonObject jsonObject) {
        this.jsonObject = jsonObject != null ? jsonObject : Json.createObject();
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public boolean hasAttribute(String name) {
        return jsonObject.hasKey(name);
    }

    public String[] names() {
        return jsonObject.keys();
    }

    public int size() {
        return get().length;
    }

    public Attribute[] get() {
        Set<Attribute> attributes = new LinkedHashSet<>();
        String[] keys = jsonObject.keys();
        for (String key : keys) {
            if (hasAttribute(key)) {
                Attribute attribute = new Attribute(key, jsonObject.getObject(key));
                attributes.add(attribute);
            }
        }
        return attributes.toArray(new Attribute[attributes.size()]);
    }

    public Attribute get(String name) {
        return hasAttribute(name) ? new Attribute(name, jsonObject.getObject(name)) : null;
    }

    public Attributes put(Attribute... attributes) {
        if (attributes != null)
            for (Attribute attribute : attributes) {
                jsonObject.put(attribute.getName(), attribute.getJsonObject());
            }
        return this;
    }

    public Attributes remove(String name) {
        if (hasAttribute(name)) {
            jsonObject.remove(name);
        }
        return this;
    }

    public Attributes clear(String... excludeAttributes) {
        List<String> excluded =
            excludeAttributes != null ? Arrays.asList(excludeAttributes) : new ArrayList<>();
        for (Attribute attribute : get()) {
            if (!excluded.contains(attribute.getName())) {
                remove(attribute.getName());
            }
        }
        return this;
    }

    public Attributes copy() {
        return new Attributes(Json.parse(getJsonObject().toJson()));
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
