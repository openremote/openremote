package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

import javax.inject.Inject;

public class MainViewImpl extends Composite implements MainView {

    interface UI extends UiBinder<DockLayoutPanel, MainViewImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private MainMenuView mainMenuView;

    @UiField
    SimpleLayoutPanel headerPanel;

    @UiField
    SimpleLayoutPanel contentPanel;

    Presenter presenter;

    @Inject
    public MainViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setMenu(MainMenuView mainMenuView) {
        this.mainMenuView = mainMenuView;
        headerPanel.add(mainMenuView);
    }


    @Override
    public AcceptsOneWidget getMainContentPanel() {
        return contentPanel;
    }

}
