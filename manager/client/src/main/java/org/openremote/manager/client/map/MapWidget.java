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
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import elemental.js.util.*;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.client.interop.mapbox.EventType;
import org.openremote.manager.client.interop.mapbox.GeoJSONSource;
import org.openremote.manager.client.interop.mapbox.Map;
import org.openremote.manager.client.interop.mapbox.Navigation;

import java.util.logging.Logger;

public class MapWidget extends ComplexPanel {

    private static final Logger LOG = Logger.getLogger(MapWidget.class.getName());

    public static final String FEATURES_SOURCE = "features";
    public static final String FEATURES_LAYER_TITLE = "features-title";
    public static final String FEATURES_LAYER_LOCATION = "features-location";

    protected FlowPanel host;
    protected String id;
    protected Map map;
    protected GeoJSONSource featuresSource;
    protected JsMapFromStringTo<Object> featuresLayerTitle; // Shows asset display name on asset location
    protected JsMapFromStringTo<Object> featuresLayerLocation; // Shows a circle on asset location
    protected JsonObject pendingFeaturesData;

    public MapWidget(JsonObject mapOptions) {
        this();
        initialise(mapOptions);
    }

    public MapWidget() {
        setElement(Document.get().createDivElement());
    }

    public Map getMap() {
        return map;
    }

    public boolean isInitialised() {
        return map != null;
    }

    public void initialise(JsonObject mapOptions) {
        if (map != null) {
            map.remove();
            remove(host);
        }

        if (mapOptions == null) {
            return;
        }

        id = Document.get().createUniqueId();

        host = new FlowPanel();
        host.setStyleName("flex");
        host.getElement().setId(id);
        add(host, (Element) getElement());

        mapOptions.put("container", id);
        map = new Map(mapOptions);

        map.addControl(new Navigation());

        map.on(EventType.LOAD, eventData -> {
            addFeatures();
        });

        /*
        JsMapFromStringTo<Object> popupOptions = JsMapFromStringTo.create();
        popupOptions.put("closeOnClick", false);
        popup = new Popup(popupOptions.cast())
            .addTo(map);
        */

        map.on(EventType.CLICK, eventData -> {
            LOG.info("### COORDS: " + eventData.getLngLat());
            LOG.info("### BOUNDS: " + map.getBounds());
        });

    }

    public void refresh() {
        // TODO: we might want to further define what "refresh" means, currently it's triggered on GoToPlaceEvent
        if (map != null) {
            map.resize();
        }
    }

    protected void addFeatures() {
        JsonObject sourceOptions = Json.createObject();

        // We might have an async update waiting, initialize the map with this data
        if (pendingFeaturesData != null) {
            sourceOptions.put("data", pendingFeaturesData);
            pendingFeaturesData = null;
        }

        /*
        sourceOptions.put("maxzoom", 20);
        sourceOptions.put("buffer", 128);
        sourceOptions.put("tolerance", 0.375);
        sourceOptions.put("cluster", false);
        sourceOptions.put("clusterRadius", 50);
        sourceOptions.put("clusterMaxZoom", 15);
        */

        featuresSource = new GeoJSONSource(sourceOptions);
        map.addSource(FEATURES_SOURCE, featuresSource);

        featuresLayerTitle = createFeaturesLayerForTitle(FEATURES_LAYER_TITLE, FEATURES_SOURCE);
        map.addLayer(featuresLayerTitle);

        featuresLayerLocation = createFeaturesLayerForLocation(FEATURES_LAYER_LOCATION, FEATURES_SOURCE);
        map.addLayer(featuresLayerLocation, FEATURES_LAYER_TITLE);
    }

    public void showFeatures(String mapFeaturesJson) {
        if (featuresSource != null) {
            featuresSource.setData(Json.parse(mapFeaturesJson));
        } else {
            pendingFeaturesData = Json.parse(mapFeaturesJson);
        }
    }

    protected JsMapFromStringTo<Object> createFeaturesLayerForTitle(String id, String source) {

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
        offset.push(1.2);
        layout.put("text-offset", offset);

        JsMapFromStringTo<Object> paint = JsMapFromStringTo.create();
        paint.put("text-color", "rgb(0, 0, 0)");
        paint.put("text-halo-color", "rgb(255, 255, 255)");
        paint.put("text-halo-width", 1.2);
        paint.put("text-halo-blur", 0.4);

        JsMapFromStringTo<Object> layer = JsMapFromStringTo.create();
        layer.put("id", id);
        layer.put("type", "symbol");
        layer.put("source", source);
        layer.put("layout", layout);
        layer.put("paint", paint);

        return layer;
    }

    protected JsMapFromStringTo<Object> createFeaturesLayerForLocation(String id, String source) {

        JsMapFromStringTo<Object> layout = JsMapFromStringTo.create();
        layout.put("visibility", "visible");

        JsMapFromStringTo<Object> paint = JsMapFromStringTo.create();
        paint.put("circle-radius", 15.0);
        paint.put("circle-opacity", 0.8);
        paint.put("circle-color", "rgb(193, 215, 47)");

        JsMapFromStringTo<Object> layer = JsMapFromStringTo.create();
        layer.put("id", id);
        layer.put("type", "circle");
        layer.put("source", source);
        layer.put("layout", layout);
        layer.put("paint", paint);

        return layer;
    }
}
