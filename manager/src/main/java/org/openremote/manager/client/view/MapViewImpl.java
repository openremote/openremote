package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import elemental.json.JsonObject;
import org.openremote.manager.client.ui.Map;

import javax.inject.Inject;

public class MapViewImpl extends Composite implements MapView {

    interface UI extends UiBinder<HTMLPanel, MapViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    Map map;

    @Inject
    public MapViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void initialiseMap(JsonObject mapSettings) {
        map.setOptions(mapSettings);
    }

    @Override
    public boolean isMapInitialised() {
        return map.isInitialised();
    }
}
