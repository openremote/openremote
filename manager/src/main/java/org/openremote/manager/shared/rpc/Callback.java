package org.openremote.manager.shared.rpc;

import elemental.xml.XMLHttpRequest;
import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Callback<T> {
    void call(int responseCode, XMLHttpRequest xmlHttpRequest, T entity);
}
