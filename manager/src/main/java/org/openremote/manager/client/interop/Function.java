package org.openremote.manager.client.interop;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Function<T,U> {
    U apply(T object);
}
