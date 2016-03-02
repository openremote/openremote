package org.openremote.manager.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.web.bindery.event.shared.HandlerRegistration;
import elemental.json.JsonObject;
import org.openremote.manager.client.interop.mapbox.*;

public class MapWidget extends ComplexPanel {
    ResizeLayoutPanel host;
    HandlerRegistration resizeHandler;
    private String id;
    private org.openremote.manager.client.interop.mapbox.Map map;

    public MapWidget(JsonObject mapOptions) {
        this();
        initialise(mapOptions);
    }

    public MapWidget() {
        setElement(Document.get().createDivElement());
    }

    public Map getJsMap() {
        return map;
    }

    public void initialise(JsonObject mapOptions) {
        if (map != null) {
            map.remove();
            resizeHandler.removeHandler();
            remove(host);
        }

        if (mapOptions == null) {
            return;
        }

        id = Document.get().createUniqueId();
        host = new ResizeLayoutPanel();
        host.setWidth("100%");
        host.setHeight("100%");
        host.getElement().setId(id);
        resizeHandler = host.addResizeHandler(event -> {
            resizeMap();
        });
        add(host, (Element)getElement());
        mapOptions.put("container", id);
        map = new Map(mapOptions);
        map.addControl(new Navigation());
        map.on(EventType.CLICK, eventData -> {
            CameraOptions opts = new CameraOptions();
            opts.center = eventData.getLngLat();
            map.jumpTo(opts);
        });
    }

    public boolean isInitialised() {
        return map != null;
    }

    private void resizeMap() {
        if (map != null) {
            map.resize();
        }
    }
}
