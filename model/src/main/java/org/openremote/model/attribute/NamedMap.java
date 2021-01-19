/*
 * Copyright 2020, OpenRemote Inc.
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

import com.google.common.collect.ForwardingMap;
import org.openremote.model.value.AbstractNameValueDescriptorHolder;
import org.openremote.model.value.AbstractNameValueHolder;
import org.openremote.model.value.NameHolder;
import org.openremote.model.value.ValueHolder;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Special map for {@link NameHolder} items where item names are used as map keys.
 */
public class NamedMap<T extends AbstractNameValueHolder<?>> extends ForwardingMap<String, T> implements Serializable {

    protected Map<String, T> delegate = new HashMap<>();

    public NamedMap() {
    }

    public NamedMap(Collection<? extends T> c) {
        addAll(c);
    }

    public NamedMap(Map<? extends String, ? extends T> map) {
        if (map != null) {
            addAll(map.values());
        }
    }

    @Override
    protected Map<String, T> delegate() {
        return delegate;
    }

    public T put(T value) {
        return this.put(value.getName(), value);
    }

    @Override
    public T put(String key, T value) {
        if (!Objects.equals(key, value.getName())) {
            throw new IllegalStateException("Item key and value name must match: key=" + key + ", name=" + value.getName());
        }
        return super.put(key, value);
    }

    protected T putSilent(T value) {
        return super.put(value.getName(), value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> map) {
        addAll(map.values());
    }

    protected void putAllSilent(Collection<? extends T> c) {
        c.forEach(this::putSilent);
    }

    public void add(@NotNull T t) {
        put(t);
    }

    public void addAll(Collection<? extends T> c) {
        if (c == null) {
            return;
        }

        c.forEach(this::add);
    }

    @SafeVarargs
    public final void addAll(T...items) {
        this.addAll(Arrays.asList(items));
    }

    public void addAll(Map<? extends String, ? extends T> map) {
        addAll(map.values());
    }

    public void addOrReplace(T item) {
        putSilent(item);
    }

    @SafeVarargs
    public final void addOrReplace(T...items) {
        addOrReplace(Arrays.asList(items));
    }

    public final void addOrReplace(Collection<? extends T> items) {
        items.forEach(this::addOrReplace);
    }

    public void addOrReplace(Map<? extends String, ? extends T> map) {
        addOrReplace(map.values());
    }

    public Optional<T> get(String name) {
        return Optional.ofNullable(super.get(name));
    }

    @SuppressWarnings("unchecked")
    public <V, W extends AbstractNameValueHolder<V>> Optional<W> get(AbstractNameValueDescriptorHolder<V> nameValueDescriptorHolder) {
        Optional<T> valueProvider = get(nameValueDescriptorHolder.getName());
        return valueProvider.map(item -> {
            Class<?> itemType = item.getType().getType();
            Class<V> expectedType = nameValueDescriptorHolder.getType().getType();
            if (itemType == expectedType) {
                return (W)item;
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<S> getValue(String name) {
        return get(name).flatMap(ValueHolder::getValue).map(v -> (S)v);
    }

    @SuppressWarnings("unchecked")
    public <S> S getValueOrDefault(String name) {
        return (S) get(name).flatMap(ValueHolder::getValue).orElse(null);
    }

    public <S> Optional<S> getValue(AbstractNameValueDescriptorHolder<S> nameValueDescriptorProvider) {
        return get(nameValueDescriptorProvider).flatMap(ValueHolder::getValue);
    }

    public <S> S getValueOrDefault(AbstractNameValueDescriptorHolder<S> nameValueDescriptorProvider) {
        return get(nameValueDescriptorProvider).flatMap(ValueHolder::getValue).orElse(null);
    }

    public boolean has(NameHolder nameHolder) {
        return has(nameHolder.getName());
    }

    public boolean has(String name) {
        return containsKey(name);
    }

    public Stream<T> stream() {
        return values().stream();
    }

    public void forEach(Consumer<? super T> action) {
        values().forEach(action);
    }

    public void removeIf(Predicate<? super T> filter) {
        values().removeIf(filter);
    }
}
