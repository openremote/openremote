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
package org.openremote.manager.client.interop.promise;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.openremote.manager.shared.BiConsumer;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Function;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Promise")
public class Promise<T,U> {

    public Promise(BiConsumer<Consumer<T>,Consumer<U>> fn) {}

    public Promise() {}

    public native <V,W> Promise<V,W> then(Function<T,Promise<V,W>> f);

    public native Promise<T,U> then(Consumer<T> f);

    public native <V,W> Promise<V,W> then(Function<T,Promise<V,W>> resolve, Function<U,Promise<V,W>> reject);

    public native Promise<T,U> then(Consumer<T> resolve, Consumer<T> reject);

    @JsMethod(name = "catch")
    public native <V,W> Promise<V,W> catchException(Function<U,Promise<V,W>> error);

    @JsMethod(name = "catch")
    public native Promise<T,U> catchException(Consumer<U> error);

    public static native <T> Promise resolve(T obj);

    public static native <U> Promise reject(U obj);

    public static native <T,U> Promise<T,U> all(Promise<T,U>... promises);

    public static native <T,U> Promise<T,U> race(Promise<T,U>... promises);
}
