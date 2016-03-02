package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface EventDataFn {
    void onFire(EventData eventData);
}
