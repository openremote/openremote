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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Provider;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.Confirmation;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.widget.FormViewImpl;
import org.openremote.manager.client.widget.Hyperlink;
import org.openremote.manager.shared.security.Tenant;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

public class AdminTenantsImpl extends FormViewImpl implements AdminTenants {

    interface UI extends UiBinder<HTMLPanel, AdminTenantsImpl> {
    }

    @UiField
    AdminTenantsTable.Style tenantsTableStyle;

    @UiField
    Hyperlink createLink;

    @UiField
    HTMLPanel mainContent;

    @UiField
    Label noTenantsLabel;

    final AdminTenantsTable table;
    Presenter presenter;

    @Inject
    public AdminTenantsImpl(Environment environment,
                            Provider<Confirmation> confirmationDialogProvider,
                            FormTableStyle formTableStyle) {
        super(confirmationDialogProvider, environment.getWidgetStyle());

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        table = new AdminTenantsTable(managerMessages, tenantsTableStyle, formTableStyle);
        table.getSelectionModel().addSelectionChangeHandler(event -> {
                Tenant selected;
                if ((selected = table.getSelectedObject()) != null
                    && presenter != null) {
                    presenter.onTenantSelected(selected);
                }
            }
        );
        mainContent.add(table);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Reset state
        setCreateTenantHistoryToken("");
        noTenantsLabel.setVisible(true);
        table.setVisible(false);
        table.setRowData(new ArrayList<>());
        table.flush();
    }

    @Override
    public void setTenants(Tenant[] tenants) {
        noTenantsLabel.setVisible(tenants.length == 0);
        table.setVisible(tenants.length > 0);
        table.setRowData(Arrays.asList(tenants));
        table.flush();
    }

    @Override
    public void setCreateTenantHistoryToken(String token) {
        createLink.setTargetHistoryToken(token);
        createLink.setVisible(token != null && token.length() > 0);
    }
}
