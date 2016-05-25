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
package org.openremote.manager.client.admin.realms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;

public class AdminRealmImpl extends Composite implements AdminRealm {

    interface UI extends UiBinder<HTMLPanel, AdminRealmImpl> {
    }

    private UI ui = GWT.create(UI.class);

    @UiField
    LabelElement realmDisplayNameInputLabel;
    @UiField
    TextBox realmDisplayNameInput;

    @UiField
    LabelElement realmNameInputLabel;
    @UiField
    TextBox realmNameInput;

    @UiField
    LabelElement realmEnabledLabel;
    @UiField
    SimpleCheckBox realmEnabledCheckBox;

    @UiField
    SimplePanel cellTableContainer;

    @UiField
    PushButton updateRealmButton;

    @UiField
    PushButton createRealmButton;

    @UiField
    PushButton deleteRealmButton;

    @UiField
    PushButton cancelButton;

    protected Presenter presenter;
    protected RealmRepresentation realm;

    @Inject
    public AdminRealmImpl() {
        initWidget(ui.createAndBindUi(this));

        realmDisplayNameInputLabel.setHtmlFor(Document.get().createUniqueId());
        realmDisplayNameInput.getElement().setId(realmDisplayNameInputLabel.getHtmlFor());

        realmNameInputLabel.setHtmlFor(Document.get().createUniqueId());
        realmNameInput.getElement().setId(realmNameInputLabel.getHtmlFor());

        realmEnabledLabel.setHtmlFor(Document.get().createUniqueId());
        realmEnabledCheckBox.getElement().setId(realmEnabledLabel.getHtmlFor());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setRealm(RealmRepresentation realm) {
        this.realm = realm;
        writeForm();
    }

    @UiHandler("updateRealmButton")
    void updateRealmClicked(ClickEvent e) {
        readForm();
        presenter.updateRealm(realm);
    }

    @UiHandler("createRealmButton")
    void createRealmClicked(ClickEvent e) {
        readForm();
        presenter.createRealm(realm);
    }

    @UiHandler("deleteRealmButton")
    void deleteClicked(ClickEvent e) {
        presenter.deleteRealm(realm);
        realm = null;
        writeForm();
    }

    @UiHandler("cancelButton")
    void cancelClicked(ClickEvent e) {
        realm = null;
        writeForm();
        presenter.cancel();
    }

    void writeForm() {
        updateRealmButton.setVisible(false);
        deleteRealmButton.setVisible(false);
        createRealmButton.setVisible(false);
        if (realm != null) {
            if (realm.getId() != null) {
                updateRealmButton.setVisible(true);
                deleteRealmButton.setVisible(true);
            } else {
                createRealmButton.setVisible(true);
            }
            realmDisplayNameInput.setText(realm.getDisplayName());
            realmNameInput.setText(realm.getRealm());
            realmEnabledCheckBox.setValue(realm.isEnabled());
        } else {
            realmDisplayNameInput.setText(null);
            realmEnabledCheckBox.setValue(false);
        }
    }

    void readForm() {
        realm.setDisplayName(realmDisplayNameInput.getText());
        realm.setRealm(realmNameInput.getText());
        realm.setEnabled(realmEnabledCheckBox.getValue());
    }

}
