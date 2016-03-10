package org.openremote.manager.client.interop;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Runnable {
    void run();
}
