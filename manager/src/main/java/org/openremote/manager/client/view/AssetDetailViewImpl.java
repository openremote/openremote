package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

import javax.inject.Inject;

public class AssetDetailViewImpl extends Composite implements AssetDetailView {

    interface UI extends UiBinder<ScrollPanel, AssetDetailViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @Inject
    public AssetDetailViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
}
