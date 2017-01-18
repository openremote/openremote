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
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * An array of {@link MetadataItem} elements.
 *
 * Note that duplicate item names are allowed for multi-valued elements.
 */
public class Metadata {

    final protected JsonArray jsonArray;

    public Metadata() {
        this(Json.createArray());
    }

    public Metadata(JsonArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    public JsonArray getJsonArray() {
        return jsonArray;
    }

    public MetadataItem[] all() {
        List<MetadataItem> list = asList();
        return list.toArray(new MetadataItem[list.size()]);
    }

    public MetadataItem[] get(String name) {
        List<MetadataItem> list = asList(
            item -> item.getName().equals(name)
        );
        return list.toArray(new MetadataItem[list.size()]);
    }

    public MetadataItem get(int index) {
        return index > 0 && index < jsonArray.length()
            ? new MetadataItem(jsonArray.getObject(index))
            : null;
    }

    public MetadataItem first(String name) {
        List<MetadataItem> list = asList();
        for (MetadataItem item : list) {
            if (item.getName().equals(name))
                return item;
        }
        return null;
    }

    public boolean contains(String name) {
        return first(name) != null;
    }

    public int indexOf(MetadataItem item) {
        List<MetadataItem> list = asList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(item.getName()))
                return i;
        }
        return -1;
    }

    public Metadata add(MetadataItem item) {
        jsonArray.set(jsonArray.length(), item.getJsonObject());
        return this;
    }

    public Metadata removeAll(String name) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JsonObject jsonObject = jsonArray.getObject(i);
            MetadataItem item = new MetadataItem(jsonObject);
            if (name.equals(item.getName()))
                jsonArray.remove(i);
        }
        return this;
    }

    public Metadata remove(int index) {
        if (index > 0 && index < jsonArray.length())
            jsonArray.remove(index);
        return this;
    }

    public Metadata copy() {
        return new Metadata((JsonArray) Json.parse(getJsonArray().toJson()));
    }

    protected List<MetadataItem> asList() {
        return asList(null);
    }

    protected List<MetadataItem> asList(Function<MetadataItem, Boolean> filter) {
        List<MetadataItem> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JsonObject jsonObject = jsonArray.getObject(i);
            MetadataItem item = new MetadataItem(jsonObject);
            if (filter == null) {
                list.add(item);
            } else if (filter.apply(item)) {
                list.add(item);
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return jsonArray.toJson();
    }
}
