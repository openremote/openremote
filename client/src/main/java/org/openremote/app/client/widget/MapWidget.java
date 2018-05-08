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
package org.openremote.app.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FlowPanel;
import elemental2.dom.DomGlobal;
import org.openremote.app.client.interop.mapbox.*;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.value.Values.create;

public class MapWidget extends FlowPanel {

    private static final Logger LOG = Logger.getLogger(MapWidget.class.getName());

    public interface ClickListener {
        void onClick(double lng, double lat);
    }

    public interface ResizeListener {
        void onResize();
    }

    // Feature IDs
    public static final String FEATURE_SOURCE_CIRCLE = "feature-source-circle";
    public static final String FEATURE_LAYER_CIRCLE = "feature-layer-circle";
    public static final String FEATURE_SOURCE_DROPPED_PIN = "feature-source-dropped-pin";
    public static final String FEATURE_LAYER_DROPPED_PIN = "feature-layer-dropped-pin";

    // Render a circle on location
    static final public ObjectValue LAYER_CIRCLE = Values.createObject();

    // Render a dropped pin and feature title on location
    static final public ObjectValue LAYER_DROPPED_PIN = Values.createObject();

    static {
        {
            ObjectValue layout = Values.createObject();
            layout.put("visibility", "visible");

            ObjectValue paint = Values.createObject();
            paint.put("circle-radius", 12.0); // This MUST be double!
            paint.put("circle-opacity", 0.5);
            // TODO Add color to theme settings
            paint.put("circle-color", "rgb(73, 169, 66)");

            LAYER_CIRCLE.put("id", FEATURE_LAYER_CIRCLE);
            LAYER_CIRCLE.put("type", "circle");
            LAYER_CIRCLE.put("source", FEATURE_SOURCE_CIRCLE);
            LAYER_CIRCLE.put("layout", layout);
            LAYER_CIRCLE.put("paint", paint);
        }
        {
            ObjectValue layout = Values.createObject();
            layout.put("visibility", "visible");

            layout.put("text-field", "{title}");
            ArrayValue textFont = Values.createArray();
            textFont.add(create("Open Sans Semibold"));
            layout.put("text-font", textFont);
            ObjectValue textSize = Values.createObject();
            ArrayValue stop1 = Values.createArray();
            stop1.addAll(create(3), create(8));
            ArrayValue stop2 = Values.createArray();
            stop2.addAll(create(10), create(20));
            ArrayValue stops = Values.createArray();
            stops.addAll(stop1, stop2);
            textSize.put("stops", stops);
            layout.put("text-size", textSize);
            layout.put("text-allow-overlap", true);
            ArrayValue offset = Values.createArray();
            offset.addAll(create(0), create(0.8));
            layout.put("text-offset", offset);
            layout.put("text-max-width", create(44));

            layout.put("icon-image", "marker_15");
            offset = Values.createArray();
            offset.addAll(create(0), create(-4));
            layout.put("icon-offset", offset);

            ObjectValue paint = Values.createObject();
            // TODO Add color to theme settings
            //paint.put("icon-color", "rgb(193, 215, 47)");
            paint.put("text-color", "#000");
            paint.put("text-halo-color", "#fff");
            paint.put("text-halo-width", 2.0);
            paint.put("text-halo-blur", 4.0);

            LAYER_DROPPED_PIN.put("id", FEATURE_LAYER_DROPPED_PIN);
            LAYER_DROPPED_PIN.put("type", "symbol");
            LAYER_DROPPED_PIN.put("source", FEATURE_SOURCE_DROPPED_PIN);
            LAYER_DROPPED_PIN.put("layout", layout);
            LAYER_DROPPED_PIN.put("paint", paint);
        }

    }

    protected String hostElementId;
    protected ClickListener clickListener;
    protected List<ResizeListener> resizeListeners = new ArrayList<>();
    protected Popup popup;
    protected MapboxMap mapboxMap;
    protected boolean mapReady;

    public MapWidget() {
        hostElementId = Document.get().createUniqueId();
        getElement().setId(hostElementId);
        setStyleName("layout vertical or-MapWidget");
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void addResizeListener(ResizeListener resizeListener) {
        this.resizeListeners.add(resizeListener);
    }

    public void removeResizeListener(ResizeListener resizeListener) {
        this.resizeListeners.remove(resizeListener);
    }

    public boolean isInitialised() {
        return mapboxMap != null;
    }

    public void initialise(ObjectValue mapOptions, Runnable onReady) {
        if (mapOptions == null) {
            return;
        }
        LOG.fine("Initializing Mapbox map");
        if (mapboxMap != null) {
            throw new IllegalStateException("Already initialized");
        }

        mapOptions.put("container", hostElementId);
        mapOptions.put("attributionControl", true);
        mapboxMap = new MapboxMap(mapOptions.asAny());

        mapboxMap.on(EventType.LOAD, eventData -> {
            initFeatureLayers();
            mapReady = true;
            if (onReady != null) {
                onReady.run();
            }
        });

        mapboxMap.on(EventType.CLICK, eventData -> {
            if (clickListener != null) {
                clickListener.onClick(eventData.getLngLat().getLng(), eventData.getLngLat().getLat());
            }
        });

        mapboxMap.on(EventType.RESIZE, eventData -> resizeListeners.forEach(ResizeListener::onResize));
    }

    public boolean isMapReady() {
        return mapReady;
    }

    public void addNavigationControl() {
        if (!isMapReady())
            throw new IllegalStateException("Map not ready");
        mapboxMap.addControl(new NavigationControl());
    }

    public void resize() {
        if (!isMapReady())
            throw new IllegalStateException("Map not ready");
        mapboxMap.resize();
    }

    public void setOpaque(boolean opaque) {
        removeStyleName("opaque");
        if (opaque) {
            addStyleName("opaque");
        }
    }

    public void showPopup(double lng, double lat, String text) {
        if (!isMapReady())
            throw new IllegalStateException("Map not ready");
        ObjectValue popupOptions = Values.createObject();
        popupOptions.put("closeOnClick", create(false));
        popupOptions.put("closeButton", create(false));
        popup = new Popup(popupOptions.asAny());
        popup.setLngLat(new LngLat(lng, lat)).setText(text);
        popup.addTo(mapboxMap);
    }

    public void hidePopup() {
        if (!isMapReady())
            throw new IllegalStateException("Map not ready");
        if (popup != null)
            popup.remove();
    }

    public void showFeature(String featureSourceId, GeoJSON geoFeature) {
        if (!isMapReady())
            throw new IllegalStateException("Map not ready");
        if (mapboxMap.getSource(featureSourceId) == null)
            throw new IllegalArgumentException("Map has no such feature source: " + featureSourceId);
        LOG.fine("Showing feature on source '" + featureSourceId + "': " + geoFeature);
        mapboxMap.getSource(featureSourceId).setData(geoFeature.getObjectValue().asAny());
    }

    public void flyTo(ObjectValue coordinates) {
        if (!isMapReady())
            throw new IllegalStateException("Map not ready");
        if (coordinates == null || !coordinates.hasKey("latitude") || !coordinates.hasKey("longitude"))
            return;
        ObjectValue options = Values.createObject();
        ArrayValue center = Values.createArray();
        center.set(0, coordinates.getNumber("longitude").get());
        center.set(1, coordinates.getNumber("latitude").get());
        options.put("center", center);
        mapboxMap.flyTo(options.asAny());
    }

    protected void initFeatureLayers() {
        LOG.fine("Adding GeoJSON feature sources and layers on map load");

        ObjectValue sourceOptionsSelection = prepareSourceOptions(FEATURE_SOURCE_DROPPED_PIN);
        mapboxMap.addSource(FEATURE_SOURCE_DROPPED_PIN, sourceOptionsSelection.asAny());
        mapboxMap.addLayer(LAYER_DROPPED_PIN.asAny());

        ObjectValue sourceOptionsAll = prepareSourceOptions(FEATURE_SOURCE_CIRCLE);
        sourceOptionsAll.put("maxzoom", create(20));
        sourceOptionsAll.put("buffer", create(128));
        sourceOptionsAll.put("tolerance", create(0.375));
        sourceOptionsAll.put("cluster", create(false));
        sourceOptionsAll.put("clusterRadius", create(50));
        sourceOptionsAll.put("clusterMaxZoom", create(15));

        mapboxMap.addSource(FEATURE_SOURCE_CIRCLE, sourceOptionsAll.asAny());
        mapboxMap.addLayer(LAYER_CIRCLE.asAny(), FEATURE_LAYER_DROPPED_PIN);
    }

    protected ObjectValue prepareSourceOptions(String featureSourceId) {
        ObjectValue sourceOptions = Values.createObject();
        // Initialize with empty collection
        ObjectValue initialData = GeoJSON.EMPTY_FEATURE_COLLECTION.getObjectValue();
        LOG.fine("Preparing initial data on feature source: " + featureSourceId);
        sourceOptions.put("type", create("geojson"));
        sourceOptions.put("data", initialData);
        return sourceOptions;
    }
}
