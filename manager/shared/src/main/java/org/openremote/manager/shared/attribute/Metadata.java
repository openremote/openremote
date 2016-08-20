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

import elemental.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class Metadata {

    final protected JsonObject jsonObject;

    public Metadata(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public MetadataElement[] getElements() {
        Set<MetadataElement> elements = new LinkedHashSet<>();

        String[] keys = jsonObject.keys();
        for (String key : keys) {
            MetadataElement element = new MetadataElement(key, jsonObject.getObject(key));
            elements.add(element);
        }

        return elements.toArray(new MetadataElement[elements.size()]);
    }

    public MetadataElement getElement(String name) {
        return hasElement(name) ? new MetadataElement(name, jsonObject.getObject(name)) : null;
    }

    public boolean hasElement(String name) {
        return jsonObject.hasKey(name);
    }

    public Metadata addElement(MetadataElement element) {
        jsonObject.put(element.getName(), element.getJsonObject());
        return this;
    }

    @Override
    public String toString() {
        return jsonObject.toJson();
    }
}
