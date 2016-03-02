package org.openremote.manager.client.interop.mapbox;

import elemental.json.JsonArray;
import elemental.json.JsonNumber;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class AnimationOptions {
    @JsProperty
    public int duration;

    @JsProperty
    public EasingFn easing;

    @JsProperty
    public JsonArray offset;

    @JsProperty
    public boolean animate;
}
