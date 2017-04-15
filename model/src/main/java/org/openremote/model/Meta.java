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
import elemental.json.JsonValue;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An array of {@link MetaItem} elements.
 *
 * Note that duplicate item names are allowed for multi-valued elements.
 */
public class Meta {

    final protected JsonArray jsonArray;

    public Meta() {
        this(Json.createArray());
    }

    public Meta(JsonArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    public JsonArray getJsonArray() {
        return jsonArray;
    }

    public int size() {
        return getJsonArray().length();
    }

    public List<MetaItem> all() {
        return get((Predicate<MetaItem>) null);
    }

    public Stream<MetaItem> stream() {
        return all().stream();
    }

    public List<MetaItem> get(String name) {
        return get(
            item -> item.getName().equals(name)
        );
    }

    public List<MetaItem> get(String name, JsonValue value) {
        return get(metaItem -> metaItem.getName().equals(name) && JsonUtil.equals(metaItem.getValue(), value));
    }

    public List<MetaItem> get(Predicate<MetaItem> filter) {
        List<MetaItem> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JsonObject jsonObject = jsonArray.getObject(i);
            MetaItem item = new MetaItem(jsonObject);
            if (filter == null) {
                list.add(item);
            } else if (filter.test(item)) {
                list.add(item);
            }
        }
        return list;
    }

    public MetaItem get(int index) {
        return index > 0 && index < jsonArray.length()
            ? new MetaItem(jsonArray.getObject(index))
            : null;
    }

    public MetaItem first(AssetMeta assetMeta) {
        return first(assetMeta.getUrn());
    }

    public MetaItem first(String name) {
        List<MetaItem> list = all();
        for (MetaItem item : list) {
            if (item.getName().equals(name))
                return item;
        }
        return null;
    }

    public MetaItem first(String name, JsonValue value) {
        List<MetaItem> list = all();
        for (MetaItem item : list) {
            if (item.getName().equals(name))
                return item;
        }
        return null;
    }

    public boolean contains(AssetMeta assetMeta) {
        return contains(assetMeta.getUrn());
    }

    public boolean contains(String name) {
        return first(name) != null;
    }

    public boolean contains(String name, JsonValue value) {
        MetaItem metaItem = first(name);
        return metaItem != null && JsonUtil.equals(metaItem.getValue(), value);
    }

    public int firstIndexOf(MetaItem item, int startIndex) {
        List<MetaItem> list = all();
        for (int i = startIndex; i < list.size(); i++) {
            if (list.get(i).getName().equals(item.getName()))
                return i;
        }
        return -1;
    }

    public Meta add(MetaItem item) {
        jsonArray.set(jsonArray.length(), item.getJsonObject());
        return this;
    }

    public Meta removeAll(AssetMeta assetMeta) {
        return removeAll(assetMeta.getUrn());
    }

    public Meta removeAll(String name) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JsonObject jsonObject = jsonArray.getObject(i);
            MetaItem item = new MetaItem(jsonObject);
            if (name.equals(item.getName()))
                jsonArray.remove(i);
        }
        return this;
    }

    public Meta remove(int index) {
        if (index >= 0 && index < jsonArray.length())
            jsonArray.remove(index);
        return this;
    }

    public Meta replace(String name, MetaItem item) {
        removeAll(name);
        add(item);
        return this;
    }

    public Meta copy() {
        return new Meta((JsonArray) Json.parse(getJsonArray().toJson()));
    }

    @Override
    public String toString() {
        return jsonArray.toJson();
    }
}
