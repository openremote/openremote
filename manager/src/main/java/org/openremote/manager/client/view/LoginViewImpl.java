package org.openremote.manager.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.AlertType;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.service.ValidatorService;

import javax.inject.Inject;

public class LoginViewImpl implements LoginView {
    interface UI extends UiBinder<Modal, LoginViewImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private Presenter presenter;
    private ValidatorService validators;

    @UiField
    Modal modal;

    @UiField
    Button btnSubmit;

    @UiField
    TextBox inputUsername;

    @UiField
    Input inputPassword;

    @UiField
    Form formLogin;

    @UiField
    Alert alertLogin;

    @UiField(provided = true)
    ManagerConstants constants;

    @UiHandler("btnSubmit")
    void onClickSubmit(ClickEvent e) {
        if (!formLogin.validate(true)) {
            return;
        }

        presenter.doLogin();
    }

    @Inject
    public LoginViewImpl(ManagerConstants constants, ValidatorService validators) {
        this.constants = constants;
        this.validators = validators;
        modal = ui.createAndBindUi(this);

        inputUsername.setValidators(validators.getBlankFieldValidator());
        inputPassword.setValidators(validators.getBlankFieldValidator());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public String getUsername() {
        return inputUsername.getValue();
    }

    @Override
    public String getPassword() {
        return inputPassword.getValue();
    }

    @Override
    public void showAlert(AlertType alertType, String alert) {
        alertLogin.setType(alertType);
        alertLogin.setText(alert);
        alertLogin.setVisible(true);
    }

    public void hideAlert() {
        alertLogin.setVisible(false);
    }

    @Override
    public void setLoginInProgress(boolean isBusy) {
        if (isBusy) {
            btnSubmit.setIcon(IconType.SPINNER);
            btnSubmit.setIconSpin(true);
            btnSubmit.setEnabled(false);
        } else {
            btnSubmit.setIcon(null);
            btnSubmit.setIconSpin(false);
            btnSubmit.setEnabled(true);
        }
    }

    @Override
    public void hide() {
        inputUsername.setValue("");
        inputPassword.setValue("");
        modal.hide();
    }

    @Override
    public void show() {
        modal.show();
    }

    @Override
    public Widget asWidget() {
        return modal;
    }
}
