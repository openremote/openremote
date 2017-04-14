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
package org.openremote.manager.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import elemental.client.Browser;
import elemental.js.util.*;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.manager.client.interop.mapbox.*;
import org.openremote.model.geo.GeoJSON;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MapWidget extends ComplexPanel {

    private static final Logger LOG = Logger.getLogger(MapWidget.class.getName());

    public interface ClickListener {
        void onClick(double lng, double lat);
    }

    // Feature IDs
    public static final String FEATURE_SOURCE_CIRCLE = "feature-source-circle";
    public static final String FEATURE_LAYER_CIRCLE = "feature-layer-circle";
    public static final String FEATURE_SOURCE_DROPPED_PIN = "feature-source-dropped-pin";
    public static final String FEATURE_LAYER_DROPPED_PIN = "feature-layer-dropped-pin";

    // Render a circle on location
    @SuppressWarnings("GwtInconsistentSerializableClass")
    static final public JsMapFromStringTo<Object> LAYER_CIRCLE = JsMapFromStringTo.create();

    // Render a dropped pin and feature title on location
    @SuppressWarnings("GwtInconsistentSerializableClass")
    static final public JsMapFromStringTo<Object> LAYER_DROPPED_PIN = JsMapFromStringTo.create();

    static {
        {
            JsMapFromStringTo<Object> layout = JsMapFromStringTo.create();
            layout.put("visibility", "visible");

            JsMapFromStringTo<Object> paint = JsMapFromStringTo.create();
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
            JsMapFromStringTo<Object> layout = JsMapFromStringTo.create();
            layout.put("visibility", "visible");

            layout.put("text-field", "{title}");
            JsArrayOfString textFont = JsArrayOfString.create();
            textFont.push("Open Sans Semibold");
            layout.put("text-font", textFont);
            JsMapFromStringTo<Object> textSize = JsMapFromStringTo.create();
            JsArrayOfInt stop1 = JsArrayOfInt.create();
            stop1.push(3);
            stop1.push(8);
            JsArrayOfInt stop2 = JsArrayOfInt.create();
            stop2.push(10);
            stop2.push(20);
            JsArrayOf<JsArrayOfInt> stops = JsArrayOf.create();
            stops.push(stop1);
            stops.push(stop2);
            textSize.put("stops", stops);
            layout.put("text-size", textSize);
            layout.put("text-allow-overlap", true);
            JsArrayOfNumber offset = JsArrayOfNumber.create();
            offset.push(0);
            offset.push(0.8);
            layout.put("text-offset", offset);

            layout.put("icon-image", "marker-15");
            offset = JsArrayOfNumber.create();
            offset.push(0);
            offset.push(-4);
            layout.put("icon-offset", offset);

            JsMapFromStringTo<Object> paint = JsMapFromStringTo.create();
            // TODO Add color to theme settings
            //paint.put("icon-color", "rgb(193, 215, 47)");
            paint.put("text-color", "#000");
            paint.put("text-halo-color", "#fff");
            paint.put("text-halo-width", 4.0);
            paint.put("text-halo-blur", 4.0);

            LAYER_DROPPED_PIN.put("id", FEATURE_LAYER_DROPPED_PIN);
            LAYER_DROPPED_PIN.put("type", "symbol");
            LAYER_DROPPED_PIN.put("source", FEATURE_SOURCE_DROPPED_PIN);
            LAYER_DROPPED_PIN.put("layout", layout);
            LAYER_DROPPED_PIN.put("paint", paint);
        }

    }

    protected FlowPanel host;
    protected String hostElementId;
    protected ClickListener clickListener;
    protected Popup popup;
    protected MapboxMap mapboxMap;
    protected boolean mapReady;

    // These sources can be initialized late, as we have to consider async arriving currentFeaturesData
    protected boolean featuresReady = false;
    protected Map<String, JsonObject> currentFeatureSourceData = new HashMap<>(); // We also hold received GeoJSON data, keyed by feature ID

    public MapWidget(JsonObject mapOptions) {
        this();
        initialise(mapOptions, null);
    }

    public MapWidget() {
        setElement(Document.get().createDivElement());
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public boolean isInitialised() {
        return mapboxMap != null;
    }

    public void initialise(JsonObject mapOptions, Runnable onLoad) {
        if (mapOptions == null) {
            return;
        }
        LOG.fine("Initializing Mapbox map");
        if (mapboxMap != null) {
            throw new IllegalStateException("Already initialized");
        }

        hostElementId = Document.get().createUniqueId();

        host = new FlowPanel();
        host.getElement().getStyle().setHeight(100, Style.Unit.PCT);
        host.getElement().setId(hostElementId);
        host.setVisible(false);
        add(host, (Element) getElement());

        mapOptions.put("container", hostElementId);
        mapOptions.put("attributionControl", false);
        mapboxMap = new MapboxMap(mapOptions);

        mapboxMap.on(EventType.LOAD, eventData -> {
            host.setVisible(true);
            resize();
            mapReady = true;
            if (onLoad != null)
                onLoad.run();
            addFeaturesOnLoad();
        });

        mapboxMap.on(EventType.CLICK, eventData -> {
            if (clickListener != null) {
                clickListener.onClick(eventData.getLngLat().getLng(), eventData.getLngLat().getLat());
            }
        });
    }

    public boolean isMapReady() {
        return mapReady;
    }

    public boolean isFeaturesReady() {
        return featuresReady;
    }

    public void addNavigationControl() {
        if (mapboxMap == null)
            return;
        mapboxMap.addControl(new NavigationControl());
    }

    public void resize() {
        if (mapboxMap == null)
            return;
        mapboxMap.resize();
        // This is not reliable, so do it again a bit later
        Browser.getWindow().setTimeout(() -> mapboxMap.resize(), 200);
    }

    public void setOpaque(boolean opaque) {
        if (mapboxMap == null)
            return;
        host.removeStyleName("opaque");
        if (opaque) {
            host.addStyleName("opaque");
        }
    }

    public void showPopup(double lng, double lat, String text) {
        if (mapboxMap == null)
            return;
        JsonObject popupOptions = Json.createObject();
        popupOptions.put("closeOnClick", Json.create(false));
        popupOptions.put("closeButton", Json.create(false));
        popup = new Popup(popupOptions);
        popup.setLngLat(new LngLat(lng, lat)).setText(text);
        popup.addTo(mapboxMap);
    }

    public void hidePopup() {
        if (popup != null)
            popup.remove();
    }

    protected JsonObject prepareSourceOptions(String featureSourceId) {
        JsonObject sourceOptions = Json.createObject();
        // We might have an async update waiting, initialize the map with this data
        JsonObject currentFeatureData = currentFeatureSourceData.remove(featureSourceId);
        if (currentFeatureData == null) {
            // Or initialize with empty collection (null doesn't work)
            currentFeatureData = GeoJSON.EMPTY_FEATURE_COLLECTION.getJsonObject();
        }
        LOG.fine("Preparing data on feature source: " + featureSourceId);
        sourceOptions.put("type", Json.create("geojson"));
        sourceOptions.put("data", currentFeatureData);
        return sourceOptions;
    }

    protected void addFeaturesOnLoad() {
        if (mapboxMap == null)
            return;
        LOG.fine("Adding GeoJSON feature sources and layers on map load");

        JsonObject sourceOptionsSelection = prepareSourceOptions(FEATURE_SOURCE_DROPPED_PIN);
        mapboxMap.addSource(FEATURE_SOURCE_DROPPED_PIN, sourceOptionsSelection);
        mapboxMap.addLayer(LAYER_DROPPED_PIN);

        JsonObject sourceOptionsAll = prepareSourceOptions(FEATURE_SOURCE_CIRCLE);
        sourceOptionsAll.put("maxzoom", Json.create(20));
        sourceOptionsAll.put("buffer", Json.create(128));
        sourceOptionsAll.put("tolerance", Json.create(0.375));
        sourceOptionsAll.put("cluster", Json.create(false));
        sourceOptionsAll.put("clusterRadius", Json.create(50));
        sourceOptionsAll.put("clusterMaxZoom", Json.create(15));

        // TODO something weird is going on with JS interop, this helps for now
        sourceOptionsAll = Json.parse(sourceOptionsAll.toJson());

        mapboxMap.addSource(FEATURE_SOURCE_CIRCLE, sourceOptionsAll);
        mapboxMap.addLayer(LAYER_CIRCLE, FEATURE_LAYER_DROPPED_PIN);

        featuresReady = true;
    }

    public void showFeature(String featureSourceId, GeoJSON geoFeature) {
        LOG.fine("Showing feature on source " + featureSourceId + ": " + geoFeature);
        JsonObject data = geoFeature.getJsonObject();
        currentFeatureSourceData.put(featureSourceId, data);
        if (featuresReady) {
            data = currentFeatureSourceData.remove(featureSourceId);
            if (data != null && mapboxMap != null)
                mapboxMap.getSource(featureSourceId).setData(data);
        }
    }

    public void flyTo(double[] coordinates) {
        if (coordinates == null || coordinates.length != 2)
            return;
        JsonObject json = Json.createObject();
        JsonArray center = Json.createArray();
        center.set(0, coordinates[0]);
        center.set(1, coordinates[1]);
        json.put("center", center);
        Scheduler.get().scheduleDeferred(() -> {
            if (mapboxMap == null)
                return;
            mapboxMap.flyTo(json);
        });
    }
}
