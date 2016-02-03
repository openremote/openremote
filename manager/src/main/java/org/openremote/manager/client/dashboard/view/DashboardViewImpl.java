package org.openremote.manager.client.dashboard.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;

import javax.inject.Inject;

public class DashboardViewImpl extends Composite implements DashboardView {

    interface UI extends UiBinder<ScrollPanel, DashboardViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    Button helloButton;

    @UiField
    Label helloLabel;

    @Inject
    public DashboardViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setHelloText(String text) {
        helloLabel.setText(text);
    }

    @UiHandler("helloButton")
    public void handleHelloButton(ClickEvent event) {
        presenter.getHelloText();
    }
}
