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
import java.util.Optional;
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

    public Stream<MetaItem> stream() {
        return getAll().stream();
    }

    public boolean any() {
        return size() == 0;
    }

    public List<MetaItem> getAll() {
        return getAll((Predicate<MetaItem>) null);
    }

    public List<MetaItem> getAll(String name) {
        return getAll(
            item -> item.getName().equals(name)
        );
    }

    public List<MetaItem> getAll(String name, JsonValue value) {
        return getAll(metaItem -> metaItem.getName().equals(name) && JsonUtil.equals(metaItem.getValue(), value));
    }

    public <T extends JsonValue> List<MetaItem> getAll(String name, Class<T> clazz) {
        return getAll(
            item -> item.getName().equals(name)
                && item.getValue() != null
                && item.getValue().getClass().getSuperclass() == clazz);
    }

    public List<MetaItem> getAll(Predicate<MetaItem> filter) {
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

    public Optional<MetaItem> get(int index) {
        return Optional.ofNullable(
            index >= 0 && index < jsonArray.length()
            ? new MetaItem(jsonArray.getObject(index))
            : null
        );
    }

    public Optional<MetaItem> get(String name) {
        return get(name, 0);
    }

    public Optional<MetaItem> get(String name, int startIndex) {
        int index = indexOf(name, startIndex);
        if (index >= 0) {
            return get(index);
        }

        return Optional.empty();
    }

    public Optional<MetaItem> get(String name, JsonValue value) {
		return get(name, value, 0);
    }

    public Optional<MetaItem> get(String name, JsonValue value, int startIndex) {
        int index = indexOf(name, value, startIndex);
        if (index >= 0) {
            return get(index);
        }

        return Optional.empty();
    }

    public <T extends JsonValue> Optional<MetaItem> get(String name, Class<T> clazz) {
		return get(name, clazz, 0);
    }

    public <T extends JsonValue> Optional<MetaItem> get(String name, Class<T> clazz, int startIndex) {
        int index = indexOf(name, clazz, startIndex);
        if (index >= 0) {
            return get(index);
        }

        return Optional.empty();
    }

    public Optional<MetaItem> get(AssetMeta meta) {
        return get(meta.getUrn(), 0);
    }

    public Optional<MetaItem> get(AssetMeta meta, int startIndex) {
        return get(meta.getUrn(), startIndex);
    }

    public Optional<MetaItem> get(AssetMeta meta, JsonValue value) {
        return get(meta.getUrn(), value, 0);
    }

    public Optional<MetaItem> get(AssetMeta meta, JsonValue value, int startIndex) {
        return get(meta.getUrn(), value, startIndex);
    }

    public <T extends JsonValue> Optional<MetaItem> get(AssetMeta meta, Class<T> clazz) {
        return get(meta.getUrn(), clazz, 0);
    }

    public <T extends JsonValue> Optional<MetaItem> get(AssetMeta meta, Class<T> clazz, int startIndex) {
        return get(meta.getUrn(), clazz, startIndex);
    }

    public boolean contains(MetaItem item) {
        return contains(item.getName(), item.getValue());
    }

    public boolean contains(String name) {
        return get(name) != null;
    }

    public <T extends JsonValue> boolean contains(String name, Class<T> clazz) {
        return indexOf(name, clazz) >= 0;
    }

    public boolean contains(String name, JsonValue value) {
        return indexOf(name, value) >= 0;
    }

    public boolean contains(AssetMeta meta) {
        return get(meta.getUrn()) != null;
    }

    public <T extends JsonValue> boolean contains(AssetMeta meta, Class<T> clazz) {
        return indexOf(meta.getUrn(), clazz) >= 0;
    }

    public boolean contains(AssetMeta meta, JsonValue value) {
        return indexOf(meta.getUrn(), value) >= 0;
    }

    public int indexOf(MetaItem item) {
        return indexOf(item.getName(), item.getValue(), 0);
    }

    public int indexOf(MetaItem item, int startIndex) {
        return indexOf(item.getName(), item.getValue(), startIndex);
    }

    public int indexOf(String name) {
        return indexOf(name, 0);
    }

    public int indexOf(String name, int startIndex) {
        List<MetaItem> list = getAll();
        for (int i = startIndex; i < list.size(); i++) {
            MetaItem item = list.get(i);
            if (MetaItem.matches(name).test(item))
                return i;
        }
        return -1;
    }

    public <T extends JsonValue> int indexOf(String name, Class<T> clazz) {
        return indexOf(name, clazz, 0);
    }

    public <T extends JsonValue> int indexOf(String name, Class<T> clazz, int startIndex) {
        List<MetaItem> list = getAll();
        for (int i = startIndex; i < list.size(); i++) {
            MetaItem item = list.get(i);
            if (MetaItem.matches(name, clazz).test(item))
                return i;
        }
        return -1;
    }

    public <T extends JsonValue> int indexOf(String name, JsonValue value) {
        return indexOf(name, value, 0);
    }

    public <T extends JsonValue> int indexOf(String name, JsonValue value, int startIndex) {
        List<MetaItem> list = getAll();
        for (int i = startIndex; i < list.size(); i++) {
            MetaItem item = list.get(i);
            if (MetaItem.matches(name, value).test(item))
                return i;
        }
        return -1;
    }

    public Meta add(MetaItem item) {
        jsonArray.set(jsonArray.length(), item.getJsonObject());
        return this;
    }

    public Meta removeAll(AssetMeta meta) {
        return removeAll(meta.getUrn());
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

    public Meta replace(AssetMeta meta, MetaItem item) {
        return replace(meta.getUrn(), item);
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
