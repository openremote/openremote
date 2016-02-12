package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

import javax.inject.Inject;

public class LoginViewImpl extends Composite implements LoginView {

    interface UI extends UiBinder<DockLayoutPanel, LoginViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    @UiField
    SimpleLayoutPanel headerPanel;

    @UiField
    SimpleLayoutPanel contentPanel;

    Presenter presenter;

    @Inject
    public LoginViewImpl() {
        initWidget(ui.createAndBindUi(this));

        headerPanel.add(new Label("THIS IS THE HEADER PANEL IN THE MAIN VIEW"));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public AcceptsOneWidget getContentPanel() {
        return contentPanel;
    }

}
