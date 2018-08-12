/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.app.client.admin.users.edit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Provider;
import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.users.AdminUsersNavigation;
import org.openremote.app.client.app.dialog.Confirmation;
import org.openremote.app.client.widget.*;
import org.openremote.app.client.widget.PushButton;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminUserEditImpl extends FormViewImpl implements AdminUserEdit {

    interface UI extends UiBinder<HTMLPanel, AdminUserEditImpl> {
    }

    @UiField(provided = true)
    AdminUsersNavigation adminUsersNavigation;

    @UiField
    Headline headline;

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
    Label resetPasswordNote;

    @UiField
    FormGroup resetPasswordGroup;
    @UiField
    PasswordTextBox resetPasswordInput;

    @UiField
    FormGroup resetPasswordControlGroup;
    @UiField
    PasswordTextBox resetPasswordControlInput;

    @UiField
    Label rolesNote;

    @UiField
    FormGroup rolesGroup;
    @UiField
    FlowPanel rolesPanel;

    @UiField
    FlowPanel registeredDevicesContainer;

    @UiField
    PushButton updateButton;

    @UiField
    PushButton createButton;

    @UiField
    PushButton deleteButton;

    @UiField
    FormButton refreshDeviceRegistrations;

    final protected Map<String, FormCheckBox> roles = new LinkedHashMap<>();

    protected Presenter presenter;

    @Inject
    public AdminUserEditImpl(Environment environment,
                             AdminUsersNavigation adminUsersNavigation) {
        super(environment.getWidgetStyle());

        this.adminUsersNavigation = adminUsersNavigation;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        clearRegisteredDevices(false);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            adminUsersNavigation.setVisible(false);
            adminUsersNavigation.reset();
            headline.setSub(null);
            usernameInput.setValue(null);
            firstNameInput.setValue(null);
            lastNameInput.setValue(null);
            emailInput.setValue(null);
            enabledCheckBox.setValue(false);
            resetPasswordInput.setValue(null);
            resetPasswordControlInput.setValue(null);
            clearRegisteredDevices(false);
            refreshDeviceRegistrations.setVisible(false);
        } else {
            adminUsersNavigation.setActive(presenter.getPlace());
            adminUsersNavigation.setVisible(true);
        }
    }

    @Override
    public void setTenantName(String realm) {
        headline.setSub(managerMessages.tenant() + ": " + realm);
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
    public void setEditMode(boolean editMode) {
        usernameInput.setEnabled(!editMode);
        clearRegisteredDevices(editMode);
        refreshDeviceRegistrations.setVisible(editMode);
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
        resetPasswordNote.setVisible(!enable);
        resetPasswordGroup.setVisible(enable);
        resetPasswordControlGroup.setVisible(enable);
    }

    @Override
    public void enableRoles(boolean enable) {
        rolesNote.setVisible(!enable);
        rolesGroup.setVisible(enable);
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

    protected void clearRegisteredDevices(boolean addEmptyMessage) {
       registeredDevicesContainer.clear();
        if (addEmptyMessage) {
            Label emptyLabel = new Label(managerMessages.noRegisteredDevices());
            emptyLabel.addStyleName(widgetStyle.FormListEmptyMessage());
            registeredDevicesContainer.add(emptyLabel);
        }
    }
}
