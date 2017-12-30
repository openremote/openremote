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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.Confirmation;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.components.client.style.WidgetStyle;
import org.openremote.manager.client.widget.FormViewImpl;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.User;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

public class AdminUsersImpl extends FormViewImpl implements AdminUsers {

    interface UI extends UiBinder<HTMLPanel, AdminUsersImpl> {
    }

    @UiField
    AdminUsersTable.Style usersTableStyle;

    @UiField(provided = true)
    AdminUsersNavigation adminUsersNavigation;

    @UiField
    HTMLPanel mainContent;

    @UiField
    ListBox tenantListBox;

    @UiField
    Label noUsersLabel;

    final AdminUsersTable table;
    Presenter presenter;

    @Inject
    public AdminUsersImpl(Provider<Confirmation> confirmationDialogProvider,
                          WidgetStyle widgetStyle,
                          FormTableStyle formTableStyle,
                          AdminUsersNavigation adminUsersNavigation) {
        super(confirmationDialogProvider, widgetStyle);

        this.adminUsersNavigation = adminUsersNavigation;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        tenantListBox.addChangeHandler(event -> {
            String realm = tenantListBox.getSelectedValue();
            if (realm == null || realm.length() == 0) {
                if (presenter != null)
                    presenter.onTenantSelected(null);
                noUsersLabel.setVisible(false);
            } else {
                if (presenter != null)
                    presenter.onTenantSelected(realm);
                noUsersLabel.setVisible(false);
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
        mainContent.add(table);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        if (presenter == null) {
            adminUsersNavigation.setVisible(false);
            adminUsersNavigation.reset();
        } else {
            adminUsersNavigation.setActive(presenter.getPlace());
            adminUsersNavigation.setVisible(true);
        }
        noUsersLabel.setVisible(false);
        tenantListBox.clear();
        tenantListBox.addItem(managerMessages.loadingDotdotdot());
        table.setVisible(false);
        table.setRowData(new ArrayList<>());
        table.flush();
    }

    @Override
    public void setTenants(Tenant[] tenants, String selectedRealm) {
        tenantListBox.clear();
        tenantListBox.addItem(managerMessages.selectTenant(), "");
        for (Tenant tenant : tenants) {
            tenantListBox.addItem(tenant.getDisplayName(), tenant.getRealm());
        }
        if (selectedRealm == null) {
            tenantListBox.setSelectedIndex(0);
        } else {
            for (int i = 0; i < tenantListBox.getItemCount(); i++) {
                if (tenantListBox.getValue(i).equals(selectedRealm)) {
                    tenantListBox.setSelectedIndex(i);
                }
            }
        }
    }

    @Override
    public void setUsers(User[] users) {
        noUsersLabel.setVisible(users.length == 0);
        table.setVisible(users.length > 0);
        table.setRowData(Arrays.asList(users));
        table.flush();
    }

}
