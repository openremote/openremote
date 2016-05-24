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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.*;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.inject.Inject;
import java.util.Arrays;

public class AdminRealmImpl extends Composite implements AdminRealms {

    interface UI extends UiBinder<HTMLPanel, AdminRealmImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    LabelElement realmsListBoxLabel;
    @UiField
    ListBox realmsListBox;

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

    final CellTable<RealmRepresentation> realmTable = new CellTable<>();

    @Inject
    public AdminRealmImpl() {
        initWidget(ui.createAndBindUi(this));

        cellTableContainer.add(realmTable);
        realmTable.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.ENABLED);

        TextHeader textHeader = new TextHeader("headerTitle");
        textHeader.setHeaderStyleNames("my-style");
        TextColumn<RealmRepresentation> nameColumn = new TextColumn<RealmRepresentation>() {
            @Override
            public String getValue(RealmRepresentation realmRepresentation) {
                return realmRepresentation.getDisplayName();
            }
        };
        realmTable.addColumn(nameColumn, textHeader);

        TextColumn<RealmRepresentation> foo = new TextColumn<RealmRepresentation>() {
            @Override
            public String getValue(RealmRepresentation realmRepresentation) {
                return realmRepresentation.getRealm();
            }
        };
        realmTable.addColumn(foo, "Realm");

        realmsListBoxLabel.setHtmlFor(Document.get().createUniqueId());
        realmsListBox.getElement().setId(realmsListBoxLabel.getHtmlFor());

        realmNameInputLabel.setHtmlFor(Document.get().createUniqueId());
        realmNameInput.getElement().setId(realmNameInputLabel.getHtmlFor());

        realmEnabledLabel.setHtmlFor(Document.get().createUniqueId());
        realmEnabledCheckBox.getElement().setId(realmEnabledLabel.getHtmlFor());

        realmEnabledCheckBox.setValue(true);
        realmEnabledCheckBox.setEnabled(false);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setRealms(RealmRepresentation[] realms) {
        realmsListBox.clear();
        for (RealmRepresentation realm : realms) {
            realmsListBox.addItem(realm.getDisplayName());
        }

        realmTable.setRowData(0, Arrays.asList(realms));

    }
}
