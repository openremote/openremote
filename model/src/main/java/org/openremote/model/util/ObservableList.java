/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ObservableList<T> extends ArrayList<T> {

    protected Consumer<ObservableList<T>> onModified;

    public ObservableList(int initialCapacity, Consumer<ObservableList<T>> onModified) {
        super(initialCapacity);
        this.onModified = onModified;
    }

    public ObservableList(Consumer<ObservableList<T>> onModified) {
        this.onModified = onModified;
    }

    public ObservableList(Collection<? extends T> c, Consumer<ObservableList<T>> onModified) {
        super(c);
        this.onModified = onModified;
    }

    protected void notifyModified() {
        if (onModified != null) {
            onModified.accept(this);
        }
    }

    @Override
    public T set(int index, T element) {
        T result = super.set(index, element);
        notifyModified();
        return result;
    }

    @Override
    public boolean add(T t) {
        boolean result = super.add(t);
        notifyModified();
        return result;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        notifyModified();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean result = super.addAll(c);
        if (result)
            notifyModified();
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean result = super.addAll(index, c);
        if (result)
            notifyModified();
        return result;
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        super.replaceAll(operator);
        // May not have modified the list but just in case
        notifyModified();
    }

    @Override
    public T remove(int index) {
        T result = super.remove(index);
        notifyModified();
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = super.remove(o);
        if (result)
            notifyModified();
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        notifyModified();
    }

    public void clear(boolean notify) {
        super.clear();
        if (notify) {
            notifyModified();
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        notifyModified();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = super.removeAll(c);
        if (result)
            notifyModified();
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = super.retainAll(c);
        if (result)
            notifyModified();
        return result;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        boolean result = super.removeIf(filter);
        if (result)
            notifyModified();
        return result;
    }
}
