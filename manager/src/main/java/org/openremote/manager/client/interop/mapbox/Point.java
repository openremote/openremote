package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Point", namespace = JsPackage.GLOBAL)
public class Point {
    @JsProperty
    public int x;

    @JsProperty
    public int y;

    public Point(int x, int y) {}
}
