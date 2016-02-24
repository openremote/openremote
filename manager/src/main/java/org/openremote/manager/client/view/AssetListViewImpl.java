package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

import javax.inject.Inject;

public class AssetListViewImpl extends Composite implements AssetListView {

    interface UI extends UiBinder<ScrollPanel, AssetListViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @Inject
    public AssetListViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
}
