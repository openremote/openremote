package org.openremote.manager.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import elemental.json.JsonObject;

/**
 * Created by Richard on 24/02/2016.
 */
public class Map extends ComplexPanel {
    boolean initialised = false;

    public Map() {
        setElement(Document.get().createDelElement());
    }

    @Override
    protected void onLoad() {
        resize();
        super.onLoad();
    }

    public void setOptions(JsonObject mapOptions) {
        initMap(getElement(), mapOptions);
        resize();
    }

    public boolean isInitialised() {
        return initialised;
    }

    private native void resize() /*-{
        if ($wnd.map === null || typeof($wnd.map) === 'undefined') {
            return;
        }
        $wnd.map.resize();
    }-*/;

    private native void initMap(Element mapContainer, JsonObject mapSettings) /*-{
        mapSettings.container = mapContainer;
        var map = new $wnd.mapboxgl.Map(mapSettings);
        map.addControl(new $wnd.mapboxgl.Navigation());
        // Add map to window scope as no obvious way of getting it later
        $wnd.map = map;
    }-*/;
}
