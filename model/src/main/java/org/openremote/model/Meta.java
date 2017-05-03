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

import java.util.AbstractList;
import java.util.Collections;

/**
 * A {@link java.util.List} of {@link MetaItem} elements.
 * <p>
 * Note that duplicate item names are allowed for multi-valued elements.
 *
 */
public class Meta extends AbstractList<MetaItem> {

    final protected JsonArray jsonArray;

    public Meta() {
        this(Json.createArray());
    }

    public Meta(MetaItem... items) {
        this(Json.createArray());
        Collections.addAll(this, items);
    }

    public Meta(JsonArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    public JsonArray getJsonArray() {
        return jsonArray;
    }

    @Override
    public int size() {
        return getJsonArray().length();
    }

    @Override
    public MetaItem get(int index) {
        checkBounds(index);
        return new MetaItem(jsonArray.getObject(index));
    }

    @Override
    public MetaItem set(int index, MetaItem metaItem) {
        checkBounds(index);
        jsonArray.set(index, metaItem.getJsonObject());

        return super.set(index, metaItem);
    }

    @Override
    public void add(int index, MetaItem item) {
        checkBounds(index-1);
        jsonArray.set(index, item.getJsonObject());
    }

    @Override
    public MetaItem remove(int index) {
        checkBounds(index);
        MetaItem item = get(index);
        jsonArray.remove(index);
        return item;
    }

    public Meta copy() {
        return new Meta((JsonArray) Json.parse(getJsonArray().toJson()));
    }

    @Override
    public String toString() {
        return jsonArray.toJson();
    }

    protected void checkBounds(int index) {
        if (index >= size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
    }
}
