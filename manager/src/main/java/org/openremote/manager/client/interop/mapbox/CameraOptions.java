package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsProperty;

public class CameraOptions {
    @JsProperty
    public LngLat center;

    @JsProperty
    public int zoom;

    @JsProperty
    public int bearing;

    @JsProperty
    public int pitch;

    @JsProperty
    public LngLat around;
}
