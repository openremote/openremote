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
package org.openremote.app.client.interop.mapbox;

import elemental2.dom.Element;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;

@JsType(isNative = true, name = "Map", namespace = "mapboxgl")
public class MapboxMap {

    @JsProperty
    public boolean debug;

    @JsProperty
    public boolean loaded;

    public MapboxMap(Any options) {}

    public native MapboxMap addClass(String className);
    public native MapboxMap addClass(String className, Any styleOptions);

    public native MapboxMap addControl(Control control);

    public native MapboxMap addLayer(Any layer);
    public native MapboxMap addLayer(Any layer, String beforeLayerId);

    public native MapboxMap addSource(String sourceId, Any source);
    public native GeoJSONSource getSource(String sourceId);

    // TODO: add batch method fucntionality to map
//    public native void batch(WorkFn work);

    // TODO: Add bounding box event support to the map
    public native void on(String eventType, EventDataFn eventDataFn);

    public native MapboxMap easeTo(CameraOptions cameraOptions);
    public native MapboxMap easeTo(AnimationOptions animationOptions);

    public native MapboxMap featuresAt(Point point, Any options, FeaturesCallbackFn callbackFn);

    public native MapboxMap featuresIn(Any options, FeaturesCallbackFn callbackFn);
    public native MapboxMap featuresIn(Point[] bounds, Any options, FeaturesCallbackFn callbackFn);

    public native MapboxMap fitBounds(LngLatBounds bounds, Any options);
    public native MapboxMap fitBounds(LngLatBounds bounds, Any options, EventData eventData);

    public native MapboxMap flyTo(Any options);

    public native MapboxMap flyTo(Any options, EventData eventData);

    public native double getBearing();

    public native LngLatBounds getBounds();

    public native Element getCanvas();

    public native Element getCanvasContainer();

    public native LngLat getCenter();

    public native String[] getClasses();

    public native Element getContainer();

    public native Any getFilter(String layerId);

    public native Any getLayer(String layerId);

    public native Any getLayoutProperty(String layerId, String propertyName);
    public native Any getLayoutProperty(String layerId, String propertyName, String className);

    public native Any getPaintProperty(String layerId, String propertyName);
    public native Any getPaintProperty(String layerId, String propertyName, String className);

    public native double getPitch();

    public native Any getStyle();

    public native int getZoom();

    public native boolean hasClass(String className);

    public native MapboxMap jumpTo(CameraOptions options);
    public native MapboxMap jumpTo(CameraOptions options, EventData eventData);

    public native MapboxMap panBy(int[] offset, AnimationOptions options);
    public native MapboxMap panBy(int[] offset, AnimationOptions options, EventData eventData);

    public native MapboxMap panTo(LngLat lngLat, AnimationOptions options);
    public native MapboxMap panTo(LngLat lngLat, AnimationOptions options, EventData eventData);

    public native Any project(LngLat lngLat);

    public native void remove();

    public native MapboxMap removeClass(String className);
    public native MapboxMap removeClass(String className, Any options);

    public native MapboxMap removeLayer(String layerId);

    public native MapboxMap removeSource(String sourceId);

    public native void repaint();

    public native MapboxMap resetNorth();
    public native MapboxMap resetNorth(EventData eventData);
    public native MapboxMap resetNorth(AnimationOptions options);
    public native MapboxMap resetNorth(AnimationOptions options, EventData eventData);

    public native MapboxMap resize();

    public native MapboxMap rotateTo(int bearing);
    public native MapboxMap rotateTo(int bearing, EventData eventData);
    public native MapboxMap rotateTo(int bearing, AnimationOptions options);
    public native MapboxMap rotateTo(int bearing, AnimationOptions options, EventData eventData);

    public native MapboxMap setBearing(int bearing);
    public native MapboxMap setBearing(int bearing, EventData eventData);

    public native MapboxMap setCenter(LngLat center);
    public native MapboxMap setCenter(LngLat center, EventData eventData);

    public native MapboxMap setClasses(String[] classNames);
    public native MapboxMap setClasses(String[] classNames, Any options);

    public native MapboxMap setFilter(String layerId, Any filter);

    public native MapboxMap setLayerZoomRange(String layerId, int minZoom, int maxZoom);

    public native MapboxMap setLayoutProperty(String layerId, String propertyName, Any propertyValue);

    public native MapboxMap setPaintProperty(String layerId, String propertyName, Any propertyValue);
    public native MapboxMap setPaintProperty(String layerId, String propertyName, Any propertyValue, String classSpecifier);

    public native MapboxMap setPitch(int pitch);
    public native MapboxMap setPitch(int pitch, EventData eventData);

    public native MapboxMap setStyle(Any style);

    public native MapboxMap setZoom(int zoom);
    public native MapboxMap setZoom(int zoom, EventData eventData);

    public native MapboxMap snapToNorth();
    public native MapboxMap snapToNorth(AnimationOptions options);
    public native MapboxMap snapToNorth(EventData eventData);
    public native MapboxMap snapToNorth(AnimationOptions options, EventData eventData);

    public native MapboxMap stop();

    public native LngLat unproject(Point point);

    public native MapboxMap zoomIn();
    public native MapboxMap zoomIn(EventData eventData);
    public native MapboxMap zoomIn(AnimationOptions options);
    public native MapboxMap zoomIn(AnimationOptions options, EventData eventData);

    public native MapboxMap zoomOut();
    public native MapboxMap zoomOut(EventData eventData);
    public native MapboxMap zoomOut(AnimationOptions options);
    public native MapboxMap zoomOut(AnimationOptions options, EventData eventData);

    public native MapboxMap zoomTo(int zoom);
    public native MapboxMap zoomTo(int zoom, AnimationOptions options);
    public native MapboxMap zoomTo(int zoom, EventData eventData);
    public native MapboxMap zoomTo(int zoom, AnimationOptions options, EventData eventData);
}
