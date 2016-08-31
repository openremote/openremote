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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import elemental.json.JsonObject;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@JsonSerialize(using = AttributesSerializer.class)
@JsonDeserialize(using = AttributesDeserializer.class)
public class Attributes {
    final protected JsonObject jsonObject;

    public Attributes() {
        this(elemental.json.Json.createObject());
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Attributes(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
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

    public Attributes add(Attribute attribute) {
        jsonObject.put(attribute.getName(), attribute.getJsonObject());
        return this;
    }

    public Attributes remove(String name) {
        if (hasAttribute(name)) {
            jsonObject.remove(name);
        }
        return this;
    }

    public Attributes clear() {
        for (Attribute attribute : get()) {
            remove(attribute.getName());
        }
        return this;
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
