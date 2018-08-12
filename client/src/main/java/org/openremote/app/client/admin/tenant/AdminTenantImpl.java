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
package org.openremote.app.client.admin.tenant;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Provider;
import org.openremote.app.client.Environment;
import org.openremote.app.client.app.dialog.Confirmation;
import org.openremote.app.client.widget.FormCheckBox;
import org.openremote.app.client.widget.FormGroup;
import org.openremote.app.client.widget.FormViewImpl;
import org.openremote.app.client.widget.PushButton;

import javax.inject.Inject;

public class AdminTenantImpl extends FormViewImpl implements AdminTenant {

    interface UI extends UiBinder<HTMLPanel, AdminTenantImpl> {
    }

    @UiField
    FormGroup displayNameGroup;
    @UiField
    TextBox displayNameInput;

    @UiField
    FormGroup realmGroup;
    @UiField
    TextBox realmInput;

    @UiField
    FormGroup enabledGroup;
    @UiField
    FormCheckBox enabledCheckBox;

    @UiField
    PushButton createButton;

    @UiField
    PushButton updateButton;

    @UiField
    PushButton deleteButton;

    @UiField
    PushButton cancelButton;

    protected Presenter presenter;

    @Inject
    public AdminTenantImpl(Environment environment) {
        super(environment.getWidgetStyle());
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            displayNameInput.setValue(null);
            realmInput.setValue(null);
            enabledCheckBox.setValue(false);
        }
    }

    @Override
    public void setTenantDisplayName(String displayName) {
        displayNameInput.setValue(displayName);
    }

    @Override
    public String getTenantDisplayName() {
        return displayNameInput.getValue().length() > 0 ? displayNameInput.getValue() : null;
    }

    @Override
    public void setTenantDisplayNameError(boolean error) {
        displayNameGroup.setError(error);
    }

    @Override
    public void setTenantRealm(String realm) {
        realmInput.setValue(realm);
    }

    @Override
    public String getTenantRealm() {
        return realmInput.getValue().length() > 0 ? realmInput.getValue() : null;
    }

    @Override
    public void setTenantRealmError(boolean error) {
        realmGroup.setError(error);
    }

    @Override
    public void setTenantEnabled(Boolean enabled) {
        enabledCheckBox.setValue(enabled != null ? enabled : false);
    }

    @Override
    public boolean getTenantEnabled() {
        return enabledCheckBox.getValue();
    }

    @Override
    public void setTenantEnabledError(boolean error) {
        enabledGroup.setError(error);
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
