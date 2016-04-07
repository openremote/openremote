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
