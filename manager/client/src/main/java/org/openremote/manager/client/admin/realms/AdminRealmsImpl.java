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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminRealmsImpl extends Composite implements AdminRealms {

    interface UI extends UiBinder<HTMLPanel, AdminRealmsImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    AdminRealmsTable.Style realmsTableStyle;

    @UiField
    PushButton createRealmButton;

    @UiField
    SimplePanel cellTableContainer;

    final AdminRealmsTable realmsTable;

    @Inject
    public AdminRealmsImpl(ManagerConstants managerConstants,
                           FormTableStyle formTableStyle) {
        initWidget(ui.createAndBindUi(this));

        realmsTable = new AdminRealmsTable(managerConstants, realmsTableStyle, formTableStyle);
        realmsTable.getSelectionModel().addSelectionChangeHandler(event -> {
                RealmRepresentation selected;
                if ((selected = realmsTable.getSelectedObject()) != null
                    && presenter != null) {
                    presenter.onRealmSelected(selected);
                }
            }
        );
        cellTableContainer.add(realmsTable);

        //setTestData();
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setRealms(RealmRepresentation[] realms) {
        realmsTable.setRowData(Arrays.asList(realms));
    }

    @UiHandler("createRealmButton")
    void createRealmClicked(ClickEvent e) {
        presenter.createRealm();
    }

    protected void setTestData() {
        List<RealmRepresentation> testData = new ArrayList<>();

        RealmRepresentation a = new RealmRepresentation();
        a.setDisplayName("This is a test");
        a.setEnabled(true);
        a.setRealm("foo");
        testData.add(a);

        RealmRepresentation b = new RealmRepresentation();
        b.setDisplayName("Another test");
        b.setEnabled(false);
        b.setRealm("bar");
        testData.add(b);

        RealmRepresentation c = new RealmRepresentation();
        c.setDisplayName("Third test tenant");
        c.setEnabled(true);
        c.setRealm("baz123");
        testData.add(c);

        realmsTable.setRowData(0, testData);

    }
}
