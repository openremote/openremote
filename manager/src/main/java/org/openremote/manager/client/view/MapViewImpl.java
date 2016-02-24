package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import elemental.json.JsonObject;

import javax.inject.Inject;

public class MapViewImpl extends Composite implements MapView {

    interface UI extends UiBinder<HTMLPanel, MapViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    HTMLPanel mapContainer;

    @Inject
    public MapViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void showMap(JsonObject mapSettings) {
        showMap(mapSettings, mapContainer.getElement());
    }

    public native void showMap(JsonObject mapSettings, Element mapContainer) /*-{
        mapSettings.container = mapContainer;
        var map = new $wnd.mapboxgl.Map(mapSettings);
        map.addControl(new $wnd.mapboxgl.Navigation());
    }-*/;

}
