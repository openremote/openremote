package org.openremote.manager.client.interop;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Consumer<T> {
    void accept(T t);
}
