/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.interop.mapbox;

import elemental.dom.Node;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Map", namespace = "mapboxgl")
public class Map {
    @JsProperty
    public boolean debug;

    @JsProperty
    public boolean loaded;

    // TODO: Get this to work with MapOptions JsType
    public Map(JsonObject options) {}

    public native Map addClass(String className);
    public native Map addClass(String className, JsonObject styleOptions);

    public native Map addControl(Control control);

    public native Map addLayer(Object layer);
    public native Map addLayer(Object layer, String beforeLayerId);

    public native Map addSource(String id, Object source);

    // TODO: add batch method fucntionality to map
//    public native void batch(WorkFn work);

    // TODO: Add bounding box event support to the map
    public native void on(String eventType, EventDataFn eventDataFn);

    public native Map easeTo(CameraOptions cameraOptions);
    public native Map easeTo(AnimationOptions animationOptions);

    public native Map featuresAt(Point point, JsonObject options, FeaturesCallbackFn callbackFn);

    public native Map featuresIn(JsonObject options, FeaturesCallbackFn callbackFn);
    public native Map featuresIn(Point[] bounds, JsonObject options, FeaturesCallbackFn callbackFn);

    public native Map fitBounds(LngLatBounds bounds, JsonObject options);
    public native Map fitBounds(LngLatBounds bounds, JsonObject options, EventData eventData);

    public native Map flyTo(JsonObject options);

    public native Map flyTo(JsonObject options, EventData eventData);

    public native double getBearing();

    public native LngLatBounds getBounds();

    public native Node getCanvas();

    public native Node getCanvasContainer();

    public native LngLat getCenter();

    public native String[] getClasses();

    public native Node getContainer();

    public native JsonArray getFilter(String layerId);

    public native JsonObject getLayer(String layerId);

    public native JsonValue getLayoutProperty(String layerId, String propertyName);
    public native JsonValue getLayoutProperty(String layerId, String propertyName, String className);

    public native JsonValue getPaintProperty(String layerId, String propertyName);
    public native JsonValue getPaintProperty(String layerId, String propertyName, String className);

    public native double getPitch();

    public native JsonObject getSource(String sourceId);

    public native JsonObject getStyle();

    public native int getZoom();

    public native boolean hasClass(String className);

    public native Map jumpTo(CameraOptions options);
    public native Map jumpTo(CameraOptions options, EventData eventData);

    public native Map panBy(int[] offset, AnimationOptions options);
    public native Map panBy(int[] offset, AnimationOptions options, EventData eventData);

    public native Map panTo(LngLat lngLat, AnimationOptions options);
    public native Map panTo(LngLat lngLat, AnimationOptions options, EventData eventData);

    public native JsonObject project(LngLat lngLat);

    public native void remove();

    public native Map removeClass(String className);
    public native Map removeClass(String className, JsonObject options);

    public native Map removeLayer(String layerId);

    public native Map removeSource(String layerId);

    public native void repaint();

    public native Map resetNorth();
    public native Map resetNorth(EventData eventData);
    public native Map resetNorth(AnimationOptions options);
    public native Map resetNorth(AnimationOptions options, EventData eventData);

    public native Map resize();

    public native Map rotateTo(int bearing);
    public native Map rotateTo(int bearing, EventData eventData);
    public native Map rotateTo(int bearing, AnimationOptions options);
    public native Map rotateTo(int bearing, AnimationOptions options, EventData eventData);

    public native Map setBearing(int bearing);
    public native Map setBearing(int bearing, EventData eventData);

    public native Map setCenter(LngLat center);
    public native Map setCenter(LngLat center, EventData eventData);

    public native Map setClasses(String[] classNames);
    public native Map setClasses(String[] classNames, JsonObject options);

    public native Map setFilter(String layerId, JsonObject filter);

    public native Map setLayerZoomRange(String layerId, int minZoom, int maxZoom);

    public native Map setLayoutProperty(String layerId, String propertyName, JsonValue propertyValue);

    public native Map setPaintProperty(String layerId, String propertyName, JsonValue propertyValue);
    public native Map setPaintProperty(String layerId, String propertyName, JsonValue propertyValue, String classSpecifier);

    public native Map setPitch(int pitch);
    public native Map setPitch(int pitch, EventData eventData);

    public native Map setStyle(JsonObject style);

    public native Map setZoom(int zoom);
    public native Map setZoom(int zoom, EventData eventData);

    public native Map snapToNorth();
    public native Map snapToNorth(AnimationOptions options);
    public native Map snapToNorth(EventData eventData);
    public native Map snapToNorth(AnimationOptions options, EventData eventData);

    public native Map stop();

    public native LngLat unproject(Point point);

    public native Map zoomIn();
    public native Map zoomIn(EventData eventData);
    public native Map zoomIn(AnimationOptions options);
    public native Map zoomIn(AnimationOptions options, EventData eventData);

    public native Map zoomOut();
    public native Map zoomOut(EventData eventData);
    public native Map zoomOut(AnimationOptions options);
    public native Map zoomOut(AnimationOptions options, EventData eventData);

    public native Map zoomTo(int zoom);
    public native Map zoomTo(int zoom, AnimationOptions options);
    public native Map zoomTo(int zoom, EventData eventData);
    public native Map zoomTo(int zoom, AnimationOptions options, EventData eventData);
}
