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

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.SingleSelectionModel;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.style.FormTable;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.widget.IconCell;
import org.openremote.manager.shared.security.Tenant;

public class AdminTenantsTable extends FormTable<Tenant> {

    public interface Style extends CssResource {
        String nameColumn();

        String realmColumn();

        String enabledColumn();
    }

    final protected Style style;

    final protected SingleSelectionModel<Tenant> selectionModel = new SingleSelectionModel<>();

    final protected TextColumn<Tenant> nameColumn = new TextColumn<Tenant>() {
        @Override
        public String getValue(Tenant tenant) {
            return tenant.getDisplayName();
        }
    };

    final protected TextColumn<Tenant> realmColumn = new TextColumn<Tenant>() {
        @Override
        public String getValue(Tenant tenant) {
            return tenant.getRealm();
        }
    };

    final protected Column<Tenant, String> enabledColumn = new Column<Tenant, String>(new IconCell()) {
        @Override
        public String getValue(Tenant tenant) {
            return tenant.getEnabled() ? "check-circle" : "circle-thin";
        }
    };

    public AdminTenantsTable(ManagerConstants managerConstants,
                             Style style,
                             FormTableStyle formTableStyle) {
        super(Integer.MAX_VALUE, formTableStyle);

        this.style = style;

        setSelectionModel(selectionModel);

        applyStyleCellText(nameColumn);
        addColumn(nameColumn, createHeader(managerConstants.tenantName()));
        addColumnStyleName(0, style.nameColumn());

        applyStyleCellText(realmColumn);
        addColumn(realmColumn, createHeader(managerConstants.realm()));
        addColumnStyleName(1, style.realmColumn());

        addColumn(enabledColumn, createHeader(managerConstants.enabled()));
        addColumnStyleName(2, style.enabledColumn());
        enabledColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }

    @Override
    public SingleSelectionModel<Tenant> getSelectionModel() {
        return selectionModel;
    }

    public Tenant getSelectedObject() {
        return getSelectionModel().getSelectedObject();
    }

}
