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
package org.openremote.model.attribute;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Values;

import java.util.AbstractList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@link java.util.List} of {@link MetaItem} elements, wrapping an {@link ArrayValue}.
 * <p>
 * Note that duplicate item names are allowed for multi-valued elements.
 */
public class Meta extends AbstractList<MetaItem> {

    final protected ArrayValue arrayValue;

    public Meta() {
        this(Values.createArray());
    }

    public Meta(MetaItem... items) {
        this(Values.createArray());
        Collections.addAll(this, items);
    }

    public Meta(ArrayValue arrayValue) {
        this.arrayValue = Objects.requireNonNull(arrayValue);
    }

    public ArrayValue getArrayValue() {
        return arrayValue;
    }

    @Override
    public int size() {
        return arrayValue.length();
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     * @throws NoSuchElementException if there is no value at the given index.
     */
    @Override
    public MetaItem get(int index) {
        checkBounds(index);
        return new MetaItem(
            arrayValue.getObject(index)
                .orElseThrow(() -> new NoSuchElementException("At index: " + index))
        );
    }

    @Override
    public MetaItem set(int index, MetaItem metaItem) {
        checkBounds(index);
        arrayValue.set(index, metaItem.getObjectValue());
        return super.set(index, metaItem);
    }

    @Override
    public void add(int index, MetaItem item) {
        checkBounds(index == 0 ? 0 : index-1);
        arrayValue.add(index, item.getObjectValue());
    }

    @Override
    public MetaItem remove(int index) {
        checkBounds(index);
        MetaItem item = get(index);
        arrayValue.remove(index);
        return item;
    }

    public Meta copy() {
        return new Meta(getArrayValue().deepCopy());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            getArrayValue().toJson() +
            '}';
    }

    protected void checkBounds(int index) {
        if (index != 0 && (index < 0 || index >= size()))
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
    }
}
