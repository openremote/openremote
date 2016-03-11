package org.openremote.manager.shared;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Consumer<T> {
    void accept(T t);
}
