package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "LngLatBounds", namespace = "mapboxgl")
public class LngLatBounds {
    public LngLatBounds(LngLat sw, LngLat ne) {}

    public native LngLat getNorthEast();

    public native LngLat getSouthWest();

    public native LngLat getSouthEast();

    public native LngLat getNorthWest();

    public native LngLat getCenter();

    public native double getNorth();

    public native double getEast();

    public native double getSouth();

    public native double getWest();

    public native LngLatBounds extend(LngLat includeLngLat);

    public native LngLatBounds extend(LngLatBounds includeBounds);
}
