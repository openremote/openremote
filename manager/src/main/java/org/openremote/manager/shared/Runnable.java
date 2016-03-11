package org.openremote.manager.shared;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface Runnable {
    void run();
}
