package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

import javax.inject.Inject;

public class MapViewImpl extends Composite implements MapView {

    interface UI extends UiBinder<ScrollPanel, MapViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @Inject
    public MapViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
}
