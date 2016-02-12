package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;
import org.openremote.manager.client.presenter.AssetsPlace;
import org.openremote.manager.client.presenter.MapPlace;

/**
 * Created by Richard on 12/02/2016.
 */
public class MainMenuViewImpl extends Composite implements MainMenuView {
    interface UI extends UiBinder<FlowPanel, MainMenuViewImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private Presenter presenter;

    @UiField
    Button mapButton;

    @UiField
    Button assetsButton;

    @UiHandler("mapButton")
    void defaultLayoutClicked(ClickEvent e) {
        presenter.goTo(new MapPlace());
    }

    @UiHandler("assetsButton")
    void vmdLayoutClicked(ClickEvent e) {
        presenter.goTo(new AssetsPlace());
    }

    @Inject
    public MainMenuViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
}
