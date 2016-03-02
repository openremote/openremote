package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Control")
public abstract class Control<T> {

    public native T addTo(Map map);

    public native void remove();
}
