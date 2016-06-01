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
package org.openremote.manager.client.admin.tenant;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TextBox;
import org.openremote.manager.client.widget.FormView;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;

public class AdminTenantImpl extends FormView implements AdminTenant {

    interface UI extends UiBinder<HTMLPanel, AdminTenantImpl> {
    }

    @UiField
    DivElement displayNameGroup;
    @UiField
    LabelElement displayNameLabel;
    @UiField
    TextBox displayNameInput;

    @UiField
    DivElement realmGroup;
    @UiField
    LabelElement realmLabel;
    @UiField
    TextBox realmInput;

    @UiField
    DivElement enabledGroup;
    @UiField
    LabelElement enabledLabel;
    @UiField
    SimpleCheckBox enabledCheckBox;

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
    public AdminTenantImpl() {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        displayNameLabel.setHtmlFor(Document.get().createUniqueId());
        displayNameInput.getElement().setId(displayNameLabel.getHtmlFor());

        realmLabel.setHtmlFor(Document.get().createUniqueId());
        realmInput.getElement().setId(realmLabel.getHtmlFor());

        enabledLabel.setHtmlFor(Document.get().createUniqueId());
        enabledCheckBox.getElement().setId(enabledLabel.getHtmlFor());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
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
        displayNameGroup.removeClassName("error");
        if (error) {
            displayNameGroup.addClassName("error");
        }
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
        realmGroup.removeClassName("error");
        if (error) {
            realmGroup.addClassName("error");
        }
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
        enabledGroup.removeClassName("error");
        if (error) {
            enabledGroup.addClassName("error");
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
        presenter.update();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        presenter.create();
    }

    @UiHandler("deleteButton")
    void deleteClicked(ClickEvent e) {
        presenter.delete();
    }

    @UiHandler("cancelButton")
    void cancelClicked(ClickEvent e) {
        presenter.cancel();
    }
}
