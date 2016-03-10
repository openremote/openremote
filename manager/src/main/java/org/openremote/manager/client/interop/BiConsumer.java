package org.openremote.manager.client.interop;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface BiConsumer<T,U> {
    void accept(T t, U u);
}
