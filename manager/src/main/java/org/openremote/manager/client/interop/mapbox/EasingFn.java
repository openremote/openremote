package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface EasingFn {
    void ease(double easeFactor);
}
