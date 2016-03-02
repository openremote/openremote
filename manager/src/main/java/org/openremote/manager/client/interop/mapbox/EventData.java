package org.openremote.manager.client.interop.mapbox;

import elemental.events.Event;
import jsinterop.annotations.JsProperty;

public class EventData {
    @JsProperty
    public native Event getOriginalEvent();

    @JsProperty
    public native Point getPoint();

    @JsProperty
    public native LngLat getLngLat();
}
