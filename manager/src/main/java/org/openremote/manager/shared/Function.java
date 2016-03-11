package org.openremote.manager.shared;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Function<T,U> {
    U apply(T object);
}
