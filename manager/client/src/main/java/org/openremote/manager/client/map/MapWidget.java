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
package org.openremote.manager.client.map;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import elemental.js.util.*;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.client.interop.mapbox.EventType;
import org.openremote.manager.client.interop.mapbox.GeoJSONSource;
import org.openremote.manager.client.interop.mapbox.MapboxMap;
import org.openremote.manager.client.interop.mapbox.Navigation;
import org.openremote.manager.shared.map.GeoJSON;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MapWidget extends ComplexPanel {

    private static final Logger LOG = Logger.getLogger(MapWidget.class.getName());

    // Feature IDs
    public static final String FEATURES_SOURCE_ALL = "features-source-all";
    public static final String FEATURES_LAYER_ALL = "features-layer-all";
    public static final String FEATURES_SOURCE_SELECTION = "features-source-selection";
    public static final String FEATURES_LAYER_SELECTION = "features-layer-selection";

    // Render a circle on location
    @SuppressWarnings("GwtInconsistentSerializableClass")
    static final public JsMapFromStringTo<Object> LAYER_ALL = JsMapFromStringTo.create();

    // Render a dropped pin and feature title on location
    @SuppressWarnings("GwtInconsistentSerializableClass")
    static final public JsMapFromStringTo<Object> LAYER_SELECTION = JsMapFromStringTo.create();

    static {
        {
            JsMapFromStringTo<Object> layout = JsMapFromStringTo.create();
            layout.put("visibility", "visible");

            JsMapFromStringTo<Object> paint = JsMapFromStringTo.create();
            paint.put("circle-radius", 12.0); // This MUST be double!
            paint.put("circle-opacity", 0.5);
            // TODO Add color to theme settings
            paint.put("circle-color", "rgb(73, 169, 66)");

            LAYER_ALL.put("id", FEATURES_LAYER_ALL);
            LAYER_ALL.put("type", "circle");
            LAYER_ALL.put("source", FEATURES_SOURCE_ALL);
            LAYER_ALL.put("layout", layout);
            LAYER_ALL.put("paint", paint);
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

            LAYER_SELECTION.put("id", FEATURES_LAYER_SELECTION);
            LAYER_SELECTION.put("type", "symbol");
            LAYER_SELECTION.put("source", FEATURES_SOURCE_SELECTION);
            LAYER_SELECTION.put("layout", layout);
            LAYER_SELECTION.put("paint", paint);
        }

    }

    protected FlowPanel host;
    protected String hostElementId;

    protected MapboxMap mapboxMap;

    // These sources can be initialized late, as we have to consider async arriving currentFeaturesData
    protected boolean featuresReady = false;
    protected Map<String, GeoJSONSource> featureSources = new HashMap<>(); // Holds the displayed GeoJSON data
    protected Map<String, JsonObject> currentFeatureSourceData = new HashMap<>(); // We also hold received GeoJSON data, keyed by feature ID

    public MapWidget(JsonObject mapOptions) {
        this();
        initialise(mapOptions);
    }

    public MapWidget() {
        setElement(Document.get().createDivElement());
    }

    public MapboxMap getMapboxMap() {
        return mapboxMap;
    }

    public boolean isInitialised() {
        return mapboxMap != null;
    }

    public void initialise(JsonObject mapOptions) {
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
        add(host, (Element) getElement());

        mapOptions.put("container", hostElementId);
        mapboxMap = new MapboxMap(mapOptions);

        mapboxMap.addControl(new Navigation());

        mapboxMap.on(EventType.LOAD, eventData -> {
            addFeaturesOnLoad();
        });

        /*
        JsMapFromStringTo<Object> popupOptions = JsMapFromStringTo.create();
        popupOptions.put("closeOnClick", false);
        popup = new Popup(popupOptions.cast())
            .addTo(map);
        */

        mapboxMap.on(EventType.CLICK, eventData -> {
            LOG.info("### COORDS: " + eventData.getLngLat());
            LOG.info("### BOUNDS: " + mapboxMap.getBounds());
        });

    }

    public void refresh() {
        // TODO: we might want to further define what "refresh" means, currently it's triggered on GoToPlaceEvent
        if (mapboxMap != null) {
            mapboxMap.resize();
        }
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
        sourceOptions.put("data", currentFeatureData);
        return sourceOptions;
    }

    protected void addFeaturesOnLoad() {
        LOG.fine("Adding GeoJSON feature sources and layers on map load");

        JsonObject sourceOptionsSelection = prepareSourceOptions(FEATURES_SOURCE_SELECTION);
        GeoJSONSource sourceSelection = new GeoJSONSource(sourceOptionsSelection);
        featureSources.put(FEATURES_SOURCE_SELECTION, sourceSelection);
        mapboxMap.addSource(FEATURES_SOURCE_SELECTION, sourceSelection);
        mapboxMap.addLayer(LAYER_SELECTION);

        JsonObject sourceOptionsAll = prepareSourceOptions(FEATURES_SOURCE_ALL);
        sourceOptionsAll.put("maxzoom", 20);
        sourceOptionsAll.put("buffer", 128);
        sourceOptionsAll.put("tolerance", 0.375);
        sourceOptionsAll.put("cluster", false);
        sourceOptionsAll.put("clusterRadius", 50);
        sourceOptionsAll.put("clusterMaxZoom", 15);
        GeoJSONSource sourceAll = new GeoJSONSource(sourceOptionsAll);
        featureSources.put(FEATURES_SOURCE_ALL, sourceAll);
        mapboxMap.addSource(FEATURES_SOURCE_ALL, sourceAll);
        mapboxMap.addLayer(LAYER_ALL, FEATURES_LAYER_SELECTION);

        featuresReady = true;
    }

    public void showFeatures(String featureSourceId, GeoJSON mapFeatures) {
        LOG.fine("Showing features on source " + featureSourceId + ": " + mapFeatures);
        JsonObject data = mapFeatures.getJsonObject();
        currentFeatureSourceData.put(featureSourceId, data);
        if (featuresReady) {
            data = currentFeatureSourceData.remove(featureSourceId);
            if (data != null && featureSources.containsKey(featureSourceId)) {
                featureSources.get(featureSourceId).setData(data);
            }
        }
    }
}
