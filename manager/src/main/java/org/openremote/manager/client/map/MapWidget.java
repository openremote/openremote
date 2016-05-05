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
import elemental.json.JsonObject;
import org.openremote.manager.client.interop.mapbox.Map;
import org.openremote.manager.client.interop.mapbox.Navigation;

public class MapWidget extends ComplexPanel {

    protected FlowPanel host;
    protected String id;
    protected Map map;

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

        /* TODO conflicts with double click-zoom on map behavior
        map.on(EventType.CLICK, eventData -> {
            CameraOptions opts = new CameraOptions();
            opts.center = eventData.getLngLat();
            map.jumpTo(opts);
        });
        */
    }
}
