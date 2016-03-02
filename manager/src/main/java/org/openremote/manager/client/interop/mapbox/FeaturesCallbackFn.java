package org.openremote.manager.client.interop.mapbox;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface FeaturesCallbackFn {
    void onCallback(JsonObject error, JsonArray features);
}
