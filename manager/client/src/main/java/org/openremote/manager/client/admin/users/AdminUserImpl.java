/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.admin.users;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.widget.FormCheckBox;
import org.openremote.manager.client.widget.FormGroup;
import org.openremote.manager.client.widget.FormViewImpl;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.gwt.dom.client.Style.Display.NONE;

public class AdminUserImpl extends FormViewImpl implements AdminUser {

    interface UI extends UiBinder<HTMLPanel, AdminUserImpl> {
    }

    @UiField
    FormGroup usernameGroup;
    @UiField
    TextBox usernameInput;

    @UiField
    FormGroup firstNameGroup;
    @UiField
    TextBox firstNameInput;

    @UiField
    FormGroup lastNameGroup;
    @UiField
    TextBox lastNameInput;

    @UiField
    FormGroup emailGroup;
    @UiField
    TextBox emailInput;

    @UiField
    FormGroup enabledGroup;
    @UiField
    FormCheckBox enabledCheckBox;

    @UiField
    FormGroup resetPasswordNoteGroup;

    @UiField
    FormGroup resetPasswordGroup;
    @UiField
    PasswordTextBox resetPasswordInput;

    @UiField
    FormGroup resetPasswordControlGroup;
    @UiField
    PasswordTextBox resetPasswordControlInput;

    @UiField
    FormGroup rolesNoteGroup;

    @UiField
    FormGroup rolesGroup;
    @UiField
    FlowPanel rolesPanel;

    @UiField
    PushButton updateButton;

    @UiField
    PushButton createButton;

    @UiField
    PushButton deleteButton;

    @UiField
    PushButton cancelButton;

    final protected Map<String, FormCheckBox> roles = new LinkedHashMap<>();

    protected Presenter presenter;

    @Inject
    public AdminUserImpl(Provider<ConfirmationDialog> confirmationDialogProvider) {
        super(confirmationDialogProvider);
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            usernameInput.setValue(null);
            firstNameInput.setValue(null);
            lastNameInput.setValue(null);
            emailInput.setValue(null);
            enabledCheckBox.setValue(false);
            resetPasswordInput.setValue(null);
            resetPasswordControlInput.setValue(null);
        }
    }

    @Override
    public void setUsername(String username) {
        usernameInput.setValue(username);
    }

    @Override
    public String getUsername() {
        return usernameInput.getValue().length() > 0 ? usernameInput.getValue() : null;
    }

    @Override
    public void setUsernameError(boolean error) {
        usernameGroup.setError(error);
    }

    @Override
    public void setUsernameEditEnabled(boolean enabled) {
        usernameInput.setEnabled(enabled);
    }

    @Override
    public void setFirstName(String firstName) {
        firstNameInput.setValue(firstName);
    }

    @Override
    public String getFirstName() {
        return firstNameInput.getValue().length() > 0 ? firstNameInput.getValue() : null;
    }

    @Override
    public void setFirstNameError(boolean error) {
        firstNameGroup.setError(error);
    }

    @Override
    public void setLastName(String lastName) {
        lastNameInput.setValue(lastName);
    }

    @Override
    public String getLastName() {
        return lastNameInput.getValue().length() > 0 ? lastNameInput.getValue() : null;
    }

    @Override
    public void setLastNameError(boolean error) {
        lastNameGroup.setError(error);
    }

    @Override
    public void setEmail(String email) {
        emailInput.setValue(email);
    }

    @Override
    public String getEmail() {
        return emailInput.getValue().length() > 0 ? emailInput.getValue() : null;
    }

    @Override
    public void setEmailError(boolean error) {
        emailGroup.setError(error);
    }

    @Override
    public void setUserEnabled(Boolean enabled) {
        enabledCheckBox.setValue(enabled != null ? enabled : false);
    }

    @Override
    public boolean getUserEnabled() {
        return enabledCheckBox.getValue();
    }

    @Override
    public void setUserEnabledError(boolean error) {
        enabledGroup.setError(error);
    }

    @Override
    public void enableResetPassword(boolean enable) {
        resetPasswordGroup.getStyle().clearDisplay();
        resetPasswordControlGroup.getStyle().clearDisplay();
        if (enable) {
            resetPasswordNoteGroup.getStyle().setDisplay(NONE);
        } else {
            resetPasswordNoteGroup.getStyle().clearDisplay();
            resetPasswordGroup.getStyle().setDisplay(NONE);
            resetPasswordControlGroup.getStyle().setDisplay(NONE);
        }
    }

    @Override
    public void enableRoles(boolean enable) {
        rolesGroup.getStyle().clearDisplay();
        if (enable) {
            rolesNoteGroup.getStyle().setDisplay(NONE);
        } else {
            rolesNoteGroup.getStyle().clearDisplay();
            rolesGroup.getStyle().setDisplay(NONE);
        }
    }

    @Override
    public String getPassword() {
        return resetPasswordInput.getValue().length() > 0 ? resetPasswordInput.getValue() : null;
    }

    @Override
    public void clearPassword() {
        resetPasswordInput.setValue(null);
    }

    @Override
    public String getPasswordControl() {
        return resetPasswordControlInput.getValue().length() > 0 ? resetPasswordControlInput.getValue() : null;
    }

    @Override
    public void clearPasswordControl() {
        resetPasswordControlInput.setValue(null);
    }

    @Override
    public void setPasswordError(boolean error) {
        resetPasswordGroup.setError(error);
        resetPasswordControlGroup.setError(error);
    }

    @Override
    public void clearRoles() {
        rolesPanel.clear();
        roles.clear();
    }

    @Override
    public void addRole(String id, String label, boolean composite, boolean assigned) {
        FlowPanel rolePanel = new FlowPanel();
        rolePanel.setStyleName("layout horizontal center");

        InlineLabel roleLabel = new InlineLabel(label);
        if (composite) {
            roleLabel.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        }

        FormCheckBox assignedCheckBox = new FormCheckBox();
        assignedCheckBox.setValue(assigned);

        assignedCheckBox.addValueChangeHandler(event -> {
            if (presenter != null) {
                presenter.onRoleAssigned(id, assignedCheckBox.getValue());
            }
        });

        rolePanel.add(assignedCheckBox);
        rolePanel.add(roleLabel);

        roles.put(id, assignedCheckBox);
        rolesPanel.add(rolePanel);
    }

    @Override
    public void toggleRoleAssigned(String id, boolean assigned) {
        if (roles.containsKey(id)) {
            roles.get(id).setValue(assigned);
        }
    }

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
    }

    @Override
    public void enableUpdate(boolean enable) {
        updateButton.setVisible(enable);
    }

    @Override
    public void enableDelete(boolean enable) {
        deleteButton.setVisible(enable);
    }

    @UiHandler("updateButton")
    void updateClicked(ClickEvent e) {
        if (presenter != null)
            presenter.update();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        if (presenter != null)
            presenter.create();
    }

    @UiHandler("deleteButton")
    void deleteClicked(ClickEvent e) {
        if (presenter != null)
            presenter.delete();
    }

    @UiHandler("cancelButton")
    void cancelClicked(ClickEvent e) {
        if (presenter != null)
            presenter.cancel();
    }
}
