package org.openremote.manager.client.interop.resteasy;

import elemental.json.JsonObject;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "", name = "MapService")
public class MapService {
    @JsMethod
    public static native JsonObject getOptions();
}
