package org.openremote.manager.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

import javax.inject.Inject;

public class LeftSideViewImpl extends Composite implements LeftSideView {

    interface UI extends UiBinder<ScrollPanel, LeftSideViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @Inject
    public LeftSideViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
}
