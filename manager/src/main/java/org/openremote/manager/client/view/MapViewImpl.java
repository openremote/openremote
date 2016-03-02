package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import elemental.json.JsonObject;
import org.openremote.manager.client.interop.mapbox.MapOptions;
import org.openremote.manager.client.ui.MapWidget;

import javax.inject.Inject;

public class MapViewImpl extends Composite implements MapView {

    interface UI extends UiBinder<MapWidget, MapViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    MapWidget mapWidget;

    @Inject
    public MapViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void initialiseMap(JsonObject mapOptions) {
        mapWidget.initialise(mapOptions);
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }
}
