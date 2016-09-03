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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.Form;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.User;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

public class AdminUsersImpl extends Composite implements AdminUsers {

    interface UI extends UiBinder<HTMLPanel, AdminUsersImpl> {
    }

    @UiField
    protected ManagerMessages managerMessages;

    @UiField
    protected WidgetStyle widgetStyle;

    @UiField
    AdminUsersTable.Style usersTableStyle;

    @UiField
    ListBox tenantListBox;

    @UiField
    Form usersForm;

    @UiField
    PushButton createButton;

    @UiField
    SimplePanel tableContainer;

    final AdminUsersTable table;
    Presenter presenter;

    @Inject
    public AdminUsersImpl(FormTableStyle formTableStyle) {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        hideUsersForm();

        tenantListBox.addChangeHandler(event -> {
            String realm = tenantListBox.getSelectedValue();
            if (realm == null || realm.length() == 0) {
                hideUsersForm();
                if (presenter != null)
                    presenter.onTenantSelected(null);
            } else {
                showUsersForm();
                if (presenter != null)
                    presenter.onTenantSelected(realm);
            }
        });

        table = new AdminUsersTable(managerMessages, usersTableStyle, formTableStyle);
        table.getSelectionModel().addSelectionChangeHandler(event -> {
                User selected;
                if ((selected = table.getSelectedObject()) != null
                    && presenter != null) {
                    presenter.onUserSelected(selected);
                }
            }
        );
        tableContainer.add(table);

    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            hideUsersForm();
            tenantListBox.clear();
            tenantListBox.addItem(managerMessages.loadingDotdotdot());
            table.setRowData(new ArrayList<>());
            table.flush();
        }
    }

    @Override
    public void setTenants(Tenant[] tenants, String selectedRealm) {
        hideUsersForm();

        tenantListBox.clear();
        tenantListBox.addItem(managerMessages.selectTenant(), "");
        for (Tenant tenant : tenants) {
            tenantListBox.addItem(tenant.getDisplayName(), tenant.getRealm());
        }
        if (selectedRealm == null) {
            tenantListBox.setSelectedIndex(0);
            hideUsersForm();
        } else {
            for (int i = 0; i < tenantListBox.getItemCount(); i++) {
                if (tenantListBox.getValue(i).equals(selectedRealm)) {
                    tenantListBox.setSelectedIndex(i);
                    showUsersForm();
                }
            }
        }
    }

    @Override
    public void setUsers(User[] users) {
        tableContainer.setVisible(users.length > 0);
        table.setRowData(Arrays.asList(users));
        table.flush();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        if (presenter != null)
            presenter.createUser();
    }

    protected void showUsersForm() {
        usersForm.setVisible(true);
    }

    protected void hideUsersForm() {
        usersForm.setVisible(false);
    }

}
