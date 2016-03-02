package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "LngLat", namespace = "mapboxgl")
public class LngLat {
    public LngLat(double lng, double lat) {}

    @JsProperty
    public native double getLat();

    @JsProperty
    public native double getLng();

}
