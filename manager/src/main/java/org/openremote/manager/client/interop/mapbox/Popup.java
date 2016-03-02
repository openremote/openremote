package org.openremote.manager.client.interop.mapbox;

import elemental.json.JsonObject;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Popup", namespace = "mapboxgl")
public class Popup extends Control<Popup> {
    public Popup() {}

    public Popup(JsonObject options) {}

    public native LngLat getLngLat();

    public native Popup setLngLat(LngLat lngLat);

    public native Popup setHTML(String html);

    public native Popup setText(String text);
}
